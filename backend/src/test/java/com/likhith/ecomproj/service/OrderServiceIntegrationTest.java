package com.likhith.ecomproj.service;

import com.likhith.ecomproj.dto.CheckoutRequest;
import com.likhith.ecomproj.model.*;
import com.likhith.ecomproj.repo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private OutboxEventRepo outboxEventRepo;

    @Autowired
    private CartItemRepo cartItemRepo;

    private Product testProduct;

    @BeforeEach
    public void setUp() {
        cartItemRepo.deleteAll();
        outboxEventRepo.deleteAll();
        orderRepo.deleteAll();

        testProduct = new Product();
        testProduct.setName("Wireless Keyboard");
        testProduct.setBrand("Logitech");
        testProduct.setPrice(new BigDecimal("2500.00"));
        testProduct.setCategory("Electronics");
        testProduct.setStockQuantity(20);
        testProduct.setAvailable(true);
        testProduct = productRepo.save(testProduct);
    }

    @Test
    @DisplayName("Order: Idempotent checkout returns existing order when duplicate orderId is submitted")
    public void testDuplicateOrderIdCheckoutIdempotency() {
        String orderId = "rzp_order_idem_1001";
        String paymentId = "PAY_MANUAL_1001"; // manual bypasses signature for test simplicity

        CheckoutRequest request1 = new CheckoutRequest();
        request1.setOrderId(orderId);
        request1.setPaymentId(paymentId);
        request1.setEmail("user@example.com");
        request1.setItems(List.of(new CheckoutRequest.CheckoutItem(testProduct.getId(), 2)));

        OrderEntity firstOrder = orderService.checkout("testuser", request1);
        assertNotNull(firstOrder);
        assertEquals(orderId, firstOrder.getOrderId());
        assertEquals(new BigDecimal("5000.00"), firstOrder.getTotalAmount());

        // Second checkout with the SAME orderId (e.g. network retry or double-click)
        CheckoutRequest request2 = new CheckoutRequest();
        request2.setOrderId(orderId);
        request2.setPaymentId(paymentId);
        request2.setEmail("user@example.com");
        request2.setItems(List.of(new CheckoutRequest.CheckoutItem(testProduct.getId(), 2)));

        OrderEntity duplicateOrder = orderService.checkout("testuser", request2);
        assertNotNull(duplicateOrder);
        assertEquals(firstOrder.getId(), duplicateOrder.getId(), "Duplicate orderId must return the identical existing order");

        // Verify only 1 order exists in database
        List<OrderEntity> allOrders = orderRepo.findAll();
        assertEquals(1, allOrders.size(), "Database must contain exactly 1 order despite multiple checkout attempts");
    }

    @Test
    @DisplayName("Order: Idempotent checkout returns existing order when duplicate paymentId is submitted")
    public void testDuplicatePaymentIdCheckoutIdempotency() {
        String orderId1 = "rzp_order_a";
        String orderId2 = "rzp_order_b";
        String sharedPaymentId = "PAY_MANUAL_SHARED_99";

        CheckoutRequest req1 = new CheckoutRequest();
        req1.setOrderId(orderId1);
        req1.setPaymentId(sharedPaymentId);
        req1.setEmail("user@example.com");
        req1.setItems(List.of(new CheckoutRequest.CheckoutItem(testProduct.getId(), 1)));

        OrderEntity first = orderService.checkout("testuser", req1);

        CheckoutRequest req2 = new CheckoutRequest();
        req2.setOrderId(orderId2);
        req2.setPaymentId(sharedPaymentId);
        req2.setEmail("user@example.com");
        req2.setItems(List.of(new CheckoutRequest.CheckoutItem(testProduct.getId(), 1)));

        OrderEntity second = orderService.checkout("testuser", req2);
        assertEquals(first.getId(), second.getId(), "Duplicate paymentId must return the existing order");
    }

    @Test
    @DisplayName("Order: Paid checkout requires Razorpay signature and rejects missing signature")
    public void testPaidCheckoutRejectsMissingSignature() {
        CheckoutRequest request = new CheckoutRequest();
        request.setOrderId("rzp_order_online_1");
        request.setPaymentId("pay_online_123"); // Non-manual payment
        request.setRazorpaySignature(null);    // Missing signature!
        request.setEmail("user@example.com");
        request.setItems(List.of(new CheckoutRequest.CheckoutItem(testProduct.getId(), 1)));

        assertThrows(SecurityException.class, () -> {
            orderService.checkout("testuser", request);
        }, "Paid checkout without Razorpay signature must throw SecurityException");
    }

    @Test
    @DisplayName("Order: Successful checkout saves outbox event with UUID eventId in same transaction")
    public void testTransactionalOutboxCreatedOnCheckout() {
        CheckoutRequest request = new CheckoutRequest();
        request.setOrderId("rzp_outbox_test_1");
        request.setPaymentId("PAY_MANUAL_OUTBOX_1");
        request.setEmail("user@example.com");
        request.setItems(List.of(new CheckoutRequest.CheckoutItem(testProduct.getId(), 1)));

        OrderEntity order = orderService.checkout("testuser", request);
        assertNotNull(order);

        List<OutboxEvent> outboxEvents = outboxEventRepo.findAll();
        assertEquals(1, outboxEvents.size(), "Checkout must persist exactly 1 outbox event");

        OutboxEvent event = outboxEvents.get(0);
        assertEquals("ORDER_PLACED", event.getEventType());
        assertEquals(order.getOrderId(), event.getAggregateId());
        assertEquals("PENDING", event.getStatus());
        assertNotNull(event.getEventId(), "OutboxEvent must have non-null UUID eventId");
    }

    @Test
    @DisplayName("Webhook: processPaymentWebhook handles duplicate webhooks idempotently")
    public void testWebhookIdempotentHandling() {
        // Create an existing PAID order
        OrderEntity order = new OrderEntity();
        order.setUsername("testuser");
        order.setOrderId("rzp_webhook_order_1");
        order.setPaymentId("pay_webhook_1");
        order.setStatus(OrderStatus.PAID);
        order.setTotalAmount(new BigDecimal("1000.00"));
        orderRepo.save(order);

        // Webhook delivery 1
        assertDoesNotThrow(() ->
                orderService.processPaymentWebhook("pay_webhook_1", "rzp_webhook_order_1", "payment.captured"));

        // Webhook delivery 2 (duplicate delivery from Razorpay)
        assertDoesNotThrow(() ->
                orderService.processPaymentWebhook("pay_webhook_1", "rzp_webhook_order_1", "payment.captured"));

        Optional<OrderEntity> fetched = orderRepo.findByOrderId("rzp_webhook_order_1");
        assertTrue(fetched.isPresent());
        assertEquals(OrderStatus.PAID, fetched.get().getStatus());
    }
}
