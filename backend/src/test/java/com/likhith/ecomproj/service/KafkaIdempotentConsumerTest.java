package com.likhith.ecomproj.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likhith.ecomproj.model.OrderPlacedEvent;
import com.likhith.ecomproj.model.OutboxEvent;
import com.likhith.ecomproj.model.ProcessedEvent;
import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.OutboxEventRepo;
import com.likhith.ecomproj.repo.ProcessedEventRepo;
import com.likhith.ecomproj.repo.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class KafkaIdempotentConsumerTest {

    @Autowired
    private InventoryUpdateConsumer consumer;

    @Autowired
    private ProcessedEventRepo processedEventRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private OutboxEventRepo outboxEventRepo;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @MockBean
    private OrderEventProducer orderEventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct;

    @BeforeEach
    public void setUp() {
        processedEventRepo.deleteAll();
        outboxEventRepo.deleteAll();

        testProduct = new Product();
        testProduct.setName("Gaming Monitor");
        testProduct.setBrand("Dell");
        testProduct.setPrice(new BigDecimal("15000.00"));
        testProduct.setCategory("Monitors");
        testProduct.setStockQuantity(10);
        testProduct.setAvailable(true);
        testProduct = productRepo.save(testProduct);
    }

    @Test
    @DisplayName("Kafka: Idempotent consumer processes event once and ignores redelivered duplicate eventId")
    public void testIdempotentConsumerPreventsDuplicateInventoryDecrement() {
        String eventId = "evt_uuid_99999";
        String orderId = "ord_idem_consumer_1";

        OrderPlacedEvent event = new OrderPlacedEvent();
        event.setEventId(eventId);
        event.setOrderId(orderId);
        event.setUsername("testuser");
        event.setItems(List.of(new OrderPlacedEvent.OrderItem(testProduct.getId(), testProduct.getName(), 2, testProduct.getPrice())));

        // 1. First event processing -> should decrement stock from 10 to 8
        consumer.handleOrderPlacedEvent(event);

        Product afterFirstRun = productRepo.findById(testProduct.getId()).orElseThrow();
        assertEquals(8, afterFirstRun.getStockQuantity(), "First consumer run must decrement stock from 10 to 8");
        assertTrue(processedEventRepo.existsByEventIdAndEventType(eventId, "ORDER_PLACED"),
                "ProcessedEvent must be recorded after processing");

        // 2. Second event processing with the SAME eventId (Kafka redelivery / retry)
        consumer.handleOrderPlacedEvent(event);

        Product afterDuplicateRun = productRepo.findById(testProduct.getId()).orElseThrow();
        assertEquals(8, afterDuplicateRun.getStockQuantity(),
                "Duplicate Kafka event must be IGNORED by idempotent consumer, keeping stock at 8");
    }

    @Test
    @DisplayName("Outbox: OutboxPublisher publishes pending events and marks them PUBLISHED")
    public void testOutboxPublisherDrainsPendingEvents() throws Exception {
        OrderPlacedEvent orderEvent = new OrderPlacedEvent();
        orderEvent.setEventId("evt_outbox_test");
        orderEvent.setOrderId("ord_outbox_pub_1");
        orderEvent.setUsername("testuser");

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId("evt_outbox_test");
        outbox.setEventType("ORDER_PLACED");
        outbox.setAggregateId("ord_outbox_pub_1");
        outbox.setPayload(objectMapper.writeValueAsString(orderEvent));
        outbox.setStatus("PENDING");
        outbox.setCreatedAt(new Date());
        outbox.setRetryCount(0);
        outboxEventRepo.save(outbox);

        doNothing().when(orderEventProducer).publishOrderPlacedEvent(any(OrderPlacedEvent.class));

        outboxPublisher.publishPendingEvents();

        verify(orderEventProducer, times(1)).publishOrderPlacedEvent(any(OrderPlacedEvent.class));

        OutboxEvent updated = outboxEventRepo.findAll().get(0);
        assertEquals("PUBLISHED", updated.getStatus(), "Outbox event must be marked PUBLISHED after successful send");
        assertNotNull(updated.getProcessedAt());
    }
}
