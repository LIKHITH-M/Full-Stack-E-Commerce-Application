package com.likhith.ecomproj.dto;

import com.likhith.ecomproj.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Clean response DTO for orders, abstracting database entity internals.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private int id;
    private String username;
    private String email;
    private BigDecimal totalAmount;
    private String paymentId;
    private String orderId;
    private OrderStatus status;
    private Date createdAt;
    private String orderItems;
}
