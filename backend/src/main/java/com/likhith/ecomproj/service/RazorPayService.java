package com.likhith.ecomproj.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@Slf4j
public class RazorPayService {

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    public String createOrder(int amount, String currency, String receiptId) throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);

        JSONObject razorPayRequest = new JSONObject();
        razorPayRequest.put("amount", amount * 100); // Razorpay expects amount in paise
        razorPayRequest.put("currency", currency);
        razorPayRequest.put("receipt", receiptId);

        Order order = razorpayClient.orders.create(razorPayRequest);
        log.info("Razorpay order created with receipt: {}", receiptId);
        return order.toString();
    }

    /**
     * Verifies Razorpay payment signature using HMAC-SHA256 with the API secret.
     *
     * Razorpay generates: HMAC_SHA256(razorpay_order_id + "|" + razorpay_payment_id, apiSecret)
     *
     * Security Requirements:
     * - Rejects missing/blank orderId, paymentId, or signature
     * - Uses constant-time comparison (MessageDigest.isEqual) to prevent timing attacks
     * - Never logs or exposes the API secret
     *
     * @param razorpayOrderId   The Razorpay order ID
     * @param razorpayPaymentId The Razorpay payment ID
     * @param razorpaySignature The signature sent in the client callback
     * @return true if signature is cryptographically valid
     */
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        if (razorpayOrderId == null || razorpayOrderId.trim().isEmpty() ||
            razorpayPaymentId == null || razorpayPaymentId.trim().isEmpty() ||
            razorpaySignature == null || razorpaySignature.trim().isEmpty()) {
            log.warn("Razorpay payment signature rejected: missing required signature parameters");
            return false;
        }

        if (apiSecret == null || apiSecret.trim().isEmpty()) {
            log.error("Razorpay API secret is not configured; cannot verify signature");
            return false;
        }

        try {
            String payload = razorpayOrderId.trim() + "|" + razorpayPaymentId.trim();
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);

            boolean valid = MessageDigest.isEqual(
                    generatedSignature.getBytes(StandardCharsets.UTF_8),
                    razorpaySignature.trim().getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("Razorpay payment signature verification FAILED for order: {}", razorpayOrderId);
            } else {
                log.info("Razorpay payment signature successfully verified for order: {}", razorpayOrderId);
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Razorpay payment signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verifies Razorpay webhook signature using HMAC-SHA256 with the dedicated Webhook secret.
     *
     * The webhook signature verification is strictly separated from the payment signature:
     * - Uses RAZORPAY_WEBHOOK_SECRET (not the API secret)
     * - Rejects missing payload, signature, or unconfigured webhook secret
     * - Constant-time comparison
     *
     * @param payload          Raw webhook JSON request body
     * @param webhookSignature X-Razorpay-Signature header value
     * @return true if webhook signature is valid
     */
    public boolean verifyWebhookSignature(String payload, String webhookSignature) {
        if (payload == null || payload.trim().isEmpty() ||
            webhookSignature == null || webhookSignature.trim().isEmpty()) {
            log.warn("Razorpay webhook signature rejected: missing payload or signature");
            return false;
        }

        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            log.error("Razorpay Webhook secret is not configured; cannot verify webhook signature");
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);

            boolean valid = MessageDigest.isEqual(
                    generatedSignature.getBytes(StandardCharsets.UTF_8),
                    webhookSignature.trim().getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("Razorpay webhook signature verification FAILED");
            } else {
                log.info("Razorpay webhook signature successfully verified");
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying Razorpay webhook signature: {}", e.getMessage());
            return false;
        }
    }

    // Package-private setters for testing without Spring context
    void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }
}
