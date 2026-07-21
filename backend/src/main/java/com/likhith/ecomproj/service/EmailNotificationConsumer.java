package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.OrderPlacedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationConsumer {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    @KafkaListener(topics = "order-events", groupId = "email-group", autoStartup = "${spring.kafka.listener.auto-startup:true}")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        System.out.println("EmailNotificationConsumer received event for order: " + event.getOrderId() + " | Recipient: " + event.getEmail());

        try {
            if (event.getEmail() != null && !event.getEmail().trim().isEmpty()) {
                SimpleMailMessage message = new SimpleMailMessage();
                if (senderEmail != null && !senderEmail.trim().isEmpty()) {
                    message.setFrom(senderEmail.trim());
                }
                message.setTo(event.getEmail().trim());
                message.setSubject("Order Confirmation - " + event.getOrderId());

                StringBuilder body = new StringBuilder();
                body.append("Dear ").append(event.getUsername()).append(",\n\n");
                body.append("Thank you for your order!\n\n");
                body.append("Order ID: ").append(event.getOrderId()).append("\n");
                body.append("Payment ID: ").append(event.getPaymentId()).append("\n");
                body.append("Total Amount: ₹").append(event.getTotalAmount()).append("\n\n");
                body.append("Order Items:\n");

                for (OrderPlacedEvent.OrderItem item : event.getItems()) {
                    body.append("  - ").append(item.getProductName())
                            .append(" x ").append(item.getQuantity())
                            .append(" = ₹").append(item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                            .append("\n");
                }

                body.append("\nThank you for shopping with us!");
                message.setText(body.toString());

                mailSender.send(message);
                System.out.println("Confirmation email sent to: " + event.getEmail());
            } else {
                System.out.println("No email address provided, skipping email notification.");
            }
        } catch (Exception e) {
            System.err.println("Failed to send email notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
