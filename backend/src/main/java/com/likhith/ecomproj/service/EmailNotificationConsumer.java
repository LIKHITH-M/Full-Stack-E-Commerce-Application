package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationConsumer {

    @Autowired
    private ResendEmailService resendEmailService;

    @KafkaListener(topics = "order-events", groupId = "email-group", autoStartup = "${spring.kafka.listener.auto-startup:true}")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        System.out.println("EmailNotificationConsumer received event for order: " + event.getOrderId() + " | Recipient: " + event.getEmail());

        try {
            if (event.getEmail() != null && !event.getEmail().trim().isEmpty()) {
                String subject = "Kafka Notification: Order Confirmation - " + event.getOrderId();
                StringBuilder html = new StringBuilder();
                html.append("<h2>Order Confirmation</h2>");
                html.append("<p>Dear ").append(event.getUsername()).append(",</p>");
                html.append("<p>Thank you for your order!</p>");
                html.append("<p><strong>Order ID:</strong> ").append(event.getOrderId()).append("</p>");
                html.append("<p><strong>Total Amount:</strong> ₹").append(event.getTotalAmount()).append("</p>");

                resendEmailService.sendEmail(event.getEmail().trim(), subject, html.toString());
            } else {
                System.out.println("No email address provided, skipping email notification.");
            }
        } catch (Exception e) {
            System.err.println("Failed to send Kafka email notification: " + e.getMessage());
        }
    }
}
