package com.likhith.ecomproj.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likhith.ecomproj.model.OrderEntity;
import com.likhith.ecomproj.model.OrderPlacedEvent;
import com.likhith.ecomproj.repo.CartItemRepo;
import com.likhith.ecomproj.repo.OrderRepo;
import com.likhith.ecomproj.service.OrderEventProducer;
import com.likhith.ecomproj.service.ResendEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private CartItemRepo cartItemRepo;

    @Autowired
    private com.likhith.ecomproj.repo.UserRepo userRepo;

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResendEmailService resendEmailService;

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

            // Clear the user's cart from DB after successful order
            cartItemRepo.deleteByUsername(username);
            System.out.println("✅ Cart cleared from DB for user: " + username);

            // Publish Kafka event (async: inventory update)
            orderEventProducer.publishOrderPlacedEvent(event);

            // ========== ASYNC EMAIL via Resend HTTP API ==========
            // Render free tier blocks SMTP ports 25/465/587.
            // Resend uses HTTPS (port 443) which is not blocked.
            final String recipientEmail = event.getEmail();
            final String orderUsername = event.getUsername();
            final String orderId = event.getOrderId();
            final String paymentId = event.getPaymentId();
            final BigDecimal totalAmount = event.getTotalAmount();

            new Thread(() -> {
                try {
                    if (recipientEmail != null && !recipientEmail.trim().isEmpty()) {
                        String subject = "Order Confirmation - " + orderId;
                        String html = "<h2>Order Confirmation</h2>"
                                + "<p>Dear " + orderUsername + ",</p>"
                                + "<p>Thank you for your order!</p>"
                                + "<table style='border-collapse:collapse;width:100%;max-width:400px'>"
                                + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Order ID</strong></td>"
                                + "<td style='padding:8px;border:1px solid #ddd'>" + orderId + "</td></tr>"
                                + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Payment ID</strong></td>"
                                + "<td style='padding:8px;border:1px solid #ddd'>" + paymentId + "</td></tr>"
                                + "<tr><td style='padding:8px;border:1px solid #ddd'><strong>Total Amount</strong></td>"
                                + "<td style='padding:8px;border:1px solid #ddd'>Rs." + totalAmount + "</td></tr>"
                                + "</table>"
                                + "<p>Thank you for shopping with us!</p>";

                        resendEmailService.sendEmail(recipientEmail.trim(), subject, html);
                    } else {
                        System.out.println("⚠️ No email address found for user: " + orderUsername);
                    }
                } catch (Exception emailEx) {
                    System.err.println("❌ Email FAILED: " + emailEx.getMessage());
                }
            }, "email-sender-" + orderId).start();
            // =====================================================

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
