package com.likhith.ecomproj.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likhith.ecomproj.model.OrderEntity;
import com.likhith.ecomproj.model.OrderPlacedEvent;
import com.likhith.ecomproj.repo.OrderRepo;
import com.likhith.ecomproj.service.OrderEventProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private com.likhith.ecomproj.repo.UserRepo userRepo;

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    /**
     * Called by frontend after successful Razorpay payment.
     * Saves the order and publishes a Kafka event for email + inventory.
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody OrderPlacedEvent event) {
        try {
            // Get the logged-in username
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            event.setUsername(username);

            if (event.getEmail() == null || event.getEmail().trim().isEmpty()) {
                com.likhith.ecomproj.model.User dbUser = userRepo.findByUsername(username);
                if (dbUser != null && dbUser.getEmail() != null) {
                    event.setEmail(dbUser.getEmail());
                }
            }

            // Save order to DB
            OrderEntity order = new OrderEntity();
            order.setUsername(username);
            order.setEmail(event.getEmail());
            order.setTotalAmount(event.getTotalAmount());
            order.setPaymentId(event.getPaymentId());
            order.setOrderId(event.getOrderId());
            order.setStatus("PLACED");
            order.setCreatedAt(new Date());
            order.setOrderItems(objectMapper.writeValueAsString(event.getItems()));

            orderRepo.save(order);
            System.out.println("✅ Order saved to DB: " + order.getOrderId());

            // Publish Kafka event (async: email + inventory update)
            orderEventProducer.publishOrderPlacedEvent(event);

            // ========== DIRECT EMAIL (bypass Kafka) ==========
            try {
                String recipientEmail = event.getEmail();
                if (recipientEmail != null && !recipientEmail.trim().isEmpty()) {
                    SimpleMailMessage message = new SimpleMailMessage();
                    if (senderEmail != null && !senderEmail.trim().isEmpty()) {
                        message.setFrom(senderEmail.trim());
                    }
                    message.setTo(recipientEmail.trim());
                    message.setSubject("Order Confirmation - " + event.getOrderId());

                    StringBuilder body = new StringBuilder();
                    body.append("Dear ").append(event.getUsername()).append(",\n\n");
                    body.append("Thank you for your order!\n\n");
                    body.append("Order ID: ").append(event.getOrderId()).append("\n");
                    body.append("Payment ID: ").append(event.getPaymentId()).append("\n");
                    body.append("Total Amount: Rs.").append(event.getTotalAmount()).append("\n\n");
                    body.append("Thank you for shopping with us!");
                    message.setText(body.toString());

                    mailSender.send(message);
                    System.out.println("✅ Direct email sent to: " + recipientEmail);
                } else {
                    System.out.println("⚠️ No email address found for user: " + username);
                }
            } catch (Exception emailEx) {
                System.err.println("❌ Direct email FAILED: " + emailEx.getMessage());
                emailEx.printStackTrace();
            }
            // =================================================

            return new ResponseEntity<>(order, HttpStatus.CREATED);
        } catch (JsonProcessingException e) {
            return new ResponseEntity<>("Failed to process order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get orders for the currently logged-in user.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderEntity>> getMyOrders() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return new ResponseEntity<>(orderRepo.findByUsername(username), HttpStatus.OK);
    }

    /**
     * Admin: Get all orders.
     */
    @GetMapping("/all")
    public ResponseEntity<List<OrderEntity>> getAllOrders() {
        return new ResponseEntity<>(orderRepo.findAll(), HttpStatus.OK);
    }
}
