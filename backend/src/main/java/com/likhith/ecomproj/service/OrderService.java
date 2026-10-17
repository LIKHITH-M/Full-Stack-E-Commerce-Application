package com.likhith.ecomproj.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.likhith.ecomproj.dto.CheckoutRequest;
import com.likhith.ecomproj.dto.OrderResponseDTO;
import com.likhith.ecomproj.model.*;
import com.likhith.ecomproj.repo.CartItemRepo;
import com.likhith.ecomproj.repo.OrderRepo;
import com.likhith.ecomproj.repo.OutboxEventRepo;
import com.likhith.ecomproj.repo.ProductRepo;
import com.likhith.ecomproj.repo.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core order service handling checkout business logic.
 *
 * Key responsibilities:
 * 1. Server-side price calculation (never trusts client-supplied prices or amounts)
 * 2. Idempotent order creation (duplicate orderId or paymentId returns existing order)
 * 3. Atomic DB transaction (order + cart clear + outbox event in one commit)
 * 4. Razorpay signature verification with rejection of missing or invalid signatures
 * 5. Transactional outbox pattern for guaranteed at-least-once Kafka event delivery
 * 6. Idempotent webhook payment confirmation
 */
@Service
@Slf4j
public class OrderService {

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private CartItemRepo cartItemRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private OutboxEventRepo outboxEventRepo;

    @Autowired
    private RazorPayService razorPayService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Processes a checkout request with full server-side validation.
     *
     * All of the following happen in ONE database transaction:
     * 1. Idempotency check on orderId and paymentId (return existing order if duplicate)
     * 2. Fetch products from DB and calculate prices server-side
     * 3. Verify Razorpay signature (rejecting missing or invalid signatures for online payments)
     * 4. Save order to DB with OrderStatus
     * 5. Clear user's cart from DB
     * 6. Save outbox event with unique eventId (for asynchronous Kafka publishing)
     *
     * If ANY step fails, the entire transaction rolls back — no partial state.
     */
    @Transactional
    public OrderEntity checkout(String username, CheckoutRequest request) {
        // 1. IDEMPOTENCY: Check if orderId or paymentId was already processed
        if (request.getOrderId() != null && !request.getOrderId().trim().isEmpty()) {
            Optional<OrderEntity> existing = orderRepo.findByOrderId(request.getOrderId().trim());
            if (existing.isPresent()) {
                log.info("Idempotent checkout: orderId {} already exists, returning existing", request.getOrderId());
                return existing.get();
            }
        }
        if (request.getPaymentId() != null && !request.getPaymentId().trim().isEmpty()) {
            Optional<OrderEntity> existing = orderRepo.findByPaymentId(request.getPaymentId().trim());
            if (existing.isPresent()) {
                log.info("Idempotent checkout: paymentId {} already exists, returning existing", request.getPaymentId());
                return existing.get();
            }
        }

        // 2. SERVER-SIDE PRICE CALCULATION: Never trust client-supplied amounts
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Checkout must contain at least one item");
        }

        List<Integer> productIds = request.getItems().stream()
                .map(CheckoutRequest.CheckoutItem::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepo.findAllByIdIn(productIds);
        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal serverCalculatedTotal = BigDecimal.ZERO;
        List<OrderPlacedEvent.OrderItem> verifiedItems = new ArrayList<>();

        for (CheckoutRequest.CheckoutItem item : request.getItems()) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: ID " + item.getProductId());
            }
            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Invalid quantity for product: " + product.getName());
            }
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for '" + product.getName()
                        + "'. Available: " + product.getStockQuantity()
                        + ", Requested: " + item.getQuantity());
            }

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            serverCalculatedTotal = serverCalculatedTotal.add(lineTotal);

            verifiedItems.add(new OrderPlacedEvent.OrderItem(
                    product.getId(),
                    product.getName(),
                    item.getQuantity(),
                    product.getPrice()
            ));
        }

        // 3. RAZORPAY SIGNATURE VERIFICATION
        boolean isManualCheckout = request.getPaymentId() != null
                && request.getPaymentId().startsWith("PAY_MANUAL_");

        if (!isManualCheckout) {
            // Reject missing signature
            if (request.getRazorpaySignature() == null || request.getRazorpaySignature().trim().isEmpty()) {
                throw new SecurityException("Missing Razorpay payment signature");
            }
            // Reject invalid signature
            boolean signatureValid = razorPayService.verifyPaymentSignature(
                    request.getOrderId(),
                    request.getPaymentId(),
                    request.getRazorpaySignature()
            );
            if (!signatureValid) {
                throw new SecurityException("Razorpay payment signature verification failed");
            }
            log.info("Razorpay signature verified for order: {}", request.getOrderId());
        }

        // Resolve customer email
        String email = request.getEmail();
        if (email == null || email.trim().isEmpty()) {
            User dbUser = userRepo.findByUsername(username);
            if (dbUser != null && dbUser.getEmail() != null) {
                email = dbUser.getEmail();
            }
        }

        // 4. SAVE ORDER TO DB
        OrderEntity order = new OrderEntity();
        order.setUsername(username);
        order.setEmail(email);
        order.setTotalAmount(serverCalculatedTotal); // Strictly server-calculated
        order.setPaymentId(request.getPaymentId());
        order.setOrderId(request.getOrderId());
        order.setStatus(isManualCheckout ? OrderStatus.PLACED : OrderStatus.PAID);
        order.setCreatedAt(new Date());

        try {
            order.setOrderItems(objectMapper.writeValueAsString(verifiedItems));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize order items", e);
        }

        orderRepo.save(order);
        log.info("Order saved to DB: {} for user: {}, total: {}", order.getOrderId(), username, serverCalculatedTotal);

        // 5. CLEAR CART (same database transaction)
        cartItemRepo.deleteByUsername(username);
        log.info("Cart cleared for user: {}", username);

        // 6. SAVE OUTBOX EVENT (same transaction — Transactional Outbox Pattern)
        String eventId = UUID.randomUUID().toString();
        OrderPlacedEvent kafkaEvent = new OrderPlacedEvent();
        kafkaEvent.setEventId(eventId);
        kafkaEvent.setUsername(username);
        kafkaEvent.setEmail(email);
        kafkaEvent.setPaymentId(request.getPaymentId());
        kafkaEvent.setOrderId(request.getOrderId());
        kafkaEvent.setTotalAmount(serverCalculatedTotal);
        kafkaEvent.setItems(verifiedItems);

        try {
            OutboxEvent outbox = new OutboxEvent();
            outbox.setEventId(eventId);
            outbox.setEventType("ORDER_PLACED");
            outbox.setAggregateId(request.getOrderId());
            outbox.setPayload(objectMapper.writeValueAsString(kafkaEvent));
            outbox.setStatus("PENDING");
            outbox.setCreatedAt(new Date());
            outbox.setRetryCount(0);
            outboxEventRepo.save(outbox);
            log.info("Outbox event saved: eventId={}, orderId={}", eventId, request.getOrderId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }

        return order;
    }

    /**
     * Idempotent webhook payment state handler.
     *
     * Ensures that if an order already exists and is PAID, redundant webhook deliveries
     * do not duplicate orders or cause corrupt state transitions.
     */
    @Transactional
    public void processPaymentWebhook(String paymentId, String razorpayOrderId, String eventType) {
        log.info("Processing payment webhook: event={}, orderId={}, paymentId={}", eventType, razorpayOrderId, paymentId);

        Optional<OrderEntity> existingOrder = Optional.empty();
        if (razorpayOrderId != null && !razorpayOrderId.trim().isEmpty()) {
            existingOrder = orderRepo.findByOrderId(razorpayOrderId.trim());
        }
        if (existingOrder.isEmpty() && paymentId != null && !paymentId.trim().isEmpty()) {
            existingOrder = orderRepo.findByPaymentId(paymentId.trim());
        }

        if (existingOrder.isPresent()) {
            OrderEntity order = existingOrder.get();
            if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.PLACED) {
                log.info("Webhook idempotency: Order {} is already in {} state. Skipping.", order.getOrderId(), order.getStatus());
                return;
            }
            order.setStatus(OrderStatus.PAID);
            if (order.getPaymentId() == null && paymentId != null) {
                order.setPaymentId(paymentId);
            }
            orderRepo.save(order);
            log.info("Webhook updated order {} status to PAID", order.getOrderId());
        } else {
            // If the order does not exist yet (e.g. webhook delivered before client POST /checkout),
            // we do NOT blindly invent an unverified order without items or user context.
            log.info("Webhook received for order {} which is not yet created by client. Awaiting client checkout.", razorpayOrderId);
        }
    }

    public List<OrderEntity> getOrdersByUsername(String username) {
        return orderRepo.findByUsername(username);
    }

    public List<OrderEntity> getAllOrders() {
        return orderRepo.findAll();
    }

    public List<OrderResponseDTO> getOrdersDTOByUsername(String username) {
        return orderRepo.findByUsername(username).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<OrderResponseDTO> getAllOrdersDTO() {
        return orderRepo.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public OrderResponseDTO toDTO(OrderEntity order) {
        return new OrderResponseDTO(
                order.getId(),
                order.getUsername(),
                order.getEmail(),
                order.getTotalAmount(),
                order.getPaymentId(),
                order.getOrderId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getOrderItems()
        );
    }

    /**
     * Calculates the server-side total for a list of product IDs and quantities.
     * Used by PaymentController to create Razorpay orders with verified amounts.
     */
    public BigDecimal calculateServerTotal(List<CheckoutRequest.CheckoutItem> items) {
        List<Integer> productIds = items.stream()
                .map(CheckoutRequest.CheckoutItem::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productRepo.findAllByIdIn(productIds);
        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal total = BigDecimal.ZERO;
        for (CheckoutRequest.CheckoutItem item : items) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: ID " + item.getProductId());
            }
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }
}
