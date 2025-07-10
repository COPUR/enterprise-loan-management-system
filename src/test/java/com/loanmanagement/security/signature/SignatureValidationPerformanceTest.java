package com.loanmanagement.security.signature;

import com.loanmanagement.security.signature.client.HttpSignatureClient;
import com.loanmanagement.security.signature.client.SignatureRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for HTTP signature validation
 * Ensures signature validation doesn't negatively impact API performance
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Signature Validation Performance Tests")
class SignatureValidationPerformanceTest {

    private RequestSignatureValidator signatureValidator;
    private SignatureComponentsExtractor componentsExtractor;
    private DigestCalculator digestCalculator;
    private HttpSignatureClient signatureClient;
    private TestSignatureKeyResolver keyResolver;
    
    private KeyPair keyPair;
    private String keyId = "performance-test-key";

    @BeforeEach
    void setUp() throws Exception {
        // Generate test key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        // Setup components
        componentsExtractor = new SignatureComponentsExtractor();
        digestCalculator = new DigestCalculator();
        signatureClient = new HttpSignatureClient(componentsExtractor, digestCalculator);
        
        // Create test key resolver
        keyResolver = new TestSignatureKeyResolver();
        keyResolver.addKey(keyId, keyPair.getPublic());
        
        signatureValidator = new RequestSignatureValidator(keyResolver, componentsExtractor);
    }

    @Test
    @DisplayName("Should validate signatures within acceptable time limit")
    void shouldValidateSignaturesWithinAcceptableTimeLimit() throws Exception {
        // Given
        int iterations = 1000;
        long maxAcceptableTimePerValidation = 50; // 50ms per validation
        
        SignatureRequest request = signatureClient.createPostRequest(
            keyId, keyPair.getPrivate(), "api.example.com", "/api/v1/loans",
            "{\"amount\":10000,\"currency\":\"USD\"}"
        );
        
        Map<String, String> headers = signatureClient.generateSignatureHeaders(request);
        MockHttpServletRequest mockRequest = createMockRequest("POST", "/api/v1/loans", headers);

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < iterations; i++) {
            boolean isValid = signatureValidator.validateSignature(mockRequest);
            assertTrue(isValid, "Signature validation failed at iteration " + i);
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        long averageTime = totalTime / iterations;

        // Then
        System.out.printf("Signature validation performance:%n");
        System.out.printf("Total time for %d validations: %dms%n", iterations, totalTime);
        System.out.printf("Average time per validation: %dms%n", averageTime);
        
        assertTrue(averageTime <= maxAcceptableTimePerValidation, 
            String.format("Average validation time (%dms) exceeds acceptable limit (%dms)", 
                         averageTime, maxAcceptableTimePerValidation));
    }

    @Test
    @DisplayName("Should handle concurrent signature validations efficiently")
    void shouldHandleConcurrentSignatureValidationsEfficiently() throws Exception {
        // Given
        int threadCount = 20;
        int requestsPerThread = 50;
        int totalRequests = threadCount * requestsPerThread;
        
        SignatureRequest request = signatureClient.createGetRequest(
            keyId, keyPair.getPrivate(), "api.example.com", "/api/v1/loans"
        );
        
        Map<String, String> headers = signatureClient.generateSignatureHeaders(request);
        MockHttpServletRequest mockRequest = createMockRequest("GET", "/api/v1/loans", headers);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalValidationTime = new AtomicLong(0);

        // When
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        long validationStart = System.nanoTime();
                        boolean isValid = signatureValidator.validateSignature(mockRequest);
                        long validationEnd = System.nanoTime();
                        
                        totalValidationTime.addAndGet(validationEnd - validationStart);
                        
                        if (isValid) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        executor.shutdown();

        // Then
        assertTrue(completed, "Concurrent validation test did not complete within timeout");
        
        long totalTime = endTime - startTime;
        long averageValidationTime = totalValidationTime.get() / totalRequests / 1_000_000; // Convert to ms
        double throughput = (double) totalRequests / totalTime * 1000; // Requests per second

        System.out.printf("Concurrent validation performance:%n");
        System.out.printf("Total requests: %d%n", totalRequests);
        System.out.printf("Successful validations: %d%n", successCount.get());
        System.out.printf("Failed validations: %d%n", failureCount.get());
        System.out.printf("Total time: %dms%n", totalTime);
        System.out.printf("Average validation time: %dms%n", averageValidationTime);
        System.out.printf("Throughput: %.2f requests/second%n", throughput);

        assertEquals(totalRequests, successCount.get(), "All validations should succeed");
        assertEquals(0, failureCount.get(), "No validations should fail");
        assertTrue(throughput > 100, "Throughput should be at least 100 requests/second");
    }

    @Test
    @DisplayName("Should cache key resolution for improved performance")
    void shouldCacheKeyResolutionForImprovedPerformance() throws Exception {
        // Given
        int iterations = 100;
        CacheTrackingKeyResolver cachingKeyResolver = new CacheTrackingKeyResolver();
        cachingKeyResolver.addKey(keyId, keyPair.getPublic());
        
        RequestSignatureValidator cachingValidator = new RequestSignatureValidator(
            cachingKeyResolver, componentsExtractor
        );
        
        SignatureRequest request = signatureClient.createGetRequest(
            keyId, keyPair.getPrivate(), "api.example.com", "/api/v1/loans"
        );
        
        Map<String, String> headers = signatureClient.generateSignatureHeaders(request);
        MockHttpServletRequest mockRequest = createMockRequest("GET", "/api/v1/loans", headers);

        // When
        for (int i = 0; i < iterations; i++) {
            boolean isValid = cachingValidator.validateSignature(mockRequest);
            assertTrue(isValid, "Signature validation failed at iteration " + i);
        }

        // Then
        System.out.printf("Key resolution performance:%n");
        System.out.printf("Total validations: %d%n", iterations);
        System.out.printf("Key resolver calls: %d%n", cachingKeyResolver.getCallCount());
        System.out.printf("Cache hit ratio: %.2f%%%n", 
            ((double)(iterations - cachingKeyResolver.getCallCount()) / iterations) * 100);
        
        // After the first call, subsequent calls should be cached
        // Allow for some cache misses due to test setup
        assertTrue(cachingKeyResolver.getCallCount() < iterations / 2, 
            "Key resolver should be called significantly less than validation count due to caching");
    }

    @Test
    @DisplayName("Should have minimal memory footprint during validation")
    void shouldHaveMinimalMemoryFootprintDuringValidation() throws Exception {
        // Given
        Runtime runtime = Runtime.getRuntime();
        int iterations = 1000;
        
        SignatureRequest request = signatureClient.createPostRequest(
            keyId, keyPair.getPrivate(), "api.example.com", "/api/v1/loans",
            "{\"amount\":10000,\"currency\":\"USD\"}"
        );
        
        Map<String, String> headers = signatureClient.generateSignatureHeaders(request);
        MockHttpServletRequest mockRequest = createMockRequest("POST", "/api/v1/loans", headers);

        // Measure memory before validation
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        // When
        for (int i = 0; i < iterations; i++) {
            boolean isValid = signatureValidator.validateSignature(mockRequest);
            assertTrue(isValid);
        }

        // Measure memory after validation
        System.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryUsed = memoryAfter - memoryBefore;
        long memoryPerValidation = memoryUsed / iterations;

        // Then
        System.out.printf("Memory usage performance:%n");
        System.out.printf("Memory before: %d bytes%n", memoryBefore);
        System.out.printf("Memory after: %d bytes%n", memoryAfter);
        System.out.printf("Memory used: %d bytes%n", memoryUsed);
        System.out.printf("Memory per validation: %d bytes%n", memoryPerValidation);

        // Memory usage should be reasonable (less than 1KB per validation)
        assertTrue(memoryPerValidation < 1024, 
            String.format("Memory usage per validation (%d bytes) is too high", memoryPerValidation));
    }

    // Helper classes and methods

    private MockHttpServletRequest createMockRequest(String method, String path, Map<String, String> headers) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(path);
        
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.addHeader(header.getKey(), header.getValue());
        }
        
        return request;
    }

    /**
     * Test implementation of SignatureKeyResolver for performance testing
     */
    private static class TestSignatureKeyResolver implements SignatureKeyResolver {
        private final Map<String, PublicKey> keys = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public Optional<PublicKey> resolvePublicKey(String keyId) {
            return Optional.ofNullable(keys.get(keyId));
        }

        public void addKey(String keyId, PublicKey publicKey) {
            keys.put(keyId, publicKey);
        }
    }

    /**
     * Key resolver that tracks call count for cache testing
     */
    private static class CacheTrackingKeyResolver implements SignatureKeyResolver {
        private final Map<String, PublicKey> keys = new java.util.concurrent.ConcurrentHashMap<>();
        private final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public Optional<PublicKey> resolvePublicKey(String keyId) {
            callCount.incrementAndGet();
            return Optional.ofNullable(keys.get(keyId));
        }

        public void addKey(String keyId, PublicKey publicKey) {
            keys.put(keyId, publicKey);
        }

        public int getCallCount() {
            return callCount.get();
        }
    }
}