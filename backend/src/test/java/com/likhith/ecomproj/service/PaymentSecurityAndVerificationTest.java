package com.likhith.ecomproj.service;

import com.likhith.ecomproj.dto.CheckoutRequest;
import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

public class PaymentSecurityAndVerificationTest {

    private static final String API_SECRET = "test_razorpay_api_secret_key_12345";
    private static final String WEBHOOK_SECRET = "test_razorpay_webhook_secret_67890";

    private RazorPayService razorPayService;

    @Mock
    private ProductRepo productRepo;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        razorPayService = new RazorPayService();
        razorPayService.setApiSecret(API_SECRET);
        razorPayService.setWebhookSecret(WEBHOOK_SECRET);
    }

    private String generateExpectedHmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    @Test
    @DisplayName("Payment: Valid Razorpay payment signature must be verified successfully")
    public void testValidPaymentSignature() throws Exception {
        String orderId = "order_123456";
        String paymentId = "pay_789012";
        String validSignature = generateExpectedHmac(orderId + "|" + paymentId, API_SECRET);

        boolean result = razorPayService.verifyPaymentSignature(orderId, paymentId, validSignature);
        assertTrue(result, "Valid signature matching HMAC-SHA256 must return true");
    }

    @Test
    @DisplayName("Payment: Tampered/invalid Razorpay payment signature must be rejected")
    public void testInvalidPaymentSignature() {
        String orderId = "order_123456";
        String paymentId = "pay_789012";
        String fakeSignature = "invalid_forged_hex_signature_abcdef0123456789";

        boolean result = razorPayService.verifyPaymentSignature(orderId, paymentId, fakeSignature);
        assertFalse(result, "Forged signature must return false");
    }

    @Test
    @DisplayName("Payment: Missing signature or null parameters must be rejected immediately")
    public void testMissingPaymentSignature() {
        assertFalse(razorPayService.verifyPaymentSignature("order_1", "pay_1", null), "Null signature must be rejected");
        assertFalse(razorPayService.verifyPaymentSignature("order_1", "pay_1", "   "), "Blank signature must be rejected");
        assertFalse(razorPayService.verifyPaymentSignature(null, "pay_1", "sig"), "Null orderId must be rejected");
        assertFalse(razorPayService.verifyPaymentSignature("order_1", null, "sig"), "Null paymentId must be rejected");
    }

    @Test
    @DisplayName("Payment: Razorpay webhook signature verification must use separate webhook secret")
    public void testWebhookSignatureVerification() throws Exception {
        String webhookPayload = "{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_999\",\"order_id\":\"order_888\"}}}}";
        String validWebhookSignature = generateExpectedHmac(webhookPayload, WEBHOOK_SECRET);

        assertTrue(razorPayService.verifyWebhookSignature(webhookPayload, validWebhookSignature),
                "Valid webhook signature with WEBHOOK_SECRET must return true");

        // Attempting to verify webhook with API secret must FAIL
        String signatureWithWrongSecret = generateExpectedHmac(webhookPayload, API_SECRET);
        assertFalse(razorPayService.verifyWebhookSignature(webhookPayload, signatureWithWrongSecret),
                "Webhook signed with API_SECRET must NOT verify against WEBHOOK_SECRET (secrets must be isolated)");

        // Missing signature must fail
        assertFalse(razorPayService.verifyWebhookSignature(webhookPayload, null));
        assertFalse(razorPayService.verifyWebhookSignature(null, validWebhookSignature));
    }

    @Test
    @DisplayName("Financial: Server-side price calculation ignores any frontend amount tampering")
    public void testServerSidePriceCalculation() {
        Product p1 = new Product();
        p1.setId(1);
        p1.setName("Laptop");
        p1.setPrice(new BigDecimal("50000.00"));
        p1.setStockQuantity(10);

        Product p2 = new Product();
        p2.setId(2);
        p2.setName("Mouse");
        p2.setPrice(new BigDecimal("1500.00"));
        p2.setStockQuantity(50);

        when(productRepo.findAllByIdIn(anyList())).thenReturn(List.of(p1, p2));

        // Client attempts to checkout 1 Laptop and 2 Mice (Actual cost = 50000 + 3000 = 53000)
        List<CheckoutRequest.CheckoutItem> items = List.of(
                new CheckoutRequest.CheckoutItem(1, 1),
                new CheckoutRequest.CheckoutItem(2, 2)
        );

        BigDecimal calculatedTotal = orderService.calculateServerTotal(items);

        assertEquals(new BigDecimal("53000.00"), calculatedTotal,
                "Backend price calculation must strictly evaluate to DB prices: 1*50000 + 2*1500 = 53000");

        // Even if frontend passed an amount of ₹1 in its request, the server calculated total is unchanged
        assertNotEquals(BigDecimal.ONE, calculatedTotal);
    }
}
