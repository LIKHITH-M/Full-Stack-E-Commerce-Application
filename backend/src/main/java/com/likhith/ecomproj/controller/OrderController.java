package com.likhith.ecomproj.controller;

import com.likhith.ecomproj.dto.CheckoutRequest;
import com.likhith.ecomproj.dto.OrderResponseDTO;
import com.likhith.ecomproj.model.OrderEntity;
import com.likhith.ecomproj.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Thin controller — all business logic delegated to OrderService.
 * Returns clean OrderResponseDTOs to avoid exposing database entity internals.
 */
@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * Checkout endpoint — called by frontend after successful payment.
     * Accepts CheckoutRequest (no client-supplied prices) and delegates to OrderService.
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDTO> checkout(@RequestBody CheckoutRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Checkout initiated by user: {}", username);

        OrderEntity order = orderService.checkout(username, request);
        return new ResponseEntity<>(orderService.toDTO(order), HttpStatus.CREATED);
    }

    /**
     * Get orders for the currently logged-in user.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponseDTO>> getMyOrders() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return new ResponseEntity<>(orderService.getOrdersDTOByUsername(username), HttpStatus.OK);
    }

    /**
     * Admin: Get all orders.
     */
    @GetMapping("/all")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return new ResponseEntity<>(orderService.getAllOrdersDTO(), HttpStatus.OK);
    }
}
