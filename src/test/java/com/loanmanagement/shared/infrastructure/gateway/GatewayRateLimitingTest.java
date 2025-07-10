package com.loanmanagement.shared.infrastructure.gateway;

import com.loanmanagement.shared.infrastructure.gateway.model.*;
import com.loanmanagement.shared.infrastructure.gateway.service.GatewayRateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gateway Rate Limiting Test
 * Tests for advanced rate limiting functionality
 */
@SpringBootTest
class GatewayRateLimitingTest {

    private GatewayRateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new GatewayRateLimitingService();
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinRateLimit() {
        // Configure rate limit
        RateLimitConfig config = RateLimitConfig.builder()
            .requestsPerMinute(10)
            .windowSize(Duration.ofMinutes(1))
            .build();
        
        rateLimitingService.configureRateLimit("client1", config);
        
        // Create test request
        ApiRequest request = ApiRequest.builder()
            .clientId("client1")
            .path("/api/v1/customers")
            .method("GET")
            .build();
        
        // Should allow first 10 requests
        for (int i = 0; i < 10; i++) {
            RateLimitDecision decision = rateLimitingService.checkRateLimit(request);
            assertThat(decision.isAllowed()).isTrue();
        }
    }

    @Test
    @DisplayName("Should reject requests exceeding rate limit")
    void shouldRejectRequestsExceedingRateLimit() {
        // Configure rate limit
        RateLimitConfig config = RateLimitConfig.builder()
            .requestsPerMinute(5)
            .windowSize(Duration.ofMinutes(1))
            .build();
        
        rateLimitingService.configureRateLimit("client2", config);
        
        // Create test request
        ApiRequest request = ApiRequest.builder()
            .clientId("client2")
            .path("/api/v1/loans")
            .method("POST")
            .build();
        
        // Allow first 5 requests
        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = rateLimitingService.checkRateLimit(request);
            assertThat(decision.isAllowed()).isTrue();
        }
        
        // 6th request should be rejected
        RateLimitDecision decision = rateLimitingService.checkRateLimit(request);
        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).contains("rate limit exceeded");
    }

    @Test
    @DisplayName("Should enforce global rate limits")
    void shouldEnforceGlobalRateLimits() {
        // Configure global rate limit
        rateLimitingService.configureGlobalRateLimit("customer-service", 3);
        
        // Create requests from different clients
        ApiRequest request1 = ApiRequest.builder()
            .clientId("client1")
            .path("/api/v1/customers/123")
            .method("GET")
            .build();
        
        ApiRequest request2 = ApiRequest.builder()
            .clientId("client2")
            .path("/api/v1/customers/456")
            .method("GET")
            .build();
        
        // Allow first 3 requests globally
        assertThat(rateLimitingService.checkRateLimit(request1).isAllowed()).isTrue();
        assertThat(rateLimitingService.checkRateLimit(request2).isAllowed()).isTrue();
        assertThat(rateLimitingService.checkRateLimit(request1).isAllowed()).isTrue();
        
        // 4th request should be rejected due to global limit
        RateLimitDecision decision = rateLimitingService.checkRateLimit(request2);
        assertThat(decision.isAllowed()).isFalse();
        assertThat(decision.getReason()).contains("Global rate limit exceeded");
    }

    @Test
    @DisplayName("Should track rate limit status correctly")
    void shouldTrackRateLimitStatusCorrectly() {
        // Configure rate limit
        RateLimitConfig config = RateLimitConfig.builder()
            .requestsPerMinute(10)
            .windowSize(Duration.ofMinutes(1))
            .build();
        
        rateLimitingService.configureRateLimit("client3", config);
        
        // Make some requests
        ApiRequest request = ApiRequest.builder()
            .clientId("client3")
            .path("/api/v1/payments")
            .method("POST")
            .build();
        
        for (int i = 0; i < 7; i++) {
            rateLimitingService.checkRateLimit(request);
        }
        
        // Check status
        RateLimitStatus status = rateLimitingService.getRateLimitStatus("client3");
        assertThat(status.getCurrentRequestCount()).isEqualTo(7);
        assertThat(status.getRequestLimit()).isEqualTo(10);
        assertThat(status.isLimited()).isFalse();
    }

    @Test
    @DisplayName("Should record rate limit violations")
    void shouldRecordRateLimitViolations() {
        // Configure rate limit
        RateLimitConfig config = RateLimitConfig.builder()
            .requestsPerMinute(2)
            .windowSize(Duration.ofMinutes(1))
            .build();
        
        rateLimitingService.configureRateLimit("client4", config);
        
        // Create test request
        ApiRequest request = ApiRequest.builder()
            .clientId("client4")
            .path("/api/v1/loans/apply")
            .method("POST")
            .build();
        
        // Exceed rate limit
        rateLimitingService.checkRateLimit(request);
        rateLimitingService.checkRateLimit(request);
        rateLimitingService.checkRateLimit(request); // This should trigger violation
        
        // Check violation recorded
        RateLimitViolation violation = rateLimitingService.getViolation("client4", "/api/v1/loans/apply");
        assertThat(violation).isNotNull();
        assertThat(violation.getClientId()).isEqualTo("client4");
        assertThat(violation.getPath()).isEqualTo("/api/v1/loans/apply");
        assertThat(violation.getReason()).contains("Rate limit exceeded");
    }

    @Test
    @DisplayName("Should support sliding window rate limiting")
    void shouldSupportSlidingWindowRateLimiting() {
        // Configure sliding window rate limiter
        RateLimitConfig config = RateLimitConfig.builder()
            .requestsPerMinute(5)
            .windowSize(Duration.ofSeconds(10)) // 10-second window for testing
            .build();
        
        rateLimitingService.configureRateLimit("client5", config);
        
        ApiRequest request = ApiRequest.builder()
            .clientId("client5")
            .path("/api/v1/test")
            .method("GET")
            .build();
        
        // Fill the window
        for (int i = 0; i < 5; i++) {
            RateLimitDecision decision = rateLimitingService.checkRateLimit(request);
            assertThat(decision.isAllowed()).isTrue();
        }
        
        // Next request should be rejected
        RateLimitDecision decision = rateLimitingService.checkRateLimit(request);
        assertThat(decision.isAllowed()).isFalse();
        
        // Verify retry after is set
        assertThat(decision.getRetryAfter()).isNotNull();
        assertThat(decision.getRetryAfter().toSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should reset client rate limits")
    void shouldResetClientRateLimits() {
        // Configure rate limit
        RateLimitConfig config = RateLimitConfig.builder()
            .requestsPerMinute(3)
            .windowSize(Duration.ofMinutes(1))
            .build();
        
        rateLimitingService.configureRateLimit("client6", config);
        
        ApiRequest request = ApiRequest.builder()
            .clientId("client6")
            .path("/api/v1/test")
            .method("GET")
            .build();
        
        // Exhaust rate limit
        for (int i = 0; i < 3; i++) {
            rateLimitingService.checkRateLimit(request);
        }
        
        // Should be rate limited
        assertThat(rateLimitingService.checkRateLimit(request).isAllowed()).isFalse();
        
        // Reset limits
        rateLimitingService.resetClientLimits("client6");
        
        // Should allow requests again
        assertThat(rateLimitingService.checkRateLimit(request).isAllowed()).isTrue();
    }

    @Test
    @DisplayName("Should handle different service paths correctly")
    void shouldHandleDifferentServicePathsCorrectly() {
        // Configure global limits for different services
        rateLimitingService.configureGlobalRateLimit("customer-service", 2);
        rateLimitingService.configureGlobalRateLimit("loan-service", 2);
        
        ApiRequest customerRequest = ApiRequest.builder()
            .clientId("client1")
            .path("/api/v1/customers/123")
            .method("GET")
            .build();
        
        ApiRequest loanRequest = ApiRequest.builder()
            .clientId("client1")
            .path("/api/v1/loans/456")
            .method("GET")
            .build();
        
        // Each service should have independent limits
        assertThat(rateLimitingService.checkRateLimit(customerRequest).isAllowed()).isTrue();
        assertThat(rateLimitingService.checkRateLimit(customerRequest).isAllowed()).isTrue();
        
        assertThat(rateLimitingService.checkRateLimit(loanRequest).isAllowed()).isTrue();
        assertThat(rateLimitingService.checkRateLimit(loanRequest).isAllowed()).isTrue();
        
        // Third request to each service should be rejected
        assertThat(rateLimitingService.checkRateLimit(customerRequest).isAllowed()).isFalse();
        assertThat(rateLimitingService.checkRateLimit(loanRequest).isAllowed()).isFalse();
    }

    @Test
    @DisplayName("Should handle requests without client ID")
    void shouldHandleRequestsWithoutClientId() {
        ApiRequest request = ApiRequest.builder()
            .path("/api/v1/public/health")
            .method("GET")
            .build(); // No client ID
        
        RateLimitDecision decision = rateLimitingService.checkRateLimit(request);
        
        // Should allow requests without client ID (public endpoints)
        assertThat(decision.isAllowed()).isTrue();
    }
}