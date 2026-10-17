package com.likhith.ecomproj.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "product", indexes = {
    @Index(name = "idx_product_category", columnList = "category"),
    @Index(name = "idx_product_brand", columnList = "brand")
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String description;
    private String brand;
    private BigDecimal price;
    private String category;
    private Date releaseDate;
    private boolean available;
    private int stockQuantity;

    private String imageName;
    private String imageType;
    @Lob
    private byte[] imageData;

    /**
     * Optimistic Locking Version field.
     *
     * What @Version does:
     * - Hibernate automatically increments this number on each UPDATE statement.
     * - When updating, Hibernate executes:
     *   UPDATE product SET stock_quantity = ?, version = version + 1 WHERE id = ? AND version = ?
     *
     * Why Optimistic Locking is needed:
     * - In an e-commerce platform, concurrent checkout orders or Kafka consumers may read the same
     *   initial stock concurrently. Without locking, one transaction overwrites another's decrement
     *   (the lost-update anomaly).
     *
     * What happens on concurrency:
     * - If Transaction A and Transaction B both read version = 0, and Transaction A commits first,
     *   the row's version becomes 1.
     * - When Transaction B attempts to commit with WHERE version = 0, 0 rows are updated.
     * - Hibernate detects this and throws ObjectOptimisticLockingFailureException, preventing data corruption.
     */
    @Version
    private Long version = 0L;

}
