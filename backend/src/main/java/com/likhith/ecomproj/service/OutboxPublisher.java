package com.likhith.ecomproj.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likhith.ecomproj.model.OrderPlacedEvent;
import com.likhith.ecomproj.model.OutboxEvent;
import com.likhith.ecomproj.repo.OutboxEventRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Transactional Outbox Publisher.
 *
 * Polls the outbox_events table for pending events and publishes them to Kafka.
 * This decouples Kafka publishing from the order transaction:
 *
 * - If Kafka is UP: Events are published and marked PUBLISHED.
 * - If Kafka is DOWN: Events stay PENDING and are retried on the next scheduled poll.
 * - If the app crashes: Events remain safely in the DB and are picked up on restart.
 *
 * This guarantees at-least-once delivery to Kafka, combined with the idempotent consumer
 * on the receiving side to handle duplicates.
 */
@Service
@Slf4j
public class OutboxPublisher {

    private static final int MAX_RETRIES = 5;

    @Autowired
    private OutboxEventRepo outboxEventRepo;

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepo
                .findByStatusAndRetryCountLessThanOrderByCreatedAtAsc("PENDING", MAX_RETRIES);

        if (pending.isEmpty()) return;

        log.info("OutboxPublisher: Found {} pending events to publish", pending.size());

        for (OutboxEvent event : pending) {
            try {
                if ("ORDER_PLACED".equals(event.getEventType())) {
                    OrderPlacedEvent orderEvent = objectMapper.readValue(
                            event.getPayload(), OrderPlacedEvent.class);
                    orderEventProducer.publishOrderPlacedEvent(orderEvent);
                }

                event.setStatus("PUBLISHED");
                event.setProcessedAt(new Date());
                outboxEventRepo.save(event);
                log.info("OutboxPublisher: Published event {} for aggregate {}", event.getId(), event.getAggregateId());

            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus("FAILED");
                    log.error("OutboxPublisher: Event {} permanently failed after {} retries: {}",
                            event.getId(), MAX_RETRIES, e.getMessage());
                } else {
                    log.warn("OutboxPublisher: Event {} publish failed (attempt {}/{}): {}",
                            event.getId(), event.getRetryCount(), MAX_RETRIES, e.getMessage());
                }
                outboxEventRepo.save(event);
            }
        }
    }
}
