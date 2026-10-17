package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.CartItem;
import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.CartItemRepo;
import com.likhith.ecomproj.repo.ProductRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Cart service encapsulating cart business logic.
 *
 * Fixes the race condition in addToCart:
 * Without @Transactional, two concurrent requests for the same user+product
 * would both see "no existing item" and both insert, causing duplicates or errors.
 * With @Transactional and a DataIntegrityViolationException fallback, we safely
 * handle the race by catching the unique constraint violation and falling back
 * to an increment.
 */
@Service
@Slf4j
public class CartService {

    @Autowired
    private CartItemRepo cartItemRepo;

    @Autowired
    private ProductRepo productRepo;

    public List<CartItem> getCartItems(String username) {
        return cartItemRepo.findByUsername(username);
    }

    /**
     * Adds a product to the cart, or increments the quantity if already present.
     * Handles the race condition where two concurrent requests try to add the same product.
     */
    @Transactional
    public CartItem addToCart(String username, int productId, int quantity) {
        Optional<CartItem> existingItem = cartItemRepo.findByUsernameAndProductId(username, productId);

        if (existingItem.isPresent()) {
            // Product already in cart — increment quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            log.info("Cart updated: user={}, product={}, newQty={}", username, productId, item.getQuantity());
            return cartItemRepo.save(item);
        }

        // Product not in cart — try to insert
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        CartItem newItem = new CartItem();
        newItem.setUsername(username);
        newItem.setProductId(productId);
        newItem.setProductName(product.getName());
        newItem.setPrice(product.getPrice());
        newItem.setQuantity(quantity);
        newItem.setBrand(product.getBrand());
        newItem.setStockQuantity(product.getStockQuantity());

        try {
            CartItem saved = cartItemRepo.save(newItem);
            log.info("Cart item added: user={}, product={}, qty={}", username, productId, quantity);
            return saved;
        } catch (DataIntegrityViolationException e) {
            // Race condition: Another concurrent request inserted this product simultaneously.
            // Fall back to incrementing the quantity of the now-existing item.
            log.warn("Cart race condition detected for user={}, product={}. Falling back to increment.", username, productId);
            CartItem existing = cartItemRepo.findByUsernameAndProductId(username, productId)
                    .orElseThrow(() -> new RuntimeException("Unexpected state: cart item vanished after conflict"));
            existing.setQuantity(existing.getQuantity() + quantity);
            return cartItemRepo.save(existing);
        }
    }

    @Transactional
    public void removeItem(String username, int productId) {
        cartItemRepo.deleteByUsernameAndProductId(username, productId);
        log.info("Cart item removed: user={}, product={}", username, productId);
    }

    @Transactional
    public void clearCart(String username) {
        cartItemRepo.deleteByUsername(username);
        log.info("Cart cleared for user: {}", username);
    }
}
