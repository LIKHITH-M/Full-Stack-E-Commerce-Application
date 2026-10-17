package com.likhith.ecomproj.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for checkout. Contains only the data the client should provide.
 * Prices and totals are calculated server-side from the database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    private String paymentId;
    private String orderId;
    private String email;
    /** Razorpay signature for payment verification. Null for manual checkout. */
    private String razorpaySignature;
    private List<CheckoutItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutItem {
        private int productId;
        private int quantity;
    }
}
