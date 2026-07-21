package com.likhith.ecomproj.controller;

import com.likhith.ecomproj.service.RazorPayService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin
public class PaymentController {

    @Autowired
    private RazorPayService razorPayService;

    @Value("${razorpay.api.key}")
    private String razorpayKeyId;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestParam int amount, @RequestParam String currency, @RequestParam String receiptId) {
        try {
            String order = razorPayService.createOrder(amount, currency, receiptId);
            Map<String, Object> response = new HashMap<>();
            response.put("order", order);
            response.put("razorpayKeyId", razorpayKeyId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RazorpayException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create order: " + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
