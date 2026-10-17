package com.likhith.ecomproj.controller;

import com.likhith.ecomproj.service.OrderService;
import com.likhith.ecomproj.service.RazorPayService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller receiving Razorpay webhook events.
 *
 * Requirements:
 * - Must verify X-Razorpay-Signature using RAZORPAY_WEBHOOK_SECRET (separate from API secret).
 * - Must NOT become an independent order-creation mechanism (does not duplicate order logic).
 * - Updates order state idempotently to PAID upon receiving "payment.captured" or "order.paid".
 * - Safely handles repeated/duplicate webhook deliveries without errors or duplications.
 */
@RestController
@RequestMapping("/api/payments")
@Slf4j
public class WebhookController {

    @Autowired
    private RazorPayService razorPayService;

    @Autowired
    private OrderService orderService;

    @PostMapping("/webhook")
    public ResponseEntity<?> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Received Razorpay webhook delivery");

        // 1. Verify webhook signature using dedicated webhook secret
        if (signature == null || !razorPayService.verifyWebhookSignature(rawPayload, signature)) {
            log.warn("Invalid or missing Razorpay webhook signature");
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid webhook signature");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // 2. Parse payload safely
        try {
            JSONObject json = new JSONObject(rawPayload);
            String event = json.optString("event");
            log.info("Razorpay webhook event type: {}", event);

            if ("payment.captured".equals(event) || "order.paid".equals(event)) {
                JSONObject payloadObj = json.getJSONObject("payload");
                JSONObject paymentObj = payloadObj.getJSONObject("payment").getJSONObject("entity");

                String paymentId = paymentObj.optString("id");
                String orderId = paymentObj.optString("order_id");

                // 3. Delegate to OrderService for idempotent state transition
                orderService.processPaymentWebhook(paymentId, orderId, event);
            }

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook payload: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", "Webhook processing failed");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}
