package com.likhith.ecomproj.service;

import com.likhith.ecomproj.model.Product;
import com.likhith.ecomproj.repo.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying JPA Optimistic Locking on the Product entity.
 * Uses the H2 in-memory test database configured in application-test.properties.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductStockOptimisticLockingTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private int testProductId;

    @BeforeEach
    void setUp() {
        productRepo.deleteAll();

        Product product = new Product();
        product.setName("Concurrency Test Phone");
        product.setDescription("Phone used for optimistic locking test");
        product.setBrand("TestBrand");
        product.setPrice(new BigDecimal("49999.00"));
        product.setCategory("Electronics");
        product.setReleaseDate(new Date());
        product.setAvailable(true);
        product.setStockQuantity(20);

        Product saved = productRepo.saveAndFlush(product);
        testProductId = saved.getId();
    }

    @Test
    @DisplayName("A. Normal stock decrement works and increments entity version")
    void testNormalStockDecrement() {
        Product initial = productRepo.findById(testProductId).orElseThrow();
        Long initialVersion = initial.getVersion();

        Product updated = productService.decrementStockWithOptimisticLocking(testProductId, 5);

        assertEquals(15, updated.getStockQuantity());
        assertTrue(updated.isAvailable());

        // Verify version incremented in database
        Product reloaded = productRepo.findById(testProductId).orElseThrow();
        assertEquals(15, reloaded.getStockQuantity());
        assertNotNull(reloaded.getVersion());
        if (initialVersion != null) {
            assertEquals(initialVersion + 1, reloaded.getVersion());
        }
    }

    @Test
    @DisplayName("B. Insufficient stock is rejected and stock remains unchanged")
    void testInsufficientStockRejected() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            productService.decrementStockWithOptimisticLocking(testProductId, 25);
        });

        assertTrue(ex.getMessage().contains("Insufficient stock"));

        // Ensure stock was not decremented
        Product reloaded = productRepo.findById(testProductId).orElseThrow();
        assertEquals(20, reloaded.getStockQuantity());
    }

    @Test
    @DisplayName("C. Product-not-found throws IllegalArgumentException")
    void testProductNotFound() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            productService.decrementStockWithOptimisticLocking(999999, 1);
        });

        assertTrue(ex.getMessage().contains("Product not found with ID: 999999"));
    }

    @Test
    @DisplayName("D. Stale version update triggers ObjectOptimisticLockingFailureException")
    void testOptimisticLockingConflictOnStaleEntity() {
        // 1. Thread/Session 1 reads the product at current version
        Product staleProduct = productRepo.findById(testProductId).orElseThrow();

        // 2. Thread/Session 2 updates the product (e.g. through the service), incrementing the version in DB
        productService.decrementStockWithOptimisticLocking(testProductId, 2);

        // 3. Thread/Session 1 tries to save the stale entity with the old version
        staleProduct.setStockQuantity(staleProduct.getStockQuantity() - 3);

        // Attempting to save the entity with the old version MUST throw ObjectOptimisticLockingFailureException
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.execute(status -> productRepo.saveAndFlush(staleProduct));
        });
    }

    @Test
    @DisplayName("E. Concurrent updates trigger optimistic locking and prevent lost-update anomaly")
    void testConcurrentUpdatesPreventLostUpdate() throws InterruptedException {
        int numberOfThreads = 8;
        int decrementPerThread = 1;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger optimisticLockConflictCount = new AtomicInteger(0);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready so they hit the DB simultaneously
                    startLatch.await();
                    productService.decrementStockWithOptimisticLocking(testProductId, decrementPerThread);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException oe) {
                    // Optimistic lock conflict detected!
                    optimisticLockConflictCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Unexpected exception in thread: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously to trigger race condition
        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // Reload the product from DB
        Product finalProduct = productRepo.findById(testProductId).orElseThrow();

        System.out.println("Concurrent Test Results -> Successes: " + successCount.get()
                + ", Optimistic Lock Conflicts: " + optimisticLockConflictCount.get()
                + ", Final Stock: " + finalProduct.getStockQuantity());

        // Verification:
        // 1. All threads either succeeded or caught an optimistic lock failure (never silent corruption)
        assertEquals(numberOfThreads, successCount.get() + optimisticLockConflictCount.get());

        // 2. The final stock MUST mathematically equal initial (20) - (successCount * decrementPerThread)
        // With optimistic locking, NO LOST UPDATES occur!
        int expectedStock = 20 - (successCount.get() * decrementPerThread);
        assertEquals(expectedStock, finalProduct.getStockQuantity(),
                "Final stock must accurately reflect exactly the number of successful decrements, with no lost updates!");
    }
}
