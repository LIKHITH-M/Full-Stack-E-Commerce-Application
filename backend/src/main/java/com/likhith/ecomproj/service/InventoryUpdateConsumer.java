package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.OrderPlacedEvent;
import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class InventoryUpdateConsumer {

    @Autowired
    private ProductRepo productRepo;

    @KafkaListener(topics = "order-events", groupId = "inventory-group", autoStartup = "${spring.kafka.listener.auto-startup:true}")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        System.out.println("InventoryUpdateConsumer received event for order: " + event.getOrderId());

        for (OrderPlacedEvent.OrderItem item : event.getItems()) {
            Product product = productRepo.findById(item.getProductId()).orElse(null);
            if (product != null) {
                int newStock = product.getStockQuantity() - item.getQuantity();
                product.setStockQuantity(Math.max(newStock, 0));
                product.setAvailable(product.getStockQuantity() > 0);
                productRepo.save(product);
                System.out.println("Stock updated for product " + product.getName() + ": " + product.getStockQuantity());
            } else {
                System.err.println("Product not found for ID: " + item.getProductId());
            }
        }
    }
}
