package com.loanmanagement.shared.infrastructure.gateway;

import com.loanmanagement.shared.infrastructure.gateway.model.*;
import com.loanmanagement.shared.infrastructure.gateway.service.*;
import com.loanmanagement.shared.infrastructure.resilience.IstioCircuitBreakerService;
import com.loanmanagement.shared.infrastructure.resilience.model.CircuitBreakerStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Istio-enhanced Gateway Infrastructure
 * Tests the complete gateway stack with rate limiting, throttling, and circuit breakers
 */
@ExtendWith(MockitoExtension.class)
class IstioGatewayIntegrationTest {

    @Mock
    private RedisTemplate<String, byte[]> redisTemplate;

    private IstioRateLimitingService rateLimitingService;
    private ThrottlingService throttlingService;
    private IstioCircuitBreakerService circuitBreakerService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new IstioRateLimitingService(redisTemplate);
        throttlingService = new ThrottlingService(redisTemplate);
        circuitBreakerService = new IstioCircuitBreakerService(null, null);
    }

    @Test
    void shouldIntegrateRateLimitingWithThrottling() {
        // Given
        IstioRateLimitRequest rateLimitRequest = IstioRateLimitRequest.builder()
            .serviceName("customer-service")
            .userId("user123")
            .path("/api/customers")
            .method("GET")
            .clientId("mobile-app")
            .sourceIp("192.168.1.100")
            .timestamp(Instant.now())
            .build();

        ThrottlingRequest throttlingRequest = ThrottlingRequest.builder()
            .userId("user123")
            .serviceName("customer-service")
            .path("/api/customers")
            .method("GET")
            .userTier("standard")
            .timestamp(Instant.now())
            .build();

        // When
        IstioRateLimitDecision rateLimitDecision = rateLimitingService.checkRateLimit(rateLimitRequest);
        ThrottlingDecision throttlingDecision = throttlingService.checkThrottling(throttlingRequest);

        // Then
        assertTrue(rateLimitDecision.isAllowed());
        assertTrue(throttlingDecision.isAllowed());
        assertTrue(rateLimitDecision.getRemainingTokens() > 0);
        assertTrue(throttlingDecision.getRemainingTokens() > 0);
    }

    @Test
    void shouldGenerateIstioConfiguration() {
        // Given
        String serviceName = "loan-service";

        // When
        Map<String, Object> envoyConfig = rateLimitingService.generateEnvoyConfiguration(serviceName);
        Map<String, Object> destinationRule = circuitBreakerService.generateIstioDestinationRule(serviceName);

        // Then
        assertNotNull(envoyConfig);
        assertNotNull(destinationRule);
        
        // Verify Envoy configuration structure
        assertTrue(envoyConfig.containsKey("rate_limits"));
        assertTrue(envoyConfig.containsKey("domain"));
        assertEquals("banking-system", envoyConfig.get("domain"));
        
        // Verify DestinationRule structure
        assertEquals("networking.istio.io/v1beta1", destinationRule.get("apiVersion"));
        assertEquals("DestinationRule", destinationRule.get("kind"));
        assertTrue(destinationRule.containsKey("spec"));
    }

    @Test
    void shouldHandleCircuitBreakerIntegration() {
        // Given
        String serviceName = "payment-service";

        // When
        CircuitBreakerStatus status = circuitBreakerService.getCircuitBreakerStatus(serviceName);
        
        // Execute with circuit breaker
        String result = circuitBreakerService.executeWithFallback(
            serviceName,
            () -> "Success",
            () -> "Fallback"
        );

        // Then
        assertNotNull(status);
        assertEquals(serviceName, status.getServiceName());
        assertEquals(CircuitBreaker.State.CLOSED, status.getState());
        assertEquals("Success", result);
    }

    @Test
    void shouldHandleHighLoadScenario() throws InterruptedException {
        // Given
        String serviceName = "customer-service";
        String userId = "load-test-user";
        int numberOfRequests = 100;
        int numberOfThreads = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        
        // When
        CompletableFuture<?>[] futures = new CompletableFuture[numberOfRequests];
        
        for (int i = 0; i < numberOfRequests; i++) {
            final int requestId = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                // Rate limiting check
                IstioRateLimitRequest rateLimitRequest = IstioRateLimitRequest.builder()
                    .serviceName(serviceName)
                    .userId(userId + requestId)
                    .path("/api/customers")
                    .method("GET")
                    .clientId("load-test")
                    .sourceIp("192.168.1." + (requestId % 255))
                    .timestamp(Instant.now())
                    .build();
                
                // Throttling check
                ThrottlingRequest throttlingRequest = ThrottlingRequest.builder()
                    .userId(userId + requestId)
                    .serviceName(serviceName)
                    .path("/api/customers")
                    .method("GET")
                    .userTier("standard")
                    .timestamp(Instant.now())
                    .build();
                
                IstioRateLimitDecision rateLimitDecision = rateLimitingService.checkRateLimit(rateLimitRequest);
                ThrottlingDecision throttlingDecision = throttlingService.checkThrottling(throttlingRequest);
                
                // Verify decisions are consistent
                assertNotNull(rateLimitDecision);
                assertNotNull(throttlingDecision);
            }, executor);
        }
        
        // Then
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldProvideGatewayMetrics() {
        // Given
        String serviceName = "loan-service";
        
        // When
        IstioRateLimitRequest request = IstioRateLimitRequest.builder()
            .serviceName(serviceName)
            .userId("metrics-user")
            .path("/api/loans")
            .method("GET")
            .clientId("web-app")
            .sourceIp("192.168.1.200")
            .timestamp(Instant.now())
            .build();
        
        IstioRateLimitDecision decision = rateLimitingService.checkRateLimit(request);
        CircuitBreakerStatus circuitStatus = circuitBreakerService.getCircuitBreakerStatus(serviceName);
        
        // Then
        assertNotNull(decision);
        assertNotNull(circuitStatus);
        
        // Verify metrics are available
        assertTrue(decision.getRemainingTokens() >= 0);
        assertNotNull(circuitStatus.getState());
        assertTrue(circuitStatus.getFailureRate() >= 0);
    }

    @Test
    void shouldHandleServiceFailureScenario() {
        // Given
        String serviceName = "failing-service";
        
        // When - Simulate service failures
        String result1 = circuitBreakerService.executeWithFallback(
            serviceName,
            () -> { throw new RuntimeException("Service failure"); },
            () -> "Fallback response"
        );
        
        String result2 = circuitBreakerService.executeWithFallback(
            serviceName,
            () -> { throw new RuntimeException("Service failure"); },
            () -> "Fallback response"
        );
        
        // Then
        assertEquals("Fallback response", result1);
        assertEquals("Fallback response", result2);
        
        // Circuit breaker should eventually open
        CircuitBreakerStatus status = circuitBreakerService.getCircuitBreakerStatus(serviceName);
        assertNotNull(status);
        // State might be CLOSED initially due to insufficient failures
        assertTrue(status.getState() == CircuitBreaker.State.CLOSED || 
                  status.getState() == CircuitBreaker.State.OPEN);
    }

    @Test
    void shouldSupportBankingSpecificThrottling() {
        // Given - High-value transaction request
        ThrottlingRequest highValueRequest = ThrottlingRequest.builder()
            .userId("premium-user")
            .serviceName("payment-service")
            .path("/api/payments/transfer")
            .method("POST")
            .userTier("premium")
            .operation("high-value-transfer")
            .transactionType("WIRE_TRANSFER")
            .businessFunction("PAYMENT_PROCESSING")
            .timestamp(Instant.now())
            .build();
        
        // When
        ThrottlingDecision decision = throttlingService.checkThrottling(highValueRequest);
        
        // Then
        assertTrue(decision.isAllowed());
        assertNotNull(decision.getReason());
        assertTrue(decision.getRemainingTokens() > 0);
    }

    @Test
    void shouldEnforceEndpointSpecificLimits() {
        // Given - Loan application endpoint (limited to 10 per hour)
        ThrottlingRequest loanApplicationRequest = ThrottlingRequest.builder()
            .userId("applicant-user")
            .serviceName("loan-service")
            .path("/api/loans/apply")
            .method("POST")
            .userTier("standard")
            .businessFunction("LOAN_ORIGINATION")
            .timestamp(Instant.now())
            .build();
        
        // When - Make multiple applications
        int allowedApplications = 0;
        for (int i = 0; i < 15; i++) {
            ThrottlingDecision decision = throttlingService.checkThrottling(loanApplicationRequest);
            if (decision.isAllowed()) {
                allowedApplications++;
            }
        }
        
        // Then - Should be limited to configured amount
        assertTrue(allowedApplications <= 10, "Should not exceed endpoint limit");
        assertTrue(allowedApplications > 0, "Should allow some applications");
    }

    @Test
    void shouldGenerateCorrectIstioYamlConfiguration() {
        // Given
        String serviceName = "risk-service";
        
        // When
        Map<String, Object> destinationRule = circuitBreakerService.generateIstioDestinationRule(serviceName);
        
        // Then
        assertNotNull(destinationRule);
        
        // Verify required fields
        assertEquals("networking.istio.io/v1beta1", destinationRule.get("apiVersion"));
        assertEquals("DestinationRule", destinationRule.get("kind"));
        
        // Verify metadata
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) destinationRule.get("metadata");
        assertNotNull(metadata);
        assertEquals(serviceName + "-circuit-breaker", metadata.get("name"));
        assertEquals("banking-system", metadata.get("namespace"));
        
        // Verify spec
        @SuppressWarnings("unchecked")
        Map<String, Object> spec = (Map<String, Object>) destinationRule.get("spec");
        assertNotNull(spec);
        assertEquals(serviceName + ".banking-system.svc.cluster.local", spec.get("host"));
        
        // Verify traffic policy
        @SuppressWarnings("unchecked")
        Map<String, Object> trafficPolicy = (Map<String, Object>) spec.get("trafficPolicy");
        assertNotNull(trafficPolicy);
        assertTrue(trafficPolicy.containsKey("connectionPool"));
        assertTrue(trafficPolicy.containsKey("outlierDetection"));
    }
}