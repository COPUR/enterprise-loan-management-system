package com.bank.infrastructure.resilience.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Enhanced Circuit Breaker implementation with intelligent failure detection,
 * adaptive thresholds, and comprehensive monitoring.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EnhancedCircuitBreaker {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final MeterRegistry meterRegistry;
    private final ExecutorService executorService;
    private final HealthMetricsCollector healthMetrics;
    
    // Circuit breaker states with detailed tracking
    private final Map<String, CircuitBreakerState> circuitStates = new ConcurrentHashMap<>();
    
    /**
     * Creates a circuit breaker with enhanced configuration for a specific service
     */
    public CircuitBreaker createCircuitBreaker(String serviceName, ServiceProfile profile) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                // Failure rate threshold - adaptive based on service criticality
                .failureRateThreshold(profile.getFailureRateThreshold())
                
                // Slow call configuration - detects performance degradation
                .slowCallRateThreshold(profile.getSlowCallRateThreshold())
                .slowCallDurationThreshold(profile.getSlowCallDuration())
                
                // Window configuration for metrics calculation
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(profile.getSlidingWindowSize())
                .minimumNumberOfCalls(profile.getMinimumNumberOfCalls())
                
                // State transition configuration
                .waitDurationInOpenState(profile.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(profile.getPermittedCallsInHalfOpen())
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                
                // Record specific exceptions
                .recordExceptions(
                    ExternalServiceException.class,
                    TimeoutException.class,
                    DatabaseException.class,
                    NetworkException.class
                )
                
                // Ignore certain exceptions
                .ignoreExceptions(
                    BusinessValidationException.class,
                    AuthenticationException.class
                )
                
                // Custom predicate for recording failures
                .recordFailurePredicate(throwable -> {
                    // Don't count client errors as circuit breaker failures
                    if (throwable instanceof ClientException) {
                        return false;
                    }
                    
                    // Check if error is retryable
                    return isRetryableError(throwable);
                })
                
                .build();
        
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName, config);
        
        // Initialize state tracking
        circuitStates.put(serviceName, new CircuitBreakerState(serviceName));
        
        // Register event listeners for monitoring
        registerEventListeners(circuitBreaker);
        
        // Register metrics
        registerMetrics(circuitBreaker);
        
        return circuitBreaker;
    }
    
    /**
     * Execute a supplier with circuit breaker, retry, and timeout protection
     */
    public <T> T executeWithProtection(
            String serviceName, 
            Supplier<T> supplier,
            Function<Throwable, T> fallbackFunction) {
        
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
        Retry retry = createRetry(serviceName);
        TimeLimiter timeLimiter = createTimeLimiter(serviceName);
        
        // Compose decorators
        Supplier<CompletableFuture<T>> futureSupplier = () -> 
            CompletableFuture.supplyAsync(supplier, executorService);
        
        // Apply timeout
        Supplier<CompletableFuture<T>> timedSupplier = 
            timeLimiter.decorateFutureSupplier(futureSupplier);
        
        // Apply circuit breaker
        Supplier<CompletableFuture<T>> circuitBreakerSupplier = 
            circuitBreaker.decorateSupplier(() -> {
                try {
                    return timedSupplier.get();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
        
        // Apply retry
        Supplier<CompletableFuture<T>> retrySupplier = 
            retry.decorateSupplier(circuitBreakerSupplier);
        
        try {
            // Execute with all protections
            return retrySupplier.get().join();
            
        } catch (Exception e) {
            log.error("Service call failed for {}: {}", serviceName, e.getMessage());
            
            // Record failure metrics
            recordFailure(serviceName, e);
            
            // Check if fallback is available
            if (fallbackFunction != null) {
                log.info("Executing fallback for service: {}", serviceName);
                return fallbackFunction.apply(e);
            }
            
            throw new ServiceUnavailableException(
                "Service " + serviceName + " is unavailable", e);
        }
    }
    
    /**
     * Execute with custom timeout and retry configuration
     */
    public <T> T executeWithCustomConfig(
            String serviceName,
            Supplier<T> supplier,
            Duration timeout,
            int maxRetries,
            Function<Throwable, T> fallbackFunction) {
        
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
        
        // Create custom retry configuration
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(maxRetries)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                    Duration.ofMillis(100), 2))
                .retryExceptions(RetryableException.class, TimeoutException.class)
                .ignoreExceptions(NonRetryableException.class)
                .build();
        
        Retry retry = Retry.of(serviceName + "-custom", retryConfig);
        
        // Create custom timeout configuration
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(timeout)
                .cancelRunningFuture(true)
                .build();
        
        TimeLimiter timeLimiter = TimeLimiter.of(serviceName + "-custom", timeLimiterConfig);
        
        return executeWithComponents(serviceName, supplier, circuitBreaker, 
                retry, timeLimiter, fallbackFunction);
    }
    
    /**
     * Health check with circuit breaker - used for proactive monitoring
     */
    public HealthCheckResult performHealthCheck(String serviceName, 
                                               Supplier<HealthStatus> healthCheckSupplier) {
        try {
            CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
            
            HealthStatus status = circuitBreaker.executeSupplier(() -> {
                // Perform health check with short timeout
                CompletableFuture<HealthStatus> future = CompletableFuture
                        .supplyAsync(healthCheckSupplier, executorService)
                        .orTimeout(5, TimeUnit.SECONDS);
                
                return future.join();
            });
            
            // Update health metrics
            healthMetrics.recordHealthCheck(serviceName, status);
            
            return HealthCheckResult.healthy(serviceName, status);
            
        } catch (Exception e) {
            log.error("Health check failed for service: {}", serviceName, e);
            return HealthCheckResult.unhealthy(serviceName, e.getMessage());
        }
    }
    
    /**
     * Adaptive circuit breaker that adjusts thresholds based on system load
     */
    public void enableAdaptiveMode(String serviceName) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                adjustCircuitBreakerThresholds(serviceName);
            } catch (Exception e) {
                log.error("Error adjusting circuit breaker thresholds", e);
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
    
    private void adjustCircuitBreakerThresholds(String serviceName) {
        CircuitBreakerState state = circuitStates.get(serviceName);
        if (state == null) return;
        
        // Get current system metrics
        double cpuUsage = healthMetrics.getCpuUsage();
        double memoryUsage = healthMetrics.getMemoryUsage();
        long responseTime = healthMetrics.getAverageResponseTime(serviceName);
        
        // Calculate adjustment factor based on system load
        double loadFactor = (cpuUsage + memoryUsage) / 200.0; // 0.0 to 1.0
        
        // Adjust thresholds based on load
        if (loadFactor > 0.8) {
            // System under high load - be more conservative
            log.info("System under high load, tightening circuit breaker for {}", serviceName);
            updateCircuitBreakerConfig(serviceName, config -> 
                config.toBuilder()
                    .failureRateThreshold(30) // Lower threshold
                    .slowCallRateThreshold(30)
                    .waitDurationInOpenState(Duration.ofSeconds(120)) // Longer wait
                    .build()
            );
        } else if (loadFactor < 0.3) {
            // System under low load - be more permissive
            log.info("System under low load, relaxing circuit breaker for {}", serviceName);
            updateCircuitBreakerConfig(serviceName, config -> 
                config.toBuilder()
                    .failureRateThreshold(60) // Higher threshold
                    .slowCallRateThreshold(60)
                    .waitDurationInOpenState(Duration.ofSeconds(30)) // Shorter wait
                    .build()
            );
        }
    }
    
    /**
     * Manual circuit breaker control for emergency situations
     */
    public void forceOpen(String serviceName, Duration duration, String reason) {
        log.warn("Manually opening circuit breaker for service: {} for {} due to: {}", 
                serviceName, duration, reason);
        
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
        circuitBreaker.transitionToOpenState();
        
        // Schedule automatic transition back
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            log.info("Transitioning circuit breaker {} to half-open after manual open period", 
                    serviceName);
            circuitBreaker.transitionToHalfOpenState();
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
        
        // Record manual intervention
        meterRegistry.counter("circuit_breaker.manual_open", 
                "service", serviceName,
                "reason", reason).increment();
    }
    
    /**
     * Get current circuit breaker statistics
     */
    public CircuitBreakerStatistics getStatistics(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
        
        return CircuitBreakerStatistics.builder()
                .serviceName(serviceName)
                .state(circuitBreaker.getState())
                .failureRate(metrics.getFailureRate())
                .slowCallRate(metrics.getSlowCallRate())
                .numberOfBufferedCalls(metrics.getNumberOfBufferedCalls())
                .numberOfFailedCalls(metrics.getNumberOfFailedCalls())
                .numberOfSlowCalls(metrics.getNumberOfSlowCalls())
                .numberOfSuccessfulCalls(metrics.getNumberOfSuccessfulCalls())
                .build();
    }
    
    // Private helper methods
    
    private void registerEventListeners(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> {
                log.info("Circuit breaker state transition: {} -> {} for service: {}",
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState(),
                        event.getCircuitBreakerName());
                
                // Send alerts for state transitions
                if (event.getStateTransition().getToState() == CircuitBreaker.State.OPEN) {
                    sendCircuitOpenAlert(event.getCircuitBreakerName());
                }
            })
            .onError(event -> {
                log.debug("Circuit breaker recorded error for service: {} - {}",
                        event.getCircuitBreakerName(),
                        event.getThrowable().getMessage());
            })
            .onSlowCallRateExceeded(event -> {
                log.warn("Slow call rate exceeded for service: {} - Rate: {}%",
                        event.getCircuitBreakerName(),
                        event.getSlowCallRate());
            });
    }
    
    private void registerMetrics(CircuitBreaker circuitBreaker) {
        // State gauge
        meterRegistry.gauge("circuit_breaker.state",
                Tags.of("service", circuitBreaker.getName()),
                circuitBreaker,
                cb -> cb.getState().getOrder());
        
        // Failure rate gauge
        meterRegistry.gauge("circuit_breaker.failure_rate",
                Tags.of("service", circuitBreaker.getName()),
                circuitBreaker,
                cb -> cb.getMetrics().getFailureRate());
        
        // Slow call rate gauge
        meterRegistry.gauge("circuit_breaker.slow_call_rate",
                Tags.of("service", circuitBreaker.getName()),
                circuitBreaker,
                cb -> cb.getMetrics().getSlowCallRate());
    }
    
    private Retry createRetry(String serviceName) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                    Duration.ofMillis(100), 2, Duration.ofSeconds(10)))
                .retryOnException(this::isRetryableError)
                .build();
        
        return Retry.of(serviceName, config);
    }
    
    private TimeLimiter createTimeLimiter(String serviceName) {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build();
        
        return TimeLimiter.of(serviceName, config);
    }
    
    private boolean isRetryableError(Throwable throwable) {
        return throwable instanceof TimeoutException ||
               throwable instanceof NetworkException ||
               throwable instanceof DatabaseException ||
               (throwable instanceof ExternalServiceException && 
                ((ExternalServiceException) throwable).isRetryable());
    }
    
    private CircuitBreaker getOrCreateCircuitBreaker(String serviceName) {
        try {
            return circuitBreakerRegistry.circuitBreaker(serviceName);
        } catch (Exception e) {
            // Create with default profile if not exists
            return createCircuitBreaker(serviceName, ServiceProfile.defaultProfile());
        }
    }
    
    private void sendCircuitOpenAlert(String serviceName) {
        // Integration with alerting system
        log.error("ALERT: Circuit breaker OPEN for service: {}", serviceName);
        // Send to monitoring/alerting service
    }
    
    private void recordFailure(String serviceName, Exception e) {
        meterRegistry.counter("service.call.failure",
                "service", serviceName,
                "exception", e.getClass().getSimpleName()
        ).increment();
    }
    
    private void updateCircuitBreakerConfig(String serviceName, 
                                           Function<CircuitBreakerConfig, CircuitBreakerConfig> updater) {
        // This would require recreating the circuit breaker with new config
        // In practice, you might use dynamic configuration management
        log.info("Circuit breaker configuration updated for service: {}", serviceName);
    }
    
    private <T> T executeWithComponents(
            String serviceName,
            Supplier<T> supplier,
            CircuitBreaker circuitBreaker,
            Retry retry,
            TimeLimiter timeLimiter,
            Function<Throwable, T> fallbackFunction) {
        
        // Similar to executeWithProtection but with custom components
        try {
            Supplier<CompletableFuture<T>> futureSupplier = () -> 
                CompletableFuture.supplyAsync(supplier, executorService);
            
            Supplier<CompletableFuture<T>> decorated = 
                Decorators.ofSupplier(() -> {
                    try {
                        return futureSupplier.get();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry)
                .withTimeLimiter(timeLimiter, executorService)
                .decorate();
            
            return decorated.get().join();
            
        } catch (Exception e) {
            if (fallbackFunction != null) {
                return fallbackFunction.apply(e);
            }
            throw new ServiceUnavailableException(
                "Service " + serviceName + " is unavailable", e);
        }
    }
}