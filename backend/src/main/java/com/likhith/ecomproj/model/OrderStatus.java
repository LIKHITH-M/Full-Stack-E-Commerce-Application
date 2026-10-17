package com.likhith.ecomproj.model;

/**
 * Represents the lifecycle states of an order.
 * Prevents invalid status values by replacing raw String status.
 */
public enum OrderStatus {
    PENDING,      // Order created, awaiting payment confirmation
    PAID,         // Payment verified successfully
    PLACED,       // Order confirmed and placed
    PROCESSING,   // Inventory being updated, notifications being sent
    SHIPPED,      // Order shipped to customer
    DELIVERED,    // Order delivered to customer
    CANCELLED,    // Order cancelled
    FAILED        // Order or payment failed
}
