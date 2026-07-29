package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.OrderPlacedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer orderEventProducer;

    private OrderPlacedEvent sampleEvent;

    @BeforeEach
    void setUp() {
        OrderPlacedEvent.OrderItem item = new OrderPlacedEvent.OrderItem(
                1, "Wireless Mouse", 2, new BigDecimal("29.99")
        );

        sampleEvent = new OrderPlacedEvent();
        sampleEvent.setUsername("testuser");
        sampleEvent.setEmail("test@example.com");
        sampleEvent.setPaymentId("pay_test_123");
        sampleEvent.setOrderId("order_456");
        sampleEvent.setTotalAmount(new BigDecimal("59.98"));
        sampleEvent.setItems(List.of(item));
    }

    @Test
    void testPublishOrderPlacedEvent_Success() {
        // Mock kafkaTemplate.send() to return a completed future
        when(kafkaTemplate.send(anyString(), anyString(), any(OrderPlacedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        orderEventProducer.publishOrderPlacedEvent(sampleEvent);

        // Verify Kafka topic and payload
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);

        verify(kafkaTemplate, times(1)).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals("order-events", topicCaptor.getValue());
        assertEquals("order_456", keyCaptor.getValue());
        assertEquals("testuser", eventCaptor.getValue().getUsername());
        assertEquals("test@example.com", eventCaptor.getValue().getEmail());
        assertEquals(new BigDecimal("59.98"), eventCaptor.getValue().getTotalAmount());
    }

    @Test
    void testPublishOrderPlacedEvent_KafkaUnavailable() {
        // Simulate Kafka being unavailable
        when(kafkaTemplate.send(anyString(), anyString(), any(OrderPlacedEvent.class)))
                .thenThrow(new RuntimeException("Kafka broker unavailable"));

        // Should NOT throw an exception — graceful degradation
        assertDoesNotThrow(() -> orderEventProducer.publishOrderPlacedEvent(sampleEvent));
    }
}
