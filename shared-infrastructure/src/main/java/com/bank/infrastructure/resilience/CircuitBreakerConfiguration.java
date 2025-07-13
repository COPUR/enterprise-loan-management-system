package com.bank.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Circuit Breaker Configuration for Banking Platform Resilience
 * 
 * Implements resilience patterns for external service integrations:
 * - Circuit breakers for fault tolerance
 * - Retry mechanisms with exponential backoff
 * - Bulkhead isolation for resource protection
 * - Time limiters for timeout handling
 * - Health indicators for monitoring
 */
@Configuration
public class CircuitBreakerConfiguration {
    
    /**
     * Circuit Breaker Registry with custom configurations
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        
        // Fraud Detection Service Circuit Breaker
        CircuitBreakerConfig fraudDetectionConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50.0f)                     // 50% failure rate threshold
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before half-open
            .slidingWindowSize(10)                           // 10 requests sliding window
            .minimumNumberOfCalls(5)                         // Minimum 5 calls before evaluation
            .permittedNumberOfCallsInHalfOpenState(3)        // 3 calls in half-open state
            .slowCallRateThreshold(80.0f)                    // 80% slow call threshold
            .slowCallDurationThreshold(Duration.ofSeconds(2)) // 2s slow call duration
            .recordExceptions(
                java.net.ConnectException.class,
                java.net.SocketTimeoutException.class,
                TimeoutException.class
            )
            .ignoreExceptions(
                IllegalArgumentException.class,
                IllegalStateException.class
            )
            .build();
        
        registry.circuitBreaker("fraudDetectionService", fraudDetectionConfig);
        
        // Compliance Service Circuit Breaker
        CircuitBreakerConfig complianceConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(60.0f)                     // Higher tolerance for compliance
            .waitDurationInOpenState(Duration.ofSeconds(60)) // Longer wait for compliance recovery
            .slidingWindowSize(20)                           // Larger window for compliance
            .minimumNumberOfCalls(10)
            .permittedNumberOfCallsInHalfOpenState(5)
            .slowCallRateThreshold(70.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(5)) // Compliance can be slower
            .build();
        
        registry.circuitBreaker("complianceService", complianceConfig);
        
        // Account Validation Service Circuit Breaker
        CircuitBreakerConfig accountValidationConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(40.0f)                     // Stricter for account validation
            .waitDurationInOpenState(Duration.ofSeconds(15)) // Faster recovery for accounts
            .slidingWindowSize(15)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(3)
            .slowCallRateThreshold(90.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(1)) // Account validation should be fast
            .build();
        
        registry.circuitBreaker("accountValidationService", accountValidationConfig);
        
        // Customer Credit Service Circuit Breaker
        CircuitBreakerConfig creditServiceConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(45.0f)
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .slidingWindowSize(12)
            .minimumNumberOfCalls(6)
            .permittedNumberOfCallsInHalfOpenState(4)
            .slowCallRateThreshold(85.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            .build();
        
        registry.circuitBreaker("customerCreditService", creditServiceConfig);
        
        return registry;
    }
    
    /**
     * Retry Registry with custom configurations
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryRegistry registry = RetryRegistry.ofDefaults();
        
        // Fraud Detection Retry Configuration
        RetryConfig fraudDetectionRetry = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .exponentialBackoffMultiplier(2.0)
            .retryOnException(throwable -> 
                throwable instanceof java.net.ConnectException ||
                throwable instanceof java.net.SocketTimeoutException ||
                throwable instanceof TimeoutException
            )
            .build();
        
        registry.retry("fraudDetectionService", fraudDetectionRetry);
        
        // Compliance Service Retry Configuration
        RetryConfig complianceRetry = RetryConfig.custom()
            .maxAttempts(5)                                  // More retries for compliance
            .waitDuration(Duration.ofSeconds(1))
            .exponentialBackoffMultiplier(1.5)
            .retryOnException(throwable -> 
                !(throwable instanceof IllegalArgumentException) &&
                !(throwable instanceof SecurityException)
            )
            .build();
        
        registry.retry("complianceService", complianceRetry);
        
        // Account Validation Retry Configuration
        RetryConfig accountValidationRetry = RetryConfig.custom()
            .maxAttempts(2)                                  // Fewer retries for fast services
            .waitDuration(Duration.ofMillis(200))
            .exponentialBackoffMultiplier(2.0)
            .build();
        
        registry.retry("accountValidationService", accountValidationRetry);
        
        return registry;
    }
    
    /**
     * Bulkhead Registry for resource isolation
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadRegistry registry = BulkheadRegistry.ofDefaults();
        
        // Fraud Detection Bulkhead
        BulkheadConfig fraudDetectionBulkhead = BulkheadConfig.custom()
            .maxConcurrentCalls(25)                          // Max 25 concurrent fraud checks
            .maxWaitDuration(Duration.ofMillis(100))         // Fast timeout for fraud checks
            .build();
        
        registry.bulkhead("fraudDetectionService", fraudDetectionBulkhead);
        
        // Compliance Bulkhead
        BulkheadConfig complianceBulkhead = BulkheadConfig.custom()
            .maxConcurrentCalls(15)                          // Fewer concurrent compliance checks
            .maxWaitDuration(Duration.ofSeconds(1))          // Longer wait for compliance
            .build();
        
        registry.bulkhead("complianceService", complianceBulkhead);
        
        // Account Validation Bulkhead
        BulkheadConfig accountValidationBulkhead = BulkheadConfig.custom()
            .maxConcurrentCalls(50)                          // More concurrent account validations
            .maxWaitDuration(Duration.ofMillis(50))          // Very fast timeout
            .build();
        
        registry.bulkhead("accountValidationService", accountValidationBulkhead);
        
        return registry;
    }
    
    /**
     * Time Limiter Registry for timeout handling
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterRegistry registry = TimeLimiterRegistry.ofDefaults();
        
        // Fraud Detection Time Limiter
        TimeLimiterConfig fraudDetectionTimeLimiter = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3))          // 3 second timeout for fraud
            .cancelRunningFuture(true)
            .build();
        
        registry.timeLimiter("fraudDetectionService", fraudDetectionTimeLimiter);
        
        // Compliance Time Limiter
        TimeLimiterConfig complianceTimeLimiter = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10))         // 10 second timeout for compliance
            .cancelRunningFuture(true)
            .build();
        
        registry.timeLimiter("complianceService", complianceTimeLimiter);
        
        // Account Validation Time Limiter
        TimeLimiterConfig accountValidationTimeLimiter = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(1))          // 1 second timeout for accounts
            .cancelRunningFuture(true)
            .build();
        
        registry.timeLimiter("accountValidationService", accountValidationTimeLimiter);
        
        return registry;
    }
    
    /**
     * Health indicators for circuit breakers
     */
    @Bean
    public HealthIndicator fraudDetectionHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerHealthIndicator(
            circuitBreakerRegistry.circuitBreaker("fraudDetectionService")
        );
    }
    
    @Bean
    public HealthIndicator complianceHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerHealthIndicator(
            circuitBreakerRegistry.circuitBreaker("complianceService")
        );
    }
    
    @Bean
    public HealthIndicator accountValidationHealthIndicator(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new CircuitBreakerHealthIndicator(
            circuitBreakerRegistry.circuitBreaker("accountValidationService")
        );
    }
    
    /**
     * Custom health indicator for circuit breakers
     */
    private static class CircuitBreakerHealthIndicator implements HealthIndicator {
        private final CircuitBreaker circuitBreaker;
        
        public CircuitBreakerHealthIndicator(CircuitBreaker circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }
        
        @Override
        public org.springframework.boot.actuate.health.Health health() {
            CircuitBreaker.State state = circuitBreaker.getState();
            
            switch (state) {
                case CLOSED:
                    return org.springframework.boot.actuator.health.Health.up()
                        .withDetail("state", state.name())
                        .withDetail("failureRate", circuitBreaker.getMetrics().getFailureRate())
                        .withDetail("calls", circuitBreaker.getMetrics().getNumberOfBufferedCalls())
                        .build();
                        
                case OPEN:
                    return org.springframework.boot.actuator.health.Health.down()
                        .withDetail("state", state.name())
                        .withDetail("failureRate", circuitBreaker.getMetrics().getFailureRate())
                        .withDetail("lastFailure", "Circuit breaker is open due to failures")
                        .build();
                        
                case HALF_OPEN:
                    return org.springframework.boot.actuator.health.Health.unknown()
                        .withDetail("state", state.name())
                        .withDetail("message", "Circuit breaker is in half-open state, testing recovery")
                        .build();
                        
                default:
                    return org.springframework.boot.actuator.health.Health.unknown()
                        .withDetail("state", state.name())
                        .build();
            }
        }
    }
}