package com.likhith.ecomproj.service;

import com.likhith.ecomproj.dto.ProductSummaryDTO;
import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepo repo;

    public List<Product> getAllProducts() {
        return repo.findAll();
    }

    public Page<ProductSummaryDTO> getAllProductSummaries(Pageable pageable) {
        return repo.findAllSummary(pageable);
    }

    public List<ProductSummaryDTO> getAllProductSummaries() {
        return repo.findAllSummary();
    }

    public Product getProduct(int id) {
        return repo.findById(id).orElse(null);
    }

    public Product addProduct(Product product, MultipartFile imageFile) throws IOException {
        product.setImageName(imageFile.getOriginalFilename());
        product.setImageType(imageFile.getContentType());
        product.setImageData(imageFile.getBytes());
        product.setAvailable(product.getStockQuantity() > 0);

        return repo.save(product);
    }

    @org.springframework.transaction.annotation.Transactional
    public Product updateProduct(int id, Product product, MultipartFile imageFile) throws IOException {
        Product existingProduct = repo.findById(id).orElse(null);
        if (existingProduct == null) {
            return null;
        }

        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setBrand(product.getBrand());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setCategory(product.getCategory());
        if (product.getReleaseDate() != null) {
            existingProduct.setReleaseDate(product.getReleaseDate());
        }
        existingProduct.setStockQuantity(product.getStockQuantity());
        existingProduct.setAvailable(product.getStockQuantity() > 0);

        if (imageFile != null && !imageFile.isEmpty()) {
            existingProduct.setImageName(imageFile.getOriginalFilename());
            existingProduct.setImageType(imageFile.getContentType());
            existingProduct.setImageData(imageFile.getBytes());
        }

        if (existingProduct.getVersion() == null) {
            existingProduct.setVersion(0L);
        }

        return repo.save(existingProduct);
    }

    public void deleteProduct(int id) {
        repo.deleteById(id);
    }


    public List<Product> searchProducts(String keyword) {
        return repo.searchProducts(keyword);
    }

    /**
     * Decrements product stock quantity inside an atomic transaction using JPA Optimistic Locking.
     *
     * How Optimistic Locking works here:
     * - The Product entity has an @Version field.
     * - When repo.save(product) is called, Hibernate verifies that the version in the database
     *   matches the version that was initially loaded in this transaction.
     * - If another concurrent thread updated this product in the meantime, the version numbers will
     *   differ and Hibernate executes UPDATE ... WHERE id = ? AND version = ? which updates 0 rows.
     * - Hibernate detects this and throws ObjectOptimisticLockingFailureException, preventing a lost update.
     *
     * Validations enforced:
     * 1. Product must exist (throws IllegalArgumentException).
     * 2. Requested quantity must be greater than 0 (throws IllegalArgumentException).
     * 3. Stock must be sufficient (throws IllegalStateException, never allowing negative stock).
     *
     * @param productId the ID of the product
     * @param quantity  the quantity to decrement
     * @return the updated Product entity
     * @throws IllegalArgumentException if product is missing or quantity <= 0
     * @throws IllegalStateException    if current stock is insufficient
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if a concurrent update occurs
     */
    @org.springframework.transaction.annotation.Transactional
    public Product decrementStockWithOptimisticLocking(int productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to decrement must be greater than 0. Received: " + quantity);
        }

        Product product = repo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

        if (product.getStockQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for product '" + product.getName()
                    + "' (ID: " + productId + "). Available: " + product.getStockQuantity()
                    + ", Requested: " + quantity);
        }

        int updatedStock = product.getStockQuantity() - quantity;
        product.setStockQuantity(updatedStock);
        product.setAvailable(updatedStock > 0);

        return repo.save(product);
    }

    /**
     * Overloaded method accepting Long productId for callers using Long identifier representations.
     */
    @org.springframework.transaction.annotation.Transactional
    public Product decrementStockWithOptimisticLocking(Long productId, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        return decrementStockWithOptimisticLocking(productId.intValue(), quantity);
    }
}
