package com.likhith.ecomproj.controller;

import com.likhith.ecomproj.dto.CheckoutRequest;
import com.likhith.ecomproj.model.CartItem;
import com.likhith.ecomproj.service.CartService;
import com.likhith.ecomproj.service.OrderService;
import com.likhith.ecomproj.service.RazorPayService;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Payment controller for Razorpay checkout integration.
 *
 * Enforces SERVER-SIDE PRICE CALCULATION:
 * - Never trusts any client-supplied amount or price.
 * - Always reads products from the database, validates stock, and calculates total server-side.
 */
@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    @Autowired
    private RazorPayService razorPayService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Value("${razorpay.api.key}")
    private String razorpayKeyId;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @RequestParam(required = false) Integer amount,
            @RequestParam(defaultValue = "INR") String currency,
            @RequestParam(required = false) String receiptId,
            @RequestBody(required = false) List<CheckoutRequest.CheckoutItem> requestItems) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Creating Razorpay payment order for user: {}", username);

        List<CheckoutRequest.CheckoutItem> itemsToPrice = requestItems;

        // If items were not sent in request body, retrieve items from the user's active cart in DB
        if (itemsToPrice == null || itemsToPrice.isEmpty()) {
            List<CartItem> cartItems = cartService.getCartItems(username);
            if (cartItems.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Cart is empty. Cannot create payment order.");
                return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
            }
            itemsToPrice = cartItems.stream()
                    .map(item -> new CheckoutRequest.CheckoutItem(item.getProductId(), item.getQuantity()))
                    .collect(Collectors.toList());
        }

        // Calculate total amount STRICTLY from database product records
        BigDecimal serverTotal = orderService.calculateServerTotal(itemsToPrice);

        if (amount != null && amount != serverTotal.intValue()) {
            log.warn("Potential price tampering detected for user {}: frontend passed amount {}, but server-calculated amount is {}",
                    username, amount, serverTotal);
        }

        int finalAmount = serverTotal.intValue();
        String finalReceipt = (receiptId != null && !receiptId.trim().isEmpty())
                ? receiptId.trim()
                : "rcpt_" + System.currentTimeMillis();

        try {
            String order = razorPayService.createOrder(finalAmount, currency, finalReceipt);
            Map<String, Object> response = new HashMap<>();
            response.put("order", order);
            response.put("razorpayKeyId", razorpayKeyId);
            response.put("serverCalculatedAmount", finalAmount);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for user {}: {}", username, e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create payment order: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
