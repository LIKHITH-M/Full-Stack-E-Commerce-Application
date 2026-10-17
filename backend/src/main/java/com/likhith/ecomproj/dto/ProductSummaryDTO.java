package com.likhith.ecomproj.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Lightweight Product DTO for list/catalog endpoints.
 * Excludes the heavy imageData BLOB to reduce payload size by ~95%.
 * Frontend fetches images separately via GET /api/product/{id}/image.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDTO {
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
}
