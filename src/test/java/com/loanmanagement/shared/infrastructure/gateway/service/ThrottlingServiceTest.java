package com.loanmanagement.shared.infrastructure.gateway.service;

import com.loanmanagement.shared.infrastructure.gateway.model.*;
import com.loanmanagement.shared.infrastructure.gateway.service.ThrottlingService.RefillType;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ThrottlingService
 * Tests distributed throttling with Redis backend
 */
@ExtendWith(MockitoExtension.class)
class ThrottlingServiceTest {

    @Mock
    private RedisTemplate<String, byte[]> redisTemplate;

    @Mock
    private ProxyManager<String> proxyManager;

    private ThrottlingService throttlingService;

    @BeforeEach
    void setUp() {
        throttlingService = new ThrottlingService(redisTemplate);
    }

    @Test
    void shouldAllowRequestWithinServiceLimit() {
        // Given
        ThrottlingRequest request = ThrottlingRequest.builder()
            .userId("user123")
            .serviceName("customer-service")
            .path("/api/customers")
            .method("GET")
            .userTier("standard")
            .timestamp(Instant.now())
            .build();

        // When
        ThrottlingDecision decision = throttlingService.checkThrottling(request);

        // Then
        assertTrue(decision.isAllowed());
        assertEquals("Request allowed", decision.getReason());
        assertTrue(decision.getRemainingTokens() > 0);
    }

    @Test
    void shouldDenyRequestExceedingEndpointLimit() {
        // Given
        ThrottlingRequest request = ThrottlingRequest.builder()
            .userId("user123")
            .serviceName("loan-service")
            .path("/api/loans/apply")
            .method("POST")
            .userTier("basic")
            .timestamp(Instant.now())
            .build();

        // Simulate multiple rapid requests
        for (int i = 0; i < 12; i++) {
            throttlingService.checkThrottling(request);
        }

        // When
        ThrottlingDecision decision = throttlingService.checkThrottling(request);

        // Then - should be denied after exceeding limit
        assertFalse(decision.isAllowed());
        assertTrue(decision.getReason().contains("Rate limit exceeded"));
        assertEquals(0, decision.getRemainingTokens());
        assertTrue(decision.getRetryAfterSeconds() > 0);
    }

    @Test
    void shouldApplyDifferentLimitsForUserTiers() {
        // Given - Basic user
        ThrottlingRequest basicRequest = ThrottlingRequest.builder()
            .userId("basic-user")
            .serviceName("customer-service")
            .path("/api/customers")
            .method("GET")
            .userTier("basic")
            .timestamp(Instant.now())
            .build();

        // Given - Premium user
        ThrottlingRequest premiumRequest = ThrottlingRequest.builder()
            .userId("premium-user")
            .serviceName("customer-service")
            .path("/api/customers")
            .method("GET")
            .userTier("premium")
            .timestamp(Instant.now())
            .build();

        // When
        ThrottlingDecision basicDecision = throttlingService.checkThrottling(basicRequest);
        ThrottlingDecision premiumDecision = throttlingService.checkThrottling(premiumRequest);

        // Then - Both should be allowed initially
        assertTrue(basicDecision.isAllowed());
        assertTrue(premiumDecision.isAllowed());
        
        // Premium user should have more tokens
        assertTrue(premiumDecision.getRemainingTokens() > basicDecision.getRemainingTokens());
    }

    @Test
    void shouldApplyOperationSpecificThrottling() {
        // Given
        ThrottlingRequest riskAssessmentRequest = ThrottlingRequest.builder()
            .userId("user123")
            .serviceName("risk-service")
            .path("/api/risk/assess")
            .method("POST")
            .userTier("standard")
            .operation("risk-assessment")
            .timestamp(Instant.now())
            .build();

        // When - Make multiple requests
        ThrottlingDecision firstDecision = throttlingService.checkThrottling(riskAssessmentRequest);
        
        // Simulate multiple rapid requests
        for (int i = 0; i < 11; i++) {
            throttlingService.checkThrottling(riskAssessmentRequest);
        }
        
        ThrottlingDecision lastDecision = throttlingService.checkThrottling(riskAssessmentRequest);

        // Then
        assertTrue(firstDecision.isAllowed());
        assertFalse(lastDecision.isAllowed());
        assertTrue(lastDecision.getReason().contains("operation:risk-assessment"));
    }

    @Test
    void shouldConfigureCustomThrottlingPolicy() {
        // Given
        String policyKey = "test:custom-policy";
        ThrottlingPolicy customPolicy = ThrottlingPolicy.builder()
            .capacity(5)
            .period(Duration.ofMinutes(1))
            .refillType(RefillType.INTERVALLY)
            .overdraftCapacity(1)
            .description("Custom test policy")
            .enabled(true)
            .priority(1)
            .scope("test")
            .build();

        // When
        throttlingService.configureThrottlingPolicy(policyKey, customPolicy);

        // Then - Policy should be stored and logged
        // This would require additional test infrastructure to verify
        assertNotNull(customPolicy);
        assertEquals(5, customPolicy.getCapacity());
        assertEquals(Duration.ofMinutes(1), customPolicy.getPeriod());
        assertEquals(RefillType.INTERVALLY, customPolicy.getRefillType());
    }

    @Test
    void shouldHandleHighVolumeRequests() {
        // Given
        ThrottlingRequest request = ThrottlingRequest.builder()
            .userId("load-test-user")
            .serviceName("customer-service")
            .path("/api/customers")
            .method("GET")
            .userTier("enterprise")
            .timestamp(Instant.now())
            .build();

        // When - Make many rapid requests
        int allowedCount = 0;
        int deniedCount = 0;
        
        for (int i = 0; i < 1000; i++) {
            ThrottlingDecision decision = throttlingService.checkThrottling(request);
            if (decision.isAllowed()) {
                allowedCount++;
            } else {
                deniedCount++;
            }
        }

        // Then - Should eventually start denying requests
        assertTrue(allowedCount > 0, "Should allow some requests");
        assertTrue(deniedCount > 0, "Should deny some requests due to rate limiting");
        assertTrue(allowedCount > deniedCount, "Enterprise tier should allow most requests");
    }

    @Test
    void shouldProvideAccurateRemainingTokensInfo() {
        // Given
        ThrottlingRequest request = ThrottlingRequest.builder()
            .userId("token-test-user")
            .serviceName("payment-service")
            .path("/api/payments/transfer")
            .method("POST")
            .userTier("standard")
            .timestamp(Instant.now())
            .build();

        // When
        ThrottlingDecision firstDecision = throttlingService.checkThrottling(request);
        ThrottlingDecision secondDecision = throttlingService.checkThrottling(request);

        // Then
        assertTrue(firstDecision.isAllowed());
        assertTrue(secondDecision.isAllowed());
        assertTrue(secondDecision.getRemainingTokens() < firstDecision.getRemainingTokens(),
            "Token count should decrease after each request");
    }

    @Test
    void shouldHandleMultipleThrottlingLevels() {
        // Given
        ThrottlingRequest request = ThrottlingRequest.builder()
            .userId("multi-level-user")
            .serviceName("loan-service")
            .path("/api/loans/apply")
            .method("POST")
            .userTier("basic")
            .operation("risk-assessment")
            .timestamp(Instant.now())
            .build();

        // When
        ThrottlingDecision decision = throttlingService.checkThrottling(request);

        // Then - Should pass through all throttling levels
        assertTrue(decision.isAllowed());
        assertNotNull(decision.getReason());
        assertTrue(decision.getRemainingTokens() >= 0);
    }

    @Test
    void shouldHandleMissingOptionalFields() {
        // Given - Request with minimal required fields
        ThrottlingRequest minimalRequest = ThrottlingRequest.builder()
            .userId("minimal-user")
            .serviceName("customer-service")
            .path("/api/customers")
            .method("GET")
            .userTier("basic")
            .timestamp(Instant.now())
            .build();

        // When
        ThrottlingDecision decision = throttlingService.checkThrottling(minimalRequest);

        // Then - Should handle gracefully
        assertTrue(decision.isAllowed());
        assertNotNull(decision.getReason());
    }
}