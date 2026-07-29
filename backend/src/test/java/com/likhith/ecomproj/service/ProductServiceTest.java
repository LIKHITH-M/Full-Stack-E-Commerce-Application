package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepo productRepo;

    @InjectMocks
    private ProductService productService;

    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        product1 = new Product();
        product1.setId(1);
        product1.setName("Wireless Mouse");
        product1.setBrand("Logitech");
        product1.setPrice(new BigDecimal("29.99"));
        product1.setCategory("Electronics");
        product1.setStockQuantity(50);
        product1.setAvailable(true);

        product2 = new Product();
        product2.setId(2);
        product2.setName("Gaming Keyboard");
        product2.setBrand("Razer");
        product2.setPrice(new BigDecimal("89.99"));
        product2.setCategory("Electronics");
        product2.setStockQuantity(20);
        product2.setAvailable(true);
    }

    @Test
    void testGetAllProducts() {
        when(productRepo.findAll()).thenReturn(Arrays.asList(product1, product2));

        List<Product> products = productService.getAllProducts();

        assertEquals(2, products.size());
        verify(productRepo, times(1)).findAll();
    }

    @Test
    void testGetProduct_Success() {
        when(productRepo.findById(1)).thenReturn(Optional.of(product1));

        Product foundProduct = productService.getProduct(1);

        assertNotNull(foundProduct);
        assertEquals("Wireless Mouse", foundProduct.getName());
        verify(productRepo, times(1)).findById(1);
    }

    @Test
    void testGetProduct_NotFound() {
        when(productRepo.findById(99)).thenReturn(Optional.empty());

        Product foundProduct = productService.getProduct(99);

        assertNull(foundProduct);
        verify(productRepo, times(1)).findById(99);
    }

    @Test
    void testSearchProducts() {
        when(productRepo.searchProducts("Mouse")).thenReturn(List.of(product1));

        List<Product> results = productService.searchProducts("Mouse");

        assertEquals(1, results.size());
        assertEquals("Wireless Mouse", results.get(0).getName());
        verify(productRepo, times(1)).searchProducts("Mouse");
    }

    @Test
    void testDeleteProduct() {
        doNothing().when(productRepo).deleteById(1);

        productService.deleteProduct(1);

        verify(productRepo, times(1)).deleteById(1);
    }
}
