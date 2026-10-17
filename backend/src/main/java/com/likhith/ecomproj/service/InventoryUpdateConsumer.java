package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.OrderPlacedEvent;
import com.likhith.ecomproj.model.ProcessedEvent;
import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.ProcessedEventRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Kafka event listener responsible for updating inventory stock asynchronously when an order is placed.
 *
 * Implements two distinct concurrency and reliability protections:
 *
 * 1. IDEMPOTENT CONSUMER (Solves: "Have I already processed this Kafka event?"):
 *    - Checks ProcessedEvent table before updating inventory.
 *    - If eventId already processed, safely ignores the duplicate message.
 *    - Uses database unique constraint on (eventId, eventType) as final safeguard.
 *
 * 2. OPTIMISTIC LOCKING (Solves: "Did another transaction modify this product stock?"):
 *    - Product entity uses @Version column.
 *    - If two consumers concurrently modify stock for the same product, the second gets
 *      ObjectOptimisticLockingFailureException and retries with the latest entity state.
 */
@Service
@Slf4j
public class InventoryUpdateConsumer {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 50;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProcessedEventRepo processedEventRepo;

    @KafkaListener(topics = "order-events", groupId = "inventory-group", autoStartup = "${spring.kafka.listener.auto-startup:true}")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        String eventKey = event.getEventId() != null ? event.getEventId() : event.getOrderId();
        log.info("InventoryUpdateConsumer received event {} for order: {}", eventKey, event.getOrderId());

        if (eventKey == null || event.getItems() == null || event.getItems().isEmpty()) {
            return;
        }

        // 1. Idempotency Check: skip if this event was already processed
        if (processedEventRepo.existsByEventIdAndEventType(eventKey, "ORDER_PLACED")) {
            log.info("Idempotent consumer: Event {} already processed. Safely ignoring duplicate Kafka message.", eventKey);
            return;
        }

        // 2. Process inventory updates using Optimistic Locking retry loop
        for (OrderPlacedEvent.OrderItem item : event.getItems()) {
            boolean updated = false;
            int attempt = 0;

            while (!updated && attempt < MAX_RETRIES) {
                attempt++;
                try {
                    Product updatedProduct = productService.decrementStockWithOptimisticLocking(
                            item.getProductId(),
                            item.getQuantity()
                    );
                    log.info("Stock updated for product '{}' (ID: {}). New Stock: {}, Version: {}",
                            updatedProduct.getName(), item.getProductId(),
                            updatedProduct.getStockQuantity(), updatedProduct.getVersion());
                    updated = true;
                } catch (ObjectOptimisticLockingFailureException ex) {
                    log.warn("Optimistic locking conflict on product ID {} (attempt {}/{}). Another transaction modified this product concurrently: {}",
                            item.getProductId(), attempt, MAX_RETRIES, ex.getMessage());
                    if (attempt < MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS * attempt);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        log.error("Failed to update stock for product ID {} after {} attempts due to persistent optimistic lock conflicts.",
                                item.getProductId(), MAX_RETRIES);
                        throw ex; // Re-throw to trigger Kafka DLT recoverer
                    }
                } catch (IllegalArgumentException | IllegalStateException businessEx) {
                    log.error("Stock update rejected for product ID {}: {}", item.getProductId(), businessEx.getMessage());
                    break; // Non-retryable business validation error
                } catch (Exception ex) {
                    log.error("Unexpected error updating inventory for product ID {}: {}", item.getProductId(), ex.getMessage());
                    throw ex; // Re-throw for Kafka retry/DLT
                }
            }
        }

        // 3. Record event as processed to guarantee idempotency on redeliveries
        try {
            ProcessedEvent processedEvent = new ProcessedEvent();
            processedEvent.setEventId(eventKey);
            processedEvent.setEventType("ORDER_PLACED");
            processedEvent.setAggregateId(event.getOrderId());
            processedEvent.setProcessedAt(new Date());
            processedEventRepo.save(processedEvent);
            log.info("Recorded event {} as processed in database", eventKey);
        } catch (DataIntegrityViolationException dive) {
            log.warn("Concurrent duplicate event {} caught by database unique constraint", eventKey);
        }
    }
}
