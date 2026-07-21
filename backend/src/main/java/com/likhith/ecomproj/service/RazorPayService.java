package com.likhith.ecomproj.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorPayService {

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    public String createOrder(int amount, String currency, String receiptId) throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);

        JSONObject razorPayRequest = new JSONObject();
        razorPayRequest.put("amount", amount * 100); // Razorpay expects amount in paise
        razorPayRequest.put("currency", currency);
        razorPayRequest.put("receipt", receiptId);

        Order order = razorpayClient.orders.create(razorPayRequest);
        return order.toString();
    }
}
