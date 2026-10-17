package com.likhith.ecomproj.controller;

import com.likhith.ecomproj.model.CartItem;
import com.likhith.ecomproj.service.CartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cart controller — delegates all business logic to CartService.
 * Fixes the original race condition in addToCart by using CartService's
 * @Transactional method with DataIntegrityViolationException fallback.
 */
@RestController
@RequestMapping("/api/cart")
@Slf4j
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * Get all cart items for the logged-in user.
     */
    @GetMapping
    public ResponseEntity<List<CartItem>> getCart() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return new ResponseEntity<>(cartService.getCartItems(username), HttpStatus.OK);
    }

    /**
     * Add a product to cart or increase quantity if already exists.
     */
    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestBody CartItem item) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        int quantity = (item.getQuantity() <= 0) ? 1 : item.getQuantity();
        CartItem result = cartService.addToCart(username, item.getProductId(), quantity);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    /**
     * Update quantity of a cart item.
     */
    @PutMapping("/{productId}")
    public ResponseEntity<CartItem> updateQuantity(@PathVariable int productId, @RequestParam int quantity) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Reuse addToCart with absolute quantity by removing existing and re-adding
        // For now, keep the simple direct update via the service
        List<CartItem> items = cartService.getCartItems(username);
        for (CartItem existing : items) {
            if (existing.getProductId() == productId) {
                existing.setQuantity(Math.max(quantity, 1));
                return new ResponseEntity<>(existing, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Remove a specific product from cart.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable int productId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.removeItem(username, productId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Clear entire cart for the logged-in user.
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartService.clearCart(username);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
