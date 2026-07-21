package com.likhith.ecomproj.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO published to Kafka when an order is placed.
 * Consumed by EmailNotificationConsumer and InventoryUpdateConsumer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent implements Serializable {

    private String username;
    private String email;
    private String paymentId;
    private String orderId;
    private BigDecimal totalAmount;
    private List<OrderItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem implements Serializable {
        private int productId;
        private String productName;
        private int quantity;
        private BigDecimal price;
    }
}
