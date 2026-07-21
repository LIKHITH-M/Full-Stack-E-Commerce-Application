package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private static final String TOPIC = "order-events";

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void publishOrderPlacedEvent(OrderPlacedEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getOrderId(), event).whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("✅ Published OrderPlacedEvent to Kafka for order: " + event.getOrderId());
                } else {
                    System.err.println("❌ Failed to publish OrderPlacedEvent to Kafka: " + ex.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Kafka is not available, skipping event publish: " + e.getMessage());
        }
    }
}
