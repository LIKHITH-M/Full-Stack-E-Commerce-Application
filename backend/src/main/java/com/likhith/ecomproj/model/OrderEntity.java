package com.likhith.ecomproj.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_username", columnList = "username"),
    @Index(name = "idx_order_order_id", columnList = "orderId"),
    @Index(name = "idx_order_payment_id", columnList = "paymentId")
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String username;
    private String email;
    private BigDecimal totalAmount;

    @Column(unique = true)
    private String paymentId;

    @Column(unique = true)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(columnDefinition = "TEXT")
    private String orderItems; // JSON string of items
}
