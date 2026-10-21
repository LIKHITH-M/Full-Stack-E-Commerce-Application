package com.likhith.ecomproj.controller;

import com.likhith.ecomproj.dto.ProductSummaryDTO;
import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for product catalog management and discovery.
 *
 * Performance Improvements:
 * 1. Excludes imageData BLOB from listing queries via ProductSummaryDTO projection (~95% smaller payload).
 * 2. Supports Spring Data pagination via ?page=0&size=20 with backwards-compatible unpaginated fallback.
 * 3. Keeps separate /api/product/{id}/image endpoint for on-demand image retrieval.
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class ProductController {

    @Autowired
    private ProductService service;

    /**
     * Get products catalog using lightweight DTO (no BLOB image data).
     * If page or size is specified, returns a Page<ProductSummaryDTO>.
     * If neither is specified, returns a List<ProductSummaryDTO> for seamless backwards compatibility.
     */
    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        if (page != null || size != null) {
            int pageNumber = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 20;
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<ProductSummaryDTO> pageResult = service.getAllProductSummaries(pageable);
            return new ResponseEntity<>(pageResult, HttpStatus.OK);
        }

        return new ResponseEntity<>(service.getAllProductSummaries(), HttpStatus.OK);
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable int id) {
        Product product = service.getProduct(id);
        if (product != null) {
            return new ResponseEntity<>(product, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/product")
    public ResponseEntity<?> addProduct(@RequestPart Product product, @RequestPart MultipartFile imageFile) {
        try {
            Product product1 = service.addProduct(product, imageFile);
            log.info("Product created successfully with ID: {}", product1.getId());
            return new ResponseEntity<>(product1, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Failed to add product: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/product/{productId}/image")
    public ResponseEntity<byte[]> getImageByProductId(@PathVariable int productId) {
        Product product = service.getProduct(productId);
        if (product == null || product.getImageData() == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        byte[] imageFile = product.getImageData();
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(product.getImageType() != null ? product.getImageType() : "image/jpeg"))
                .body(imageFile);
    }

    @PutMapping("/product/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable int id, 
                                                @RequestPart Product product, 
                                                @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {
        try {
            Product updated = service.updateProduct(id, product, imageFile);
            if (updated != null) {
                log.info("Product updated with ID: {}", id);
                return new ResponseEntity<>("updated", HttpStatus.OK);
            }
            return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to update product {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>("Failed to update: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/product/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable int id) {
        Product product = service.getProduct(id);
        if (product != null) {
            service.deleteProduct(id);
            log.info("Product deleted with ID: {}", id);
            return new ResponseEntity<>("Deleted", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        log.info("Searching products with keyword: {}", keyword);
        List<Product> products = service.searchProducts(keyword);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }
}