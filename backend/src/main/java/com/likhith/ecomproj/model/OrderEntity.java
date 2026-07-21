package com.likhith.ecomproj.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "orders")
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
    private String paymentId;
    private String orderId;
    private String status; // PLACED, CONFIRMED, SHIPPED, DELIVERED

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @Column(columnDefinition = "TEXT")
    private String orderItems; // JSON string of items
}
