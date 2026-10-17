package com.likhith.ecomproj.service;

import com.likhith.ecomproj.dto.ProductSummaryDTO;
import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ProductPerformanceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepo productRepo;

    @BeforeEach
    public void setUp() {
        productRepo.deleteAll();

        for (int i = 1; i <= 25; i++) {
            Product p = new Product();
            p.setName("Product " + i);
            p.setDescription("Description " + i);
            p.setBrand("Brand " + (i % 3));
            p.setPrice(new BigDecimal("100.00").multiply(BigDecimal.valueOf(i)));
            p.setCategory("Category " + (i % 2));
            p.setReleaseDate(new Date());
            p.setAvailable(true);
            p.setStockQuantity(50);
            p.setImageName("image_" + i + ".png");
            p.setImageType("image/png");
            // Heavy 10KB BLOB to simulate image storage
            p.setImageData(new byte[10240]);
            productRepo.save(p);
        }
    }

    @Test
    @DisplayName("Performance: ProductSummaryDTO list excludes heavy imageData BLOB")
    public void testProductSummaryExcludesImageData() {
        List<ProductSummaryDTO> summaries = productService.getAllProductSummaries();
        assertEquals(25, summaries.size());

        for (ProductSummaryDTO summary : summaries) {
            assertNotNull(summary.getName());
            assertNotNull(summary.getPrice());
            // Verify by reflection that ProductSummaryDTO has NO imageData field
            boolean hasImageDataField = false;
            for (var field : ProductSummaryDTO.class.getDeclaredFields()) {
                if ("imageData".equals(field.getName())) {
                    hasImageDataField = true;
                    break;
                }
            }
            assertFalse(hasImageDataField, "ProductSummaryDTO must not have imageData BLOB field");
        }
    }

    @Test
    @DisplayName("Performance: Spring Data Pagination returns correct page slices and metadata")
    public void testProductPagination() {
        Page<ProductSummaryDTO> page0 = productService.getAllProductSummaries(PageRequest.of(0, 10));
        assertEquals(10, page0.getNumberOfElements());
        assertEquals(25, page0.getTotalElements());
        assertEquals(3, page0.getTotalPages());
        assertTrue(page0.hasNext());

        Page<ProductSummaryDTO> page2 = productService.getAllProductSummaries(PageRequest.of(2, 10));
        assertEquals(5, page2.getNumberOfElements(), "Third page with size 10 should contain 5 remaining items");
        assertFalse(page2.hasNext());
    }
}
