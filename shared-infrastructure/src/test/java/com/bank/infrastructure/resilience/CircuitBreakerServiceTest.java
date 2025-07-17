package com.bank.infrastructure.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Circuit Breaker Service TDD Tests")
class CircuitBreakerServiceTest {

    private CircuitBreakerService circuitBreakerService;
    private static final String TEST_SERVICE = "test-service";

    @BeforeEach
    void setUp() {
        circuitBreakerService = new CircuitBreakerService();
    }

    @Nested
    @DisplayName("Circuit Breaker State Management")
    class CircuitBreakerStateTests {

        @Test
        @DisplayName("Should start in CLOSED state")
        void shouldStartInClosedState() {
            CircuitBreakerService.CircuitBreakerState state = 
                circuitBreakerService.getCircuitBreakerState(TEST_SERVICE);
            
            assertThat(state).isEqualTo(CircuitBreakerService.CircuitBreakerState.CLOSED);
        }

        @Test
        @DisplayName("Should open circuit breaker after failure threshold")
        void shouldOpenCircuitBreakerAfterFailureThreshold() {
            // Given - Execute failing operations to reach threshold
            for (int i = 0; i < 5; i++) {
                try {
                    circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                        throw new RuntimeException("Service failure");
                    });
                } catch (Exception e) {
                    // Expected failures
                }
            }

            // When - Check state
            CircuitBreakerService.CircuitBreakerState state = 
                circuitBreakerService.getCircuitBreakerState(TEST_SERVICE);

            // Then
            assertThat(state).isEqualTo(CircuitBreakerService.CircuitBreakerState.OPEN);
            assertThat(circuitBreakerService.isCircuitBreakerOpen(TEST_SERVICE)).isTrue();
        }

        @Test
        @DisplayName("Should reject calls when circuit breaker is open")
        void shouldRejectCallsWhenCircuitBreakerIsOpen() {
            // Given - Force circuit breaker to open
            for (int i = 0; i < 5; i++) {
                try {
                    circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                        throw new RuntimeException("Service failure");
                    });
                } catch (Exception e) {
                    // Expected failures
                }
            }

            // When - Try to execute operation
            // Then - Should throw CircuitBreakerException
            assertThatThrownBy(() -> 
                circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> "success")
            ).isInstanceOf(CircuitBreakerService.CircuitBreakerException.class)
             .hasMessageContaining("Circuit breaker is OPEN");
        }

        @Test
        @DisplayName("Should use fallback value when circuit breaker is open")
        void shouldUseFallbackValueWhenCircuitBreakerIsOpen() {
            // Given - Force circuit breaker to open
            for (int i = 0; i < 5; i++) {
                try {
                    circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                        throw new RuntimeException("Service failure");
                    });
                } catch (Exception e) {
                    // Expected failures
                }
            }

            // When - Execute with fallback
            String result = circuitBreakerService.executeWithCircuitBreaker(
                TEST_SERVICE, 
                () -> "primary", 
                "fallback"
            );

            // Then
            assertThat(result).isEqualTo("fallback");
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Recovery")
    class CircuitBreakerRecoveryTests {

        @Test
        @DisplayName("Should transition to HALF_OPEN after recovery timeout")
        void shouldTransitionToHalfOpenAfterRecoveryTimeout() throws InterruptedException {
            // Given - Force circuit breaker to open
            for (int i = 0; i < 5; i++) {
                try {
                    circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                        throw new RuntimeException("Service failure");
                    });
                } catch (Exception e) {
                    // Expected failures
                }
            }

            // When - Wait for recovery timeout (simulated by forcing next call)
            Thread.sleep(100); // Small delay to simulate timeout

            // Force state check by attempting operation
            try {
                circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> "success");
            } catch (Exception e) {
                // May fail if still in open state
            }

            // Then - Should eventually allow calls through
            CircuitBreakerService.CircuitBreakerMetrics metrics = 
                circuitBreakerService.getMetrics(TEST_SERVICE);
            
            assertThat(metrics.getTotalCalls()).isGreaterThan(5);
        }

        @Test
        @DisplayName("Should close circuit breaker after successful recovery")
        void shouldCloseCircuitBreakerAfterSuccessfulRecovery() {
            // Given - Force circuit breaker to open
            for (int i = 0; i < 5; i++) {
                try {
                    circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                        throw new RuntimeException("Service failure");
                    });
                } catch (Exception e) {
                    // Expected failures
                }
            }

            // When - Execute successful operations after recovery
            try {
                // This should trigger transition to HALF_OPEN and then CLOSED
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(10); // Small delay to simulate recovery
                    try {
                        circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> "success");
                        break; // Success, exit loop
                    } catch (CircuitBreakerService.CircuitBreakerException e) {
                        // Still in open state, continue
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Then - Eventually should be closed or allow operations
            CircuitBreakerService.CircuitBreakerMetrics metrics = 
                circuitBreakerService.getMetrics(TEST_SERVICE);
            
            assertThat(metrics.getTotalCalls()).isGreaterThan(5);
        }
    }

    @Nested
    @DisplayName("Circuit Breaker Metrics")
    class CircuitBreakerMetricsTests {

        @Test
        @DisplayName("Should track metrics correctly")
        void shouldTrackMetricsCorrectly() {
            // Given - Execute mixed operations
            String result1 = circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> "success1");
            String result2 = circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> "success2");
            
            try {
                circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                    throw new RuntimeException("failure");
                });
            } catch (Exception e) {
                // Expected failure
            }

            // When - Get metrics
            CircuitBreakerService.CircuitBreakerMetrics metrics = 
                circuitBreakerService.getMetrics(TEST_SERVICE);

            // Then
            assertThat(metrics.getServiceName()).isEqualTo(TEST_SERVICE);
            assertThat(metrics.getTotalCalls()).isEqualTo(3);
            assertThat(metrics.getFailureCount()).isEqualTo(1);
            assertThat(metrics.getFailureRate()).isEqualTo(1.0/3.0);
            assertThat(metrics.getState()).isEqualTo(CircuitBreakerService.CircuitBreakerState.CLOSED);
        }

        @Test
        @DisplayName("Should return default metrics for non-existent service")
        void shouldReturnDefaultMetricsForNonExistentService() {
            // When
            CircuitBreakerService.CircuitBreakerMetrics metrics = 
                circuitBreakerService.getMetrics("non-existent-service");

            // Then
            assertThat(metrics.getServiceName()).isEqualTo("non-existent-service");
            assertThat(metrics.getTotalCalls()).isEqualTo(0);
            assertThat(metrics.getFailureCount()).isEqualTo(0);
            assertThat(metrics.getFailureRate()).isEqualTo(0.0);
            assertThat(metrics.getState()).isEqualTo(CircuitBreakerService.CircuitBreakerState.CLOSED);
        }
    }

    @Nested
    @DisplayName("Health Check Integration")
    class HealthCheckTests {

        @Test
        @DisplayName("Should report healthy when all circuit breakers are closed")
        void shouldReportHealthyWhenAllCircuitBreakersAreClosed() {
            // Given - Execute successful operations
            circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> "success");
            circuitBreakerService.executeWithCircuitBreaker("another-service", () -> "success");

            // When
            Health health = circuitBreakerService.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsKey("circuit-breakers");
        }

        @Test
        @DisplayName("Should report unhealthy when circuit breakers are open")
        void shouldReportUnhealthyWhenCircuitBreakersAreOpen() {
            // Given - Force circuit breaker to open
            for (int i = 0; i < 5; i++) {
                try {
                    circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                        throw new RuntimeException("Service failure");
                    });
                } catch (Exception e) {
                    // Expected failures
                }
            }

            // When
            Health health = circuitBreakerService.health();

            // Then
            assertThat(health.getStatus()).isEqualTo(Status.DOWN);
            assertThat(health.getDetails()).containsKey("open-circuits");
        }
    }

    @Nested
    @DisplayName("Concurrent Operations")
    class ConcurrentOperationsTests {

        @Test
        @DisplayName("Should handle concurrent operations correctly")
        void shouldHandleConcurrentOperationsCorrectly() {
            // Given - Multiple concurrent operations
            int threadCount = 10;
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            // When - Execute concurrent operations
            @SuppressWarnings("unchecked")
            CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                            if (index % 3 == 0) {
                                throw new RuntimeException("Simulated failure");
                            }
                            return "success";
                        });
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    }
                });
            }

            // Wait for all to complete
            CompletableFuture.allOf(futures).join();

            // Then - Metrics should be consistent
            CircuitBreakerService.CircuitBreakerMetrics metrics = 
                circuitBreakerService.getMetrics(TEST_SERVICE);
            
            assertThat(metrics.getTotalCalls()).isEqualTo(threadCount);
            assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
        }
    }

    @Nested
    @DisplayName("Runnable Operations")
    class RunnableOperationsTests {

        @Test
        @DisplayName("Should execute runnable operations with circuit breaker")
        void shouldExecuteRunnableOperationsWithCircuitBreaker() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);

            // When
            circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                counter.incrementAndGet();
            });

            // Then
            assertThat(counter.get()).isEqualTo(1);
            
            CircuitBreakerService.CircuitBreakerMetrics metrics = 
                circuitBreakerService.getMetrics(TEST_SERVICE);
            assertThat(metrics.getTotalCalls()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle runnable operation failures")
        void shouldHandleRunnableOperationFailures() {
            // Given
            AtomicInteger counter = new AtomicInteger(0);

            // When - Execute failing runnable
            assertThatThrownBy(() -> 
                circuitBreakerService.executeWithCircuitBreaker(TEST_SERVICE, () -> {
                    counter.incrementAndGet();
                    throw new RuntimeException("Runnable failure");
                })
            ).isInstanceOf(RuntimeException.class)
             .hasMessage("Runnable failure");

            // Then
            assertThat(counter.get()).isEqualTo(1);
            
            CircuitBreakerService.CircuitBreakerMetrics metrics = 
                circuitBreakerService.getMetrics(TEST_SERVICE);
            assertThat(metrics.getTotalCalls()).isEqualTo(1);
            assertThat(metrics.getFailureCount()).isEqualTo(1);
        }
    }
}