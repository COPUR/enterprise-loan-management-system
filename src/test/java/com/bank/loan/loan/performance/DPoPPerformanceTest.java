package com.bank.loan.loan.performance;

import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.bank.loan.loan.security.dpop.service.DPoPTokenValidationService;
import com.bank.loan.loan.security.dpop.util.DPoPTestKeyGenerator;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DPoP Performance Tests")
class DPoPPerformanceTest {

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    private DPoPProofValidationService dpopProofValidationService;
    private DPoPTokenValidationService dpopTokenValidationService;
    private DPoPTestKeyGenerator keyGenerator;
    private ECKey testKeyPair;
    private String mockAccessToken;

    @BeforeEach
    void setUp() throws Exception {
        dpopProofValidationService = new DPoPProofValidationService(redisTemplate);
        dpopTokenValidationService = new DPoPTokenValidationService(jwtDecoder, dpopProofValidationService);
        keyGenerator = new DPoPTestKeyGenerator();
        testKeyPair = keyGenerator.generateECKey();
        mockAccessToken = "mock.access.token";

        // Mock JWT decoder
        String jktThumbprint = keyGenerator.calculateJktThumbprint(testKeyPair);
        Jwt mockJwt = Jwt.withTokenValue(mockAccessToken)
                .header("alg", "PS256")
                .header("typ", "JWT")
                .claim("iss", "https://auth.example.com")
                .claim("sub", "user123")
                .claim("aud", "https://api.example.com")
                .claim("exp", Instant.now().plusSeconds(300))
                .claim("iat", Instant.now())
                .claim("jti", "token123")
                .claim("cnf", Map.of("jkt", jktThumbprint))
                .claim("scope", "loans payments")
                .build();

        when(jwtDecoder.decode(anyString())).thenReturn(mockJwt);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class));
    }

    @Nested
    @DisplayName("DPoP Proof Validation Performance")
    class DPoPProofValidationPerformanceTests {

        @Test
        @DisplayName("Should validate DPoP proofs within performance threshold")
        void shouldValidateDPoPProofsWithinPerformanceThreshold() throws Exception {
            int numberOfRequests = 1000;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < numberOfRequests; i++) {
                String dpopProof = keyGenerator.createValidDPoPProof(
                        testKeyPair, "GET", "https://api.example.com/loans/" + i);

                dpopProofValidationService.validateDPoPProof(
                        dpopProof, "GET", "https://api.example.com/loans/" + i, null);
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double averageTime = (double) totalTime / numberOfRequests;

            assertThat(averageTime).isLessThan(10.0); // Should average less than 10ms per validation
            assertThat(totalTime).isLessThan(5000); // Total time should be less than 5 seconds
        }

        @Test
        @DisplayName("Should handle concurrent DPoP proof validation efficiently")
        void shouldHandleConcurrentDPoPProofValidationEfficiently() throws Exception {
            int numberOfThreads = 10;
            int requestsPerThread = 100;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

            long startTime = System.currentTimeMillis();

            CompletableFuture<Void>[] futures = IntStream.range(0, numberOfThreads)
                    .mapToObj(threadId -> CompletableFuture.runAsync(() -> {
                        try {
                            for (int i = 0; i < requestsPerThread; i++) {
                                String dpopProof = keyGenerator.createValidDPoPProof(
                                        testKeyPair, "GET", 
                                        "https://api.example.com/loans/" + threadId + "/" + i);

                                dpopProofValidationService.validateDPoPProof(
                                        dpopProof, "GET", 
                                        "https://api.example.com/loans/" + threadId + "/" + i, null);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }, executorService))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).join();

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double averageTime = (double) totalTime / (numberOfThreads * requestsPerThread);

            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);

            assertThat(averageTime).isLessThan(15.0); // Concurrent should be within 15ms average
            assertThat(totalTime).isLessThan(10000); // Total time should be less than 10 seconds
        }

        @Test
        @DisplayName("Should maintain performance with different key types")
        void shouldMaintainPerformanceWithDifferentKeyTypes() throws Exception {
            ECKey ecKey = keyGenerator.generateECKey();
            var rsaKey = keyGenerator.generateRSAKey();

            int numberOfRequests = 500;

            // Test EC key performance
            long ecStartTime = System.currentTimeMillis();
            for (int i = 0; i < numberOfRequests; i++) {
                String dpopProof = keyGenerator.createValidDPoPProof(
                        ecKey, "GET", "https://api.example.com/loans/ec/" + i);
                dpopProofValidationService.validateDPoPProof(
                        dpopProof, "GET", "https://api.example.com/loans/ec/" + i, null);
            }
            long ecEndTime = System.currentTimeMillis();
            double ecAverageTime = (double) (ecEndTime - ecStartTime) / numberOfRequests;

            // Test RSA key performance
            long rsaStartTime = System.currentTimeMillis();
            for (int i = 0; i < numberOfRequests; i++) {
                String dpopProof = keyGenerator.createRSADPoPProof(
                        rsaKey, "GET", "https://api.example.com/loans/rsa/" + i);
                dpopProofValidationService.validateDPoPProof(
                        dpopProof, "GET", "https://api.example.com/loans/rsa/" + i, null);
            }
            long rsaEndTime = System.currentTimeMillis();
            double rsaAverageTime = (double) (rsaEndTime - rsaStartTime) / numberOfRequests;

            assertThat(ecAverageTime).isLessThan(10.0); // EC should be fast
            assertThat(rsaAverageTime).isLessThan(20.0); // RSA can be slightly slower but should be reasonable
        }
    }

    @Nested
    @DisplayName("DPoP Token Validation Performance")
    class DPoPTokenValidationPerformanceTests {

        @Test
        @DisplayName("Should validate DPoP-bound tokens within performance threshold")
        void shouldValidateDPoPBoundTokensWithinPerformanceThreshold() throws Exception {
            int numberOfRequests = 1000;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < numberOfRequests; i++) {
                String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                        testKeyPair, "GET", "https://api.example.com/loans/" + i, mockAccessToken);

                dpopTokenValidationService.validateDPoPBoundToken(
                        mockAccessToken, dpopProof, "GET", "https://api.example.com/loans/" + i);
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double averageTime = (double) totalTime / numberOfRequests;

            assertThat(averageTime).isLessThan(15.0); // Should average less than 15ms per validation
            assertThat(totalTime).isLessThan(10000); // Total time should be less than 10 seconds
        }

        @Test
        @DisplayName("Should handle high-volume token extraction efficiently")
        void shouldHandleHighVolumeTokenExtractionEfficiently() throws Exception {
            int numberOfRequests = 2000;
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < numberOfRequests; i++) {
                String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                        testKeyPair, "GET", "https://api.example.com/loans/" + i, mockAccessToken);

                var boundToken = dpopTokenValidationService.validateAndExtractDPoPBoundToken(
                        mockAccessToken, dpopProof, "GET", "https://api.example.com/loans/" + i);

                assertThat(boundToken).isNotNull();
                assertThat(boundToken.getAccessToken()).isEqualTo(mockAccessToken);
            }

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            double averageTime = (double) totalTime / numberOfRequests;

            assertThat(averageTime).isLessThan(20.0); // Should average less than 20ms per extraction
            assertThat(totalTime).isLessThan(15000); // Total time should be less than 15 seconds
        }
    }

    @Nested
    @DisplayName("Memory Performance Tests")
    class MemoryPerformanceTests {

        @Test
        @DisplayName("Should maintain stable memory usage during validation")
        void shouldMaintainStableMemoryUsageDuringValidation() throws Exception {
            Runtime runtime = Runtime.getRuntime();
            
            // Force garbage collection
            System.gc();
            Thread.sleep(100);
            
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Perform many validations
            for (int i = 0; i < 5000; i++) {
                String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                        testKeyPair, "GET", "https://api.example.com/loans/" + i, mockAccessToken);

                dpopTokenValidationService.validateDPoPBoundToken(
                        mockAccessToken, dpopProof, "GET", "https://api.example.com/loans/" + i);

                // Periodic cleanup check
                if (i % 1000 == 0) {
                    System.gc();
                    Thread.sleep(10);
                }
            }

            // Force garbage collection
            System.gc();
            Thread.sleep(100);
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            
            // Memory increase should be reasonable (less than 50MB)
            assertThat(memoryIncrease).isLessThan(50 * 1024 * 1024);
        }

        @Test
        @DisplayName("Should handle memory efficiently with large number of unique keys")
        void shouldHandleMemoryEfficientlyWithLargeNumberOfUniqueKeys() throws Exception {
            Runtime runtime = Runtime.getRuntime();
            
            System.gc();
            Thread.sleep(100);
            
            long initialMemory = runtime.totalMemory() - runtime.freeMemory();
            
            // Create and use many different keys
            for (int i = 0; i < 1000; i++) {
                ECKey uniqueKey = keyGenerator.generateECKey();
                String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                        uniqueKey, "GET", "https://api.example.com/loans/" + i, mockAccessToken);

                // Mock JWT with unique key
                String jktThumbprint = keyGenerator.calculateJktThumbprint(uniqueKey);
                Jwt mockJwt = Jwt.withTokenValue(mockAccessToken)
                        .header("alg", "PS256")
                        .header("typ", "JWT")
                        .claim("iss", "https://auth.example.com")
                        .claim("sub", "user" + i)
                        .claim("cnf", Map.of("jkt", jktThumbprint))
                        .build();

                when(jwtDecoder.decode(anyString())).thenReturn(mockJwt);

                dpopTokenValidationService.validateDPoPBoundToken(
                        mockAccessToken, dpopProof, "GET", "https://api.example.com/loans/" + i);

                if (i % 100 == 0) {
                    System.gc();
                    Thread.sleep(10);
                }
            }

            System.gc();
            Thread.sleep(100);
            
            long finalMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalMemory - initialMemory;
            
            // Memory increase should be reasonable even with many keys (less than 100MB)
            assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024);
        }
    }

    @Nested
    @DisplayName("Throughput Performance Tests")
    class ThroughputPerformanceTests {

        @Test
        @DisplayName("Should achieve minimum throughput requirements")
        void shouldAchieveMinimumThroughputRequirements() throws Exception {
            int testDurationSeconds = 10;
            long endTime = System.currentTimeMillis() + (testDurationSeconds * 1000);
            int requestCount = 0;

            while (System.currentTimeMillis() < endTime) {
                String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                        testKeyPair, "GET", "https://api.example.com/loans/" + requestCount, mockAccessToken);

                dpopTokenValidationService.validateDPoPBoundToken(
                        mockAccessToken, dpopProof, "GET", "https://api.example.com/loans/" + requestCount);

                requestCount++;
            }

            double throughput = (double) requestCount / testDurationSeconds;
            
            // Should achieve at least 100 requests per second
            assertThat(throughput).isGreaterThan(100.0);
        }

        @Test
        @DisplayName("Should scale throughput with concurrent processing")
        void shouldScaleThroughputWithConcurrentProcessing() throws Exception {
            int numberOfThreads = 4;
            int testDurationSeconds = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            
            long endTime = System.currentTimeMillis() + (testDurationSeconds * 1000);
            final int[] totalRequests = {0};

            CompletableFuture<Void>[] futures = IntStream.range(0, numberOfThreads)
                    .mapToObj(threadId -> CompletableFuture.runAsync(() -> {
                        int threadRequests = 0;
                        try {
                            while (System.currentTimeMillis() < endTime) {
                                String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                                        testKeyPair, "GET", 
                                        "https://api.example.com/loans/" + threadId + "/" + threadRequests, 
                                        mockAccessToken);

                                dpopTokenValidationService.validateDPoPBoundToken(
                                        mockAccessToken, dpopProof, "GET", 
                                        "https://api.example.com/loans/" + threadId + "/" + threadRequests);

                                threadRequests++;
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        
                        synchronized (totalRequests) {
                            totalRequests[0] += threadRequests;
                        }
                    }, executorService))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).join();

            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);

            double concurrentThroughput = (double) totalRequests[0] / testDurationSeconds;
            
            // Should achieve at least 300 requests per second with 4 threads
            assertThat(concurrentThroughput).isGreaterThan(300.0);
        }
    }

    @Nested
    @DisplayName("Stress Tests")
    class StressTests {

        @Test
        @DisplayName("Should remain stable under sustained high load")
        void shouldRemainStableUnderSustainedHighLoad() throws Exception {
            int testDurationMinutes = 2;
            long endTime = System.currentTimeMillis() + (testDurationMinutes * 60 * 1000);
            int requestCount = 0;
            int errorCount = 0;

            while (System.currentTimeMillis() < endTime) {
                try {
                    String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                            testKeyPair, "GET", "https://api.example.com/loans/" + requestCount, mockAccessToken);

                    dpopTokenValidationService.validateDPoPBoundToken(
                            mockAccessToken, dpopProof, "GET", "https://api.example.com/loans/" + requestCount);

                    requestCount++;
                } catch (Exception e) {
                    errorCount++;
                }

                // Brief pause to avoid overwhelming the system
                if (requestCount % 1000 == 0) {
                    Thread.sleep(1);
                }
            }

            double errorRate = (double) errorCount / (requestCount + errorCount);
            
            assertThat(requestCount).isGreaterThan(10000); // Should process significant number of requests
            assertThat(errorRate).isLessThan(0.01); // Error rate should be less than 1%
        }
    }
}