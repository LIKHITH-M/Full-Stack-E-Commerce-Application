package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailNotificationConsumer {

    @Autowired
    private ResendEmailService resendEmailService;

    @KafkaListener(topics = "order-events", groupId = "email-group", autoStartup = "${spring.kafka.listener.auto-startup:true}")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("EmailNotificationConsumer received event for order: {} | Recipient: {}", event.getOrderId(), event.getEmail());

        try {
            if (event.getEmail() != null && !event.getEmail().trim().isEmpty()) {
                String subject = "Order Confirmation - " + event.getOrderId();
                StringBuilder html = new StringBuilder();
                html.append("<h2>Order Confirmation</h2>");
                html.append("<p>Dear ").append(event.getUsername()).append(",</p>");
                html.append("<p>Thank you for your order!</p>");
                html.append("<p><strong>Order ID:</strong> ").append(event.getOrderId()).append("</p>");
                html.append("<p><strong>Total Amount:</strong> ₹").append(event.getTotalAmount()).append("</p>");

                resendEmailService.sendEmail(event.getEmail().trim(), subject, html.toString());
                log.info("Successfully sent order confirmation email for order: {}", event.getOrderId());
            } else {
                log.info("No email address provided for order {}, skipping email notification.", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Failed to send order email notification for order {}: {}", event.getOrderId(), e.getMessage());
        }
    }
}
