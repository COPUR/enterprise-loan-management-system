package com.bank.infrastructure.resilience.service;

import com.bank.infrastructure.resilience.circuitbreaker.EnhancedCircuitBreaker;
import com.bank.infrastructure.resilience.pattern.BulkheadPattern;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Template class for implementing resilient service calls with all protection patterns.
 * Provides a clean API for service-to-service communication with built-in resilience.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientServiceTemplate {
    
    private final EnhancedCircuitBreaker enhancedCircuitBreaker;
    private final BulkheadPattern bulkheadPattern;
    private final MeterRegistry meterRegistry;
    
    /**
     * Execute a synchronous service call with full resilience protection
     */
    public <T> T executeSync(
            String serviceName,
            Supplier<T> serviceCall,
            Function<Throwable, T> fallback) {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Execute with circuit breaker and bulkhead protection
            T result = enhancedCircuitBreaker.executeWithProtection(
                    serviceName,
                    () -> bulkheadPattern.executeWithSemaphoreBulkhead(
                            serviceName,
                            serviceCall,
                            BulkheadProfile.defaultProfile()
                    ),
                    fallback
            );
            
            recordSuccess(serviceName, sample);
            return result;
            
        } catch (Exception e) {
            recordFailure(serviceName, sample, e);
            throw e;
        }
    }
    
    /**
     * Execute an asynchronous service call with resilience protection
     */
    public <T> CompletableFuture<T> executeAsync(
            String serviceName,
            Supplier<CompletableFuture<T>> asyncServiceCall,
            Function<Throwable, T> fallback) {
        
        return bulkheadPattern.executeWithThreadPoolBulkhead(
                serviceName,
                () -> {
                    Timer.Sample sample = Timer.start(meterRegistry);
                    
                    return enhancedCircuitBreaker.executeWithProtection(
                            serviceName,
                            () -> {
                                try {
                                    T result = asyncServiceCall.get()
                                            .get(30, TimeUnit.SECONDS);
                                    recordSuccess(serviceName, sample);
                                    return result;
                                } catch (Exception e) {
                                    recordFailure(serviceName, sample, e);
                                    throw new RuntimeException(e);
                                }
                            },
                            fallback
                    );
                },
                ThreadPoolBulkheadProfile.defaultProfile()
        );
    }
    
    /**
     * Execute with custom timeout
     */
    public <T> T executeWithTimeout(
            String serviceName,
            Supplier<T> serviceCall,
            Duration timeout,
            Function<Throwable, T> fallback) {
        
        return enhancedCircuitBreaker.executeWithCustomConfig(
                serviceName,
                serviceCall,
                timeout,
                3, // max retries
                fallback
        );
    }
    
    /**
     * Execute critical operation with stricter resilience settings
     */
    public <T> T executeCritical(
            String serviceName,
            Supplier<T> serviceCall,
            Function<Throwable, T> fallback) {
        
        // Use more conservative bulkhead for critical operations
        return bulkheadPattern.executeWithTimeoutAndBulkhead(
                serviceName + "-critical",
                () -> enhancedCircuitBreaker.executeWithCustomConfig(
                        serviceName,
                        serviceCall,
                        Duration.ofSeconds(5), // Shorter timeout
                        1, // No retries for critical operations
                        fallback
                ),
                Duration.ofSeconds(10),
                BulkheadProfile.criticalService()
        );
    }
    
    /**
     * Execute batch operations with bulkhead isolation
     */
    public <T> CompletableFuture<T> executeBatch(
            String serviceName,
            Supplier<T> batchOperation) {
        
        return bulkheadPattern.executeWithThreadPoolBulkhead(
                serviceName + "-batch",
                () -> {
                    try {
                        return batchOperation.get();
                    } catch (Exception e) {
                        log.error("Batch operation failed for service: {}", serviceName, e);
                        throw new RuntimeException(e);
                    }
                },
                ThreadPoolBulkheadProfile.builder()
                        .maxThreadPoolSize(5) // Limited threads for batch operations
                        .coreThreadPoolSize(2)
                        .queueCapacity(50)
                        .keepAliveDuration(Duration.ofMinutes(5))
                        .build()
        );
    }
    
    /**
     * Execute with health check and automatic circuit breaker management
     */
    public <T> T executeWithHealthCheck(
            String serviceName,
            Supplier<T> serviceCall,
            Supplier<HealthStatus> healthCheck,
            Function<Throwable, T> fallback) {
        
        // Perform health check first
        HealthCheckResult healthResult = enhancedCircuitBreaker.performHealthCheck(
                serviceName, healthCheck);
        
        if (!healthResult.isHealthy()) {
            log.warn("Service {} is unhealthy, using fallback", serviceName);
            return fallback.apply(new ServiceUnhealthyException(serviceName));
        }
        
        // Execute if healthy
        return executeSync(serviceName, serviceCall, fallback);
    }
    
    /**
     * Builder pattern for complex resilient operations
     */
    public <T> ResilientOperationBuilder<T> forService(String serviceName) {
        return new ResilientOperationBuilder<>(serviceName, this);
    }
    
    // Private helper methods
    
    private void recordSuccess(String serviceName, Timer.Sample sample) {
        sample.stop(Timer.builder("service.call.duration")
                .tag("service", serviceName)
                .tag("outcome", "success")
                .register(meterRegistry));
        
        meterRegistry.counter("service.call.total",
                "service", serviceName,
                "outcome", "success"
        ).increment();
    }
    
    private void recordFailure(String serviceName, Timer.Sample sample, Exception e) {
        sample.stop(Timer.builder("service.call.duration")
                .tag("service", serviceName)
                .tag("outcome", "failure")
                .tag("exception", e.getClass().getSimpleName())
                .register(meterRegistry));
        
        meterRegistry.counter("service.call.total",
                "service", serviceName,
                "outcome", "failure",
                "exception", e.getClass().getSimpleName()
        ).increment();
    }
    
    /**
     * Fluent builder for complex resilient operations
     */
    public static class ResilientOperationBuilder<T> {
        private final String serviceName;
        private final ResilientServiceTemplate template;
        private Duration timeout = Duration.ofSeconds(10);
        private int maxRetries = 3;
        private Function<Throwable, T> fallback;
        private Supplier<HealthStatus> healthCheck;
        private boolean critical = false;
        
        ResilientOperationBuilder(String serviceName, ResilientServiceTemplate template) {
            this.serviceName = serviceName;
            this.template = template;
        }
        
        public ResilientOperationBuilder<T> withTimeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public ResilientOperationBuilder<T> withMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public ResilientOperationBuilder<T> withFallback(Function<Throwable, T> fallback) {
            this.fallback = fallback;
            return this;
        }
        
        public ResilientOperationBuilder<T> withHealthCheck(Supplier<HealthStatus> healthCheck) {
            this.healthCheck = healthCheck;
            return this;
        }
        
        public ResilientOperationBuilder<T> critical() {
            this.critical = true;
            return this;
        }
        
        public T execute(Supplier<T> operation) {
            if (healthCheck != null) {
                return template.executeWithHealthCheck(
                        serviceName, operation, healthCheck, fallback);
            } else if (critical) {
                return template.executeCritical(serviceName, operation, fallback);
            } else {
                return template.executeWithTimeout(
                        serviceName, operation, timeout, fallback);
            }
        }
        
        public CompletableFuture<T> executeAsync(Supplier<CompletableFuture<T>> operation) {
            return template.executeAsync(serviceName, operation, fallback);
        }
    }
}

// Supporting classes

class ServiceUnhealthyException extends RuntimeException {
    public ServiceUnhealthyException(String serviceName) {
        super("Service " + serviceName + " is unhealthy");
    }
}

@lombok.Data
@lombok.Builder
class HealthStatus {
    private boolean healthy;
    private String message;
    private long responseTimeMs;
    
    public static HealthStatus healthy() {
        return HealthStatus.builder()
                .healthy(true)
                .message("Service is healthy")
                .build();
    }
    
    public static HealthStatus unhealthy(String reason) {
        return HealthStatus.builder()
                .healthy(false)
                .message(reason)
                .build();
    }
}

@lombok.Data
@lombok.Builder
class HealthCheckResult {
    private String serviceName;
    private boolean healthy;
    private String message;
    private long checkDurationMs;
    
    public static HealthCheckResult healthy(String serviceName, HealthStatus status) {
        return HealthCheckResult.builder()
                .serviceName(serviceName)
                .healthy(true)
                .message(status.getMessage())
                .build();
    }
    
    public static HealthCheckResult unhealthy(String serviceName, String message) {
        return HealthCheckResult.builder()
                .serviceName(serviceName)
                .healthy(false)
                .message(message)
                .build();
    }
}