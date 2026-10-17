package com.likhith.ecomproj.controller;

import com.likhith.ecomproj.model.CartItem;
import com.likhith.ecomproj.repo.CartItemRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartItemRepo cartItemRepo;

    /**
     * Get all cart items for the logged-in user.
     */
    @GetMapping
    public ResponseEntity<List<CartItem>> getCart() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return new ResponseEntity<>(cartItemRepo.findByUsername(username), HttpStatus.OK);
    }

    /**
     * Add a product to cart or increase quantity if already exists.
     */
    @PostMapping
    public ResponseEntity<CartItem> addToCart(@RequestBody CartItem item) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        item.setUsername(username);

        Optional<CartItem> existing = cartItemRepo.findByUsernameAndProductId(username, item.getProductId());
        if (existing.isPresent()) {
            CartItem cart = existing.get();
            cart.setQuantity(cart.getQuantity() + 1);
            return new ResponseEntity<>(cartItemRepo.save(cart), HttpStatus.OK);
        } else {
            if (item.getQuantity() <= 0) item.setQuantity(1);
            return new ResponseEntity<>(cartItemRepo.save(item), HttpStatus.CREATED);
        }
    }

    /**
     * Update quantity of a cart item.
     */
    @PutMapping("/{productId}")
    public ResponseEntity<CartItem> updateQuantity(@PathVariable int productId, @RequestParam int quantity) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<CartItem> existing = cartItemRepo.findByUsernameAndProductId(username, productId);
        if (existing.isPresent()) {
            CartItem cart = existing.get();
            cart.setQuantity(Math.max(quantity, 1));
            return new ResponseEntity<>(cartItemRepo.save(cart), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Remove a specific product from cart.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable int productId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartItemRepo.deleteByUsernameAndProductId(username, productId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Clear entire cart for the logged-in user (called after successful checkout).
     */
    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        cartItemRepo.deleteByUsername(username);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
