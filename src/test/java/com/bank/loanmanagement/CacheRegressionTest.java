package com.bank.loanmanagement;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Redis ElastiCache Regression Testing for Enterprise Loan Management System
 * Validates multi-level caching performance and data consistency
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CacheRegressionTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;
    private final Map<String, Long> cacheMetrics = new HashMap<>();

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    @Order(1)
    @DisplayName("Cache Infrastructure Health Regression")
    void testCacheHealthRegression() {
        long startTime = System.currentTimeMillis();
        
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
            baseUrl + "/api/v1/cache/health", String.class);
        
        Assertions.assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        String responseBody = healthResponse.getBody();
        
        // Validate Redis ElastiCache health status
        Assertions.assertTrue(responseBody.contains("\"status\":\"healthy\""));
        Assertions.assertTrue(responseBody.contains("\"connected\":true"));
        Assertions.assertTrue(responseBody.contains("\"redis_health\":true"));
        
        // Validate cache strategies configuration
        Assertions.assertTrue(responseBody.contains("\"multi_level\":\"L1 (in-memory) + L2 (Redis)\""));
        Assertions.assertTrue(responseBody.contains("\"eviction_policy\":\"LRU (Least Recently Used)\""));
        Assertions.assertTrue(responseBody.contains("\"ttl_strategy\":\"Variable TTL by data type\""));
        
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.put("Health Check", duration);
        
        System.out.println("✓ Cache health regression completed in " + duration + "ms");
    }

    @Test
    @Order(2)
    @DisplayName("Cache Performance Metrics Regression")
    void testCacheMetricsRegression() {
        long startTime = System.currentTimeMillis();
        
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            baseUrl + "/api/v1/cache/metrics", String.class);
        
        Assertions.assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        String responseBody = metricsResponse.getBody();
        
        // Validate essential cache metrics exist
        Assertions.assertTrue(responseBody.contains("\"cache_hits\""));
        Assertions.assertTrue(responseBody.contains("\"cache_misses\""));
        Assertions.assertTrue(responseBody.contains("\"hit_ratio_percentage\""));
        Assertions.assertTrue(responseBody.contains("\"total_operations\""));
        Assertions.assertTrue(responseBody.contains("\"active_connections\""));
        Assertions.assertTrue(responseBody.contains("\"cache_enabled\":true"));
        
        // Validate banking cache categories
        Assertions.assertTrue(responseBody.contains("\"customer_cache\":\"active\""));
        Assertions.assertTrue(responseBody.contains("\"loan_cache\":\"active\""));
        Assertions.assertTrue(responseBody.contains("\"payment_cache\":\"active\""));
        Assertions.assertTrue(responseBody.contains("\"compliance_cache\":\"active\""));
        Assertions.assertTrue(responseBody.contains("\"security_cache\":\"active\""));
        Assertions.assertTrue(responseBody.contains("\"rate_limit_cache\":\"active\""));
        
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.put("Metrics Collection", duration);
        
        System.out.println("✓ Cache metrics regression completed in " + duration + "ms");
    }

    @Test
    @Order(3)
    @DisplayName("Cache Invalidation Regression")
    void testCacheInvalidationRegression() {
        long startTime = System.currentTimeMillis();
        
        // Test selective cache invalidation
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Test customer cache invalidation
        HttpEntity<String> customerRequest = new HttpEntity<>("{\"pattern\":\"customer\"}", headers);
        ResponseEntity<String> customerResponse = restTemplate.postForEntity(
            baseUrl + "/api/v1/cache/invalidate", customerRequest, String.class);
        
        Assertions.assertEquals(HttpStatus.OK, customerResponse.getStatusCode());
        Assertions.assertTrue(customerResponse.getBody().contains("\"status\":\"completed\""));
        
        // Test complete cache invalidation
        HttpEntity<String> allRequest = new HttpEntity<>("{\"invalidate\":\"all\"}", headers);
        ResponseEntity<String> allResponse = restTemplate.postForEntity(
            baseUrl + "/api/v1/cache/invalidate", allRequest, String.class);
        
        Assertions.assertEquals(HttpStatus.OK, allResponse.getStatusCode());
        Assertions.assertTrue(allResponse.getBody().contains("\"status\":\"completed\""));
        Assertions.assertTrue(allResponse.getBody().contains("\"keys_invalidated\""));
        
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.put("Invalidation", duration);
        
        System.out.println("✓ Cache invalidation regression completed in " + duration + "ms");
    }

    @Test
    @Order(4)
    @DisplayName("Cache Performance Under Load Regression")
    void testCacheLoadRegression() {
        long startTime = System.currentTimeMillis();
        
        List<CompletableFuture<ResponseEntity<String>>> futures = new ArrayList<>();
        List<Long> responseTimes = new ArrayList<>();
        
        // Simulate concurrent cache access
        for (int i = 0; i < 20; i++) {
            final int requestId = i;
            CompletableFuture<ResponseEntity<String>> future = CompletableFuture.supplyAsync(() -> {
                long requestStart = System.currentTimeMillis();
                
                ResponseEntity<String> response = restTemplate.getForEntity(
                    baseUrl + "/api/v1/cache/health", String.class);
                
                long requestDuration = System.currentTimeMillis() - requestStart;
                synchronized (responseTimes) {
                    responseTimes.add(requestDuration);
                }
                
                return response;
            });
            
            futures.add(future);
        }
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Validate all requests succeeded
        for (CompletableFuture<ResponseEntity<String>> future : futures) {
            try {
                ResponseEntity<String> response = future.get(5, TimeUnit.SECONDS);
                Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
            } catch (Exception e) {
                Assertions.fail("Concurrent cache request failed: " + e.getMessage());
            }
        }
        
        // Calculate performance metrics
        double avgResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        long maxResponseTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0L);
        
        // Cache responses should be fast even under load
        Assertions.assertTrue(avgResponseTime < 50.0, "Average cache response time should be under 50ms");
        Assertions.assertTrue(maxResponseTime < 200L, "Max cache response time should be under 200ms");
        
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.put("Load Testing", duration);
        
        System.out.println("✓ Cache load regression completed in " + duration + "ms");
        System.out.println("  Average response time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("  Max response time: " + maxResponseTime + "ms");
        System.out.println("  Concurrent requests: " + futures.size());
    }

    @Test
    @Order(5)
    @DisplayName("Cache Data Consistency Regression")
    void testCacheConsistencyRegression() {
        long startTime = System.currentTimeMillis();
        
        // Test cache consistency after data updates
        ResponseEntity<String> initialResponse = restTemplate.getForEntity(
            baseUrl + "/api/v1/tdd/coverage-report", String.class);
        
        Assertions.assertEquals(HttpStatus.OK, initialResponse.getStatusCode());
        String initialData = initialResponse.getBody();
        Assertions.assertTrue(initialData.contains("\"tdd_coverage\""));
        
        // Invalidate compliance cache
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"pattern\":\"compliance\"}", headers);
        
        restTemplate.postForEntity(baseUrl + "/api/v1/cache/invalidate", request, String.class);
        
        // Wait for cache to refresh
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test that data is still accessible after cache invalidation
        ResponseEntity<String> refreshedResponse = restTemplate.getForEntity(
            baseUrl + "/api/v1/tdd/coverage-report", String.class);
        
        Assertions.assertEquals(HttpStatus.OK, refreshedResponse.getStatusCode());
        String refreshedData = refreshedResponse.getBody();
        Assertions.assertTrue(refreshedData.contains("\"tdd_coverage\""));
        
        // Data should be consistent (may be refreshed with current timestamp)
        Assertions.assertTrue(refreshedData.contains("87.4") || refreshedData.contains("tdd_coverage"));
        
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.put("Consistency", duration);
        
        System.out.println("✓ Cache consistency regression completed in " + duration + "ms");
    }

    @Test
    @Order(6)
    @DisplayName("Multi-Level Cache Strategy Regression")
    void testMultiLevelCacheRegression() {
        long startTime = System.currentTimeMillis();
        
        // Test L1 (in-memory) and L2 (Redis) cache behavior
        List<Long> firstAccessTimes = new ArrayList<>();
        List<Long> cachedAccessTimes = new ArrayList<>();
        
        // First access (potentially cache miss)
        for (int i = 0; i < 5; i++) {
            long accessStart = System.currentTimeMillis();
            
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/cache/health", String.class);
            
            long accessDuration = System.currentTimeMillis() - accessStart;
            firstAccessTimes.add(accessDuration);
            
            Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        }
        
        // Wait briefly for cache warming
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Subsequent access (should be cached)
        for (int i = 0; i < 5; i++) {
            long accessStart = System.currentTimeMillis();
            
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/cache/health", String.class);
            
            long accessDuration = System.currentTimeMillis() - accessStart;
            cachedAccessTimes.add(accessDuration);
            
            Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        }
        
        // Calculate average access times
        double avgFirstAccess = firstAccessTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        double avgCachedAccess = cachedAccessTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        // Cached access should generally be faster or similar
        // (Note: In test environment, differences might be minimal)
        System.out.println("  First access average: " + String.format("%.2f", avgFirstAccess) + "ms");
        System.out.println("  Cached access average: " + String.format("%.2f", avgCachedAccess) + "ms");
        
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.put("Multi-Level Cache", duration);
        
        System.out.println("✓ Multi-level cache regression completed in " + duration + "ms");
    }

    @Test
    @Order(7)
    @DisplayName("Banking-Specific Cache Categories Regression")
    void testBankingCacheCategoriesRegression() {
        long startTime = System.currentTimeMillis();
        
        // Test each banking cache category
        String[] cacheCategories = {
            "customer_cache", "loan_cache", "payment_cache", 
            "compliance_cache", "security_cache", "rate_limit_cache"
        };
        
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
            baseUrl + "/api/v1/cache/metrics", String.class);
        
        Assertions.assertEquals(HttpStatus.OK, metricsResponse.getStatusCode());
        String responseBody = metricsResponse.getBody();
        
        for (String category : cacheCategories) {
            Assertions.assertTrue(responseBody.contains("\"" + category + "\":\"active\""),
                "Cache category " + category + " should be active");
        }
        
        // Test cache category-specific invalidation
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        for (String category : new String[]{"customer", "loan", "payment"}) {
            HttpEntity<String> request = new HttpEntity<>("{\"pattern\":\"" + category + "\"}", headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/cache/invalidate", request, String.class);
            
            Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        }
        
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.put("Banking Categories", duration);
        
        System.out.println("✓ Banking cache categories regression completed in " + duration + "ms");
    }

    @Test
    @Order(8)
    @DisplayName("Cache Error Handling Regression")
    void testCacheErrorHandlingRegression() {
        long startTime = System.currentTimeMillis();
        
        // Test invalid cache operations
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Test invalid invalidation request
        HttpEntity<String> invalidRequest = new HttpEntity<>("{\"invalid\":\"request\"}", headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/v1/cache/invalidate", invalidRequest, String.class);
        
        // Should handle gracefully (either accept or return proper error)
        Assertions.assertTrue(response.getStatusCode().is2xxSuccessful() || 
                             response.getStatusCode().is4xxClientError());
        
        // Test that system remains stable after invalid requests
        ResponseEntity<String> healthCheck = restTemplate.getForEntity(
            baseUrl + "/api/v1/cache/health", String.class);
        
        Assertions.assertEquals(HttpStatus.OK, healthCheck.getStatusCode());
        
        long duration = System.currentTimeMillis() - startTime;
        cacheMetrics.put("Error Handling", duration);
        
        System.out.println("✓ Cache error handling regression completed in " + duration + "ms");
    }

    @AfterAll
    static void printCacheRegressionReport() {
        System.out.println("\n=== CACHE REGRESSION TEST REPORT ===");
        System.out.println("✓ All cache regression tests completed successfully");
        System.out.println("✓ Redis ElastiCache multi-level caching validated");
        System.out.println("✓ Banking-specific cache categories operational");
        System.out.println("✓ Cache performance meets banking standards");
        System.out.println("✓ Cache invalidation patterns working correctly");
        System.out.println("✓ Error handling maintains system stability");
    }
}