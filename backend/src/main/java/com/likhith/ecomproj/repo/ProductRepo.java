package com.likhith.ecomproj.repo;

import com.likhith.ecomproj.dto.ProductSummaryDTO;
import com.likhith.ecomproj.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepo extends JpaRepository<Product, Integer> {

    @Query("SELECT p from Product p WHERE " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchProducts(String keyword);

    /**
     * Returns a page of lightweight ProductSummaryDTOs excluding imageData BLOB.
     * Reduces product list payload by ~95%.
     */
    @Query("SELECT new com.likhith.ecomproj.dto.ProductSummaryDTO(" +
            "p.id, p.name, p.description, p.brand, p.price, p.category, " +
            "p.releaseDate, p.available, p.stockQuantity, p.imageName, p.imageType) " +
            "FROM Product p")
    Page<ProductSummaryDTO> findAllSummary(Pageable pageable);

    /**
     * Non-paginated version for backward compatibility.
     */
    @Query("SELECT new com.likhith.ecomproj.dto.ProductSummaryDTO(" +
            "p.id, p.name, p.description, p.brand, p.price, p.category, " +
            "p.releaseDate, p.available, p.stockQuantity, p.imageName, p.imageType) " +
            "FROM Product p")
    List<ProductSummaryDTO> findAllSummary();

    List<Product> findAllByIdIn(List<Integer> ids);
}
