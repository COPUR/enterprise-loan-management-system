package com.bank.infrastructure.resilience;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
public class CircuitBreakerService implements HealthIndicator {

    @Value("${circuit-breaker.failure-threshold:5}")
    private int failureThreshold = 5;

    @Value("${circuit-breaker.recovery-timeout:30000}")
    private long recoveryTimeoutMs = 30000;

    @Value("${circuit-breaker.success-threshold:3}")
    private int successThreshold = 3;

    private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    public enum CircuitBreakerState {
        CLOSED,    // Normal operation
        OPEN,      // Circuit breaker is open, rejecting calls
        HALF_OPEN  // Testing if service has recovered
    }

    public static class CircuitBreakerException extends RuntimeException {
        public CircuitBreakerException(String message) {
            super(message);
        }
    }

    public static class CircuitBreakerConfig {
        private final int failureThreshold;
        private final long recoveryTimeoutMs;
        private final int successThreshold;

        public CircuitBreakerConfig(int failureThreshold, long recoveryTimeoutMs, int successThreshold) {
            this.failureThreshold = failureThreshold;
            this.recoveryTimeoutMs = recoveryTimeoutMs;
            this.successThreshold = successThreshold;
        }

        public int getFailureThreshold() { return failureThreshold; }
        public long getRecoveryTimeoutMs() { return recoveryTimeoutMs; }
        public int getSuccessThreshold() { return successThreshold; }
    }

    private static class CircuitBreaker {
        private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong rejectedCalls = new AtomicLong(0);
        private final CircuitBreakerConfig config;
        private final String serviceName;

        public CircuitBreaker(String serviceName, CircuitBreakerConfig config) {
            this.serviceName = serviceName;
            this.config = config;
        }

        public <T> T execute(Supplier<T> operation) {
            totalCalls.incrementAndGet();

            if (state == CircuitBreakerState.OPEN) {
                if (shouldAttemptReset()) {
                    state = CircuitBreakerState.HALF_OPEN;
                    successCount.set(0);
                } else {
                    rejectedCalls.incrementAndGet();
                    throw new CircuitBreakerException("Circuit breaker is OPEN for service: " + serviceName);
                }
            }

            try {
                T result = operation.get();
                onSuccess();
                return result;
            } catch (Exception e) {
                onFailure();
                throw e;
            }
        }

        private boolean shouldAttemptReset() {
            return System.currentTimeMillis() - lastFailureTime.get() >= config.getRecoveryTimeoutMs();
        }

        private void onSuccess() {
            failureCount.set(0);
            if (state == CircuitBreakerState.HALF_OPEN) {
                if (successCount.incrementAndGet() >= config.getSuccessThreshold()) {
                    state = CircuitBreakerState.CLOSED;
                    successCount.set(0);
                }
            }
        }

        private void onFailure() {
            lastFailureTime.set(System.currentTimeMillis());
            if (failureCount.incrementAndGet() >= config.getFailureThreshold()) {
                state = CircuitBreakerState.OPEN;
            }
        }

        public CircuitBreakerState getState() {
            return state;
        }

        public int getFailureCount() {
            return failureCount.get();
        }

        public long getTotalCalls() {
            return totalCalls.get();
        }

        public long getRejectedCalls() {
            return rejectedCalls.get();
        }

        public double getFailureRate() {
            long total = totalCalls.get();
            return total > 0 ? (double) failureCount.get() / total : 0.0;
        }
    }

    public <T> T executeWithCircuitBreaker(String serviceName, Supplier<T> operation) {
        CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(serviceName);
        return circuitBreaker.execute(operation);
    }

    public <T> T executeWithCircuitBreaker(String serviceName, Supplier<T> operation, T fallbackValue) {
        try {
            return executeWithCircuitBreaker(serviceName, operation);
        } catch (CircuitBreakerException e) {
            return fallbackValue;
        }
    }

    public void executeWithCircuitBreaker(String serviceName, Runnable operation) {
        executeWithCircuitBreaker(serviceName, () -> {
            operation.run();
            return null;
        });
    }

    private CircuitBreaker getOrCreateCircuitBreaker(String serviceName) {
        return circuitBreakers.computeIfAbsent(serviceName, 
            name -> new CircuitBreaker(name, new CircuitBreakerConfig(
                failureThreshold, recoveryTimeoutMs, successThreshold)));
    }

    public CircuitBreakerState getCircuitBreakerState(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
        return circuitBreaker != null ? circuitBreaker.getState() : CircuitBreakerState.CLOSED;
    }

    public boolean isCircuitBreakerOpen(String serviceName) {
        return getCircuitBreakerState(serviceName) == CircuitBreakerState.OPEN;
    }

    public CircuitBreakerMetrics getMetrics(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(serviceName);
        if (circuitBreaker == null) {
            return new CircuitBreakerMetrics(serviceName, CircuitBreakerState.CLOSED, 0, 0, 0, 0.0);
        }
        
        return new CircuitBreakerMetrics(
            serviceName,
            circuitBreaker.getState(),
            circuitBreaker.getFailureCount(),
            circuitBreaker.getTotalCalls(),
            circuitBreaker.getRejectedCalls(),
            circuitBreaker.getFailureRate()
        );
    }

    public static class CircuitBreakerMetrics {
        private final String serviceName;
        private final CircuitBreakerState state;
        private final int failureCount;
        private final long totalCalls;
        private final long rejectedCalls;
        private final double failureRate;

        public CircuitBreakerMetrics(String serviceName, CircuitBreakerState state, int failureCount, 
                                   long totalCalls, long rejectedCalls, double failureRate) {
            this.serviceName = serviceName;
            this.state = state;
            this.failureCount = failureCount;
            this.totalCalls = totalCalls;
            this.rejectedCalls = rejectedCalls;
            this.failureRate = failureRate;
        }

        public String getServiceName() { return serviceName; }
        public CircuitBreakerState getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public long getTotalCalls() { return totalCalls; }
        public long getRejectedCalls() { return rejectedCalls; }
        public double getFailureRate() { return failureRate; }
    }

    @Override
    public Health health() {
        boolean allHealthy = circuitBreakers.values().stream()
            .allMatch(cb -> cb.getState() != CircuitBreakerState.OPEN);
        
        if (allHealthy) {
            return Health.up()
                .withDetail("circuit-breakers", circuitBreakers.size())
                .withDetail("status", "All circuit breakers operational")
                .build();
        } else {
            return Health.down()
                .withDetail("circuit-breakers", circuitBreakers.size())
                .withDetail("open-circuits", circuitBreakers.values().stream()
                    .filter(cb -> cb.getState() == CircuitBreakerState.OPEN)
                    .count())
                .withDetail("status", "Some circuit breakers are OPEN")
                .build();
        }
    }
}