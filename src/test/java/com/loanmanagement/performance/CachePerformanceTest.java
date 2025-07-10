package com.loanmanagement.performance;

import com.loanmanagement.customer.application.port.out.CustomerRepository;
import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.shared.infrastructure.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cache Performance Testing
 * Tests Redis cache performance under various load conditions
 */
@SpringBootTest
@ActiveProfiles("performance")
@TestPropertySource(properties = {
        "spring.cache.type=redis",
        "spring.redis.host=localhost",
        "spring.redis.port=6379",
        "spring.redis.timeout=2000ms",
        "spring.redis.lettuce.pool.max-active=20",
        "spring.redis.lettuce.pool.max-idle=10",
        "spring.redis.lettuce.pool.min-idle=5"
})
@DisplayName("Cache Performance Tests")
class CachePerformanceTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private CacheManager cacheManager;
    
    @Autowired
    private ExecutorService loadTestExecutorService;
    
    @Autowired
    private PerformanceMetricsCollector metricsCollector;
    
    @Autowired
    private LoadTestDataGenerator dataGenerator;
    
    @BeforeEach
    void setUp() {
        metricsCollector.clearMetrics();
        clearAllCaches();
    }
    
    @Test
    @DisplayName("Cache Hit vs Miss Performance")
    void cacheHitVsMissPerformance() throws InterruptedException {
        // Setup test data
        setupTestDataForCacheTest();
        
        int concurrentRequests = 100;
        
        metricsCollector.startCollection();
        
        // Test cache misses (cold cache)
        CountDownLatch coldCacheLatch = new CountDownLatch(concurrentRequests);
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    Long customerId = (long) (requestIndex % 10 + 1);
                    customerRepository.findById(customerId);
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    metricsCollector.recordOperation("CacheMiss", responseTime, true);
                    
                } catch (Exception e) {
                    metricsCollector.recordOperation("CacheMiss", 0, false);
                } finally {
                    coldCacheLatch.countDown();
                }
            });
        }
        
        assertTrue(coldCacheLatch.await(30, TimeUnit.SECONDS), "Cold cache test should complete");
        
        // Test cache hits (warm cache)
        CountDownLatch warmCacheLatch = new CountDownLatch(concurrentRequests);
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    Long customerId = (long) (requestIndex % 10 + 1);
                    customerRepository.findById(customerId);
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    metricsCollector.recordOperation("CacheHit", responseTime, true);
                    
                } catch (Exception e) {
                    metricsCollector.recordOperation("CacheHit", 0, false);
                } finally {
                    warmCacheLatch.countDown();
                }
            });
        }
        
        assertTrue(warmCacheLatch.await(15, TimeUnit.SECONDS), "Warm cache test should complete");
        
        metricsCollector.stopCollection();
        
        // Generate performance report
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("CACHE PERFORMANCE REPORT:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics missMetrics = 
            report.operationMetrics().get("CacheMiss");
        PerformanceMetricsCollector.OperationMetrics hitMetrics = 
            report.operationMetrics().get("CacheHit");
        
        assertNotNull(missMetrics);
        assertNotNull(hitMetrics);
        
        // Cache hits should be significantly faster than misses
        assertTrue(hitMetrics.avgResponseTime() < missMetrics.avgResponseTime(), 
                "Cache hits should be faster than cache misses");
        
        // Cache hits should be very fast
        assertTrue(hitMetrics.avgResponseTime() < 50, 
                "Cache hits should average under 50ms");
    }
    
    @Test
    @DisplayName("Cache Eviction Performance")
    void cacheEvictionPerformance() throws InterruptedException {
        int testDataSize = 1000;
        
        // Generate test data
        List<LoadTestDataGenerator.TestCustomerData> testCustomers = 
            dataGenerator.generateCustomers(testDataSize);
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(testDataSize);
        
        // Fill cache beyond capacity to trigger evictions
        for (int i = 0; i < testDataSize; i++) {
            final int index = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    LoadTestDataGenerator.TestCustomerData customerData = testCustomers.get(index);
                    
                    // Store in cache
                    cacheService.put("customer:" + index, customerData, 300); // 5 minutes TTL
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    metricsCollector.recordOperation("CacheStore", responseTime, true);
                    
                } catch (Exception e) {
                    metricsCollector.recordOperation("CacheStore", 0, false);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(60, TimeUnit.SECONDS), "Cache eviction test should complete");
        
        metricsCollector.stopCollection();
        
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("CACHE EVICTION PERFORMANCE REPORT:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics storeMetrics = 
            report.operationMetrics().get("CacheStore");
        
        assertNotNull(storeMetrics);
        assertTrue(storeMetrics.avgResponseTime() < 100, 
                "Cache store operations should be fast even under eviction pressure");
    }
    
    @Test
    @DisplayName("Cache Concurrent Access Performance")
    void cacheConcurrentAccessPerformance() throws InterruptedException {
        // Setup test data
        setupTestDataForCacheTest();
        
        int concurrentUsers = 200;
        int operationsPerUser = 10;
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        
        for (int i = 0; i < concurrentUsers; i++) {
            final int userIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerUser; j++) {
                        // Mix of cache operations
                        switch (j % 3) {
                            case 0 -> performCacheGet(userIndex, j);
                            case 1 -> performCacheSet(userIndex, j);
                            case 2 -> performCacheDelete(userIndex, j);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(120, TimeUnit.SECONDS), "Concurrent cache access test should complete");
        
        metricsCollector.stopCollection();
        
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("CONCURRENT CACHE ACCESS REPORT:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        assertTrue(report.overallMetrics().overallSuccessRate() > 95.0, 
                "Cache operations should have high success rate under concurrent access");
        assertTrue(report.overallMetrics().throughput() > 100, 
                "Cache throughput should be high");
    }
    
    @Test
    @DisplayName("Cache Memory Usage Performance")
    void cacheMemoryUsagePerformance() throws InterruptedException {
        int largeDataSetSize = 10000;
        
        metricsCollector.startCollection();
        
        // Test memory usage with large dataset
        CountDownLatch latch = new CountDownLatch(largeDataSetSize);
        
        for (int i = 0; i < largeDataSetSize; i++) {
            final int index = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    // Store large objects in cache
                    String largeValue = generateLargeString(1000); // 1KB per object
                    cacheService.put("large_object:" + index, largeValue, 600);
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    metricsCollector.recordOperation("LargeCacheStore", responseTime, true);
                    
                } catch (Exception e) {
                    metricsCollector.recordOperation("LargeCacheStore", 0, false);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(180, TimeUnit.SECONDS), "Memory usage test should complete");
        
        metricsCollector.stopCollection();
        
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("CACHE MEMORY USAGE REPORT:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics metrics = 
            report.operationMetrics().get("LargeCacheStore");
        
        assertNotNull(metrics);
        assertTrue(metrics.avgResponseTime() < 200, 
                "Large cache store operations should remain performant");
    }
    
    // Helper methods
    
    private void setupTestDataForCacheTest() {
        // Create some test customers
        for (int i = 1; i <= 10; i++) {
            try {
                Customer customer = new Customer(
                        "TestCustomer" + i,
                        "LastName" + i,
                        "test" + i + "@example.com",
                        "+1-555-000" + i,
                        java.time.LocalDate.now().minusYears(25),
                        com.loanmanagement.shared.domain.model.Money.of("USD", 
                                java.math.BigDecimal.valueOf(5000))
                );
                customerRepository.save(customer);
            } catch (Exception e) {
                // Ignore setup errors
            }
        }
    }
    
    private void performCacheGet(int userIndex, int operationIndex) {
        try {
            long startTime = System.currentTimeMillis();
            
            String key = "test_key_" + (userIndex * 10 + operationIndex);
            Object value = cacheService.get(key);
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("CacheGet", responseTime, true);
            
        } catch (Exception e) {
            metricsCollector.recordOperation("CacheGet", 0, false);
        }
    }
    
    private void performCacheSet(int userIndex, int operationIndex) {
        try {
            long startTime = System.currentTimeMillis();
            
            String key = "test_key_" + (userIndex * 10 + operationIndex);
            String value = "test_value_" + userIndex + "_" + operationIndex;
            
            cacheService.put(key, value, 300); // 5 minutes TTL
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("CacheSet", responseTime, true);
            
        } catch (Exception e) {
            metricsCollector.recordOperation("CacheSet", 0, false);
        }
    }
    
    private void performCacheDelete(int userIndex, int operationIndex) {
        try {
            long startTime = System.currentTimeMillis();
            
            String key = "test_key_" + (userIndex * 10 + operationIndex);
            cacheService.delete(key);
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("CacheDelete", responseTime, true);
            
        } catch (Exception e) {
            metricsCollector.recordOperation("CacheDelete", 0, false);
        }
    }
    
    private void clearAllCaches() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                cacheManager.getCache(cacheName).clear();
            });
        } catch (Exception e) {
            // Ignore cache clear errors
        }
    }
    
    private String generateLargeString(int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append("A");
        }
        return sb.toString();
    }
}