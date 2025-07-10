package com.bank.infrastructure.resilience.config;

import com.bank.infrastructure.resilience.circuitbreaker.EnhancedCircuitBreaker;
import com.bank.infrastructure.resilience.circuitbreaker.ServiceProfile;
import com.bank.infrastructure.resilience.monitoring.ExceptionMetrics;
import com.bank.infrastructure.resilience.monitoring.HealthMetricsCollector;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Central configuration for resilience patterns including circuit breakers,
 * retries, timeouts, and exception handling.
 */
@Configuration
public class ResilienceConfiguration {
    
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
    
    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }
    
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }
    
    @Bean
    public ExecutorService resilienceExecutorService() {
        return Executors.newFixedThreadPool(50, r -> {
            Thread thread = new Thread(r);
            thread.setName("resilience-executor-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @Bean
    public ExceptionMetrics exceptionMetrics(MeterRegistry meterRegistry) {
        return new ExceptionMetrics(meterRegistry);
    }
    
    @Bean
    public HealthMetricsCollector healthMetricsCollector(MeterRegistry meterRegistry) {
        return new HealthMetricsCollector(meterRegistry);
    }
    
    @Bean
    public EnhancedCircuitBreaker enhancedCircuitBreaker(
            CircuitBreakerRegistry circuitBreakerRegistry,
            MeterRegistry meterRegistry,
            ExecutorService resilienceExecutorService,
            HealthMetricsCollector healthMetricsCollector) {
        
        return new EnhancedCircuitBreaker(
                circuitBreakerRegistry,
                meterRegistry,
                resilienceExecutorService,
                healthMetricsCollector
        );
    }
    
    /**
     * Service-specific circuit breaker profiles
     */
    @Bean
    public ServiceProfileRegistry serviceProfileRegistry() {
        ServiceProfileRegistry registry = new ServiceProfileRegistry();
        
        // Critical services - more conservative settings
        registry.register("payment-service", ServiceProfile.builder()
                .serviceName("payment-service")
                .failureRateThreshold(25.0f) // Trip at 25% failure rate
                .slowCallRateThreshold(25.0f)
                .slowCallDuration(Duration.ofSeconds(2))
                .slidingWindowSize(100) // Last 100 calls
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedCallsInHalfOpen(5)
                .build());
        
        registry.register("loan-service", ServiceProfile.builder()
                .serviceName("loan-service")
                .failureRateThreshold(30.0f)
                .slowCallRateThreshold(30.0f)
                .slowCallDuration(Duration.ofSeconds(3))
                .slidingWindowSize(100)
                .minimumNumberOfCalls(10)
                .waitDurationInOpenState(Duration.ofSeconds(45))
                .permittedCallsInHalfOpen(5)
                .build());
        
        // External services - more tolerant settings
        registry.register("credit-bureau", ServiceProfile.builder()
                .serviceName("credit-bureau")
                .failureRateThreshold(40.0f) // External service might be less reliable
                .slowCallRateThreshold(50.0f)
                .slowCallDuration(Duration.ofSeconds(5))
                .slidingWindowSize(50)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedCallsInHalfOpen(3)
                .build());
        
        registry.register("notification-service", ServiceProfile.builder()
                .serviceName("notification-service")
                .failureRateThreshold(50.0f) // Non-critical, can tolerate more failures
                .slowCallRateThreshold(60.0f)
                .slowCallDuration(Duration.ofSeconds(10))
                .slidingWindowSize(50)
                .minimumNumberOfCalls(5)
                .waitDurationInOpenState(Duration.ofSeconds(20))
                .permittedCallsInHalfOpen(3)
                .build());
        
        // Database operations
        registry.register("database-primary", ServiceProfile.builder()
                .serviceName("database-primary")
                .failureRateThreshold(10.0f) // Very conservative for database
                .slowCallRateThreshold(20.0f)
                .slowCallDuration(Duration.ofMillis(500))
                .slidingWindowSize(200)
                .minimumNumberOfCalls(20)
                .waitDurationInOpenState(Duration.ofSeconds(120))
                .permittedCallsInHalfOpen(10)
                .build());
        
        return registry;
    }
}

/**
 * Configuration properties for resilience settings
 */
@ConfigurationProperties(prefix = "resilience")
class ResilienceProperties {
    
    private CircuitBreakerDefaults circuitBreaker = new CircuitBreakerDefaults();
    private RetryDefaults retry = new RetryDefaults();
    private TimeoutDefaults timeout = new TimeoutDefaults();
    private RateLimitDefaults rateLimit = new RateLimitDefaults();
    
    public static class CircuitBreakerDefaults {
        private float failureRateThreshold = 50.0f;
        private float slowCallRateThreshold = 50.0f;
        private Duration slowCallDuration = Duration.ofSeconds(3);
        private int slidingWindowSize = 100;
        private int minimumNumberOfCalls = 10;
        private Duration waitDurationInOpenState = Duration.ofSeconds(60);
        private int permittedCallsInHalfOpen = 5;
        
        // Getters and setters...
    }
    
    public static class RetryDefaults {
        private int maxAttempts = 3;
        private Duration initialInterval = Duration.ofMillis(100);
        private double multiplier = 2.0;
        private Duration maxInterval = Duration.ofSeconds(10);
        
        // Getters and setters...
    }
    
    public static class TimeoutDefaults {
        private Duration defaultTimeout = Duration.ofSeconds(10);
        private Duration databaseTimeout = Duration.ofSeconds(5);
        private Duration externalServiceTimeout = Duration.ofSeconds(30);
        
        // Getters and setters...
    }
    
    public static class RateLimitDefaults {
        private int requestsPerSecond = 100;
        private int burstCapacity = 200;
        private Duration blockDuration = Duration.ofMinutes(1);
        
        // Getters and setters...
    }
}