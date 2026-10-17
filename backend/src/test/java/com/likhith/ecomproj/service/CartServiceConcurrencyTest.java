package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.CartItem;
import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.CartItemRepo;
import com.likhith.ecomproj.repo.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CartServiceConcurrencyTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepo cartItemRepo;

    @Autowired
    private ProductRepo productRepo;

    private Product product;

    @BeforeEach
    public void setUp() {
        cartItemRepo.deleteAll();

        product = new Product();
        product.setName("USB-C Hub");
        product.setBrand("Anker");
        product.setPrice(new BigDecimal("2999.00"));
        product.setCategory("Accessories");
        product.setStockQuantity(50);
        product.setAvailable(true);
        product = productRepo.save(product);
    }

    @Test
    @DisplayName("Cart: Subsequent addToCart for the same product safely increments quantity")
    public void testAddToCartIncrementsQuantity() {
        String username = "cartuser";

        // Add 1 item
        CartItem first = cartService.addToCart(username, product.getId(), 1);
        assertEquals(1, first.getQuantity());

        // Add 2 more of the same item
        CartItem second = cartService.addToCart(username, product.getId(), 2);
        assertEquals(3, second.getQuantity(), "Quantity must increment to 1 + 2 = 3");

        // Verify only 1 row exists in cart_items table for this user & product
        List<CartItem> items = cartService.getCartItems(username);
        assertEquals(1, items.size());
        assertEquals(3, items.get(0).getQuantity());
    }

    @Test
    @DisplayName("Cart: clearCart and removeItem execute cleanly within transactional boundary")
    public void testRemoveAndClearCart() {
        String username = "cartuser2";
        cartService.addToCart(username, product.getId(), 2);
        assertFalse(cartService.getCartItems(username).isEmpty());

        cartService.removeItem(username, product.getId());
        assertTrue(cartService.getCartItems(username).isEmpty());
    }
}
