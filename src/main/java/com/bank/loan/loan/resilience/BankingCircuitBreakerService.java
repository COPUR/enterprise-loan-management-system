package com.bank.loanmanagement.loan.resilience;

import com.bank.loanmanagement.loan.exception.BankingExceptionCatalogue;
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
// Using individual decorator patterns instead of generic Decorators class
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Comprehensive circuit breaker service for banking operations
 * Implements adaptive patterns with AI-powered threshold adjustments
 */
@Service
@RequiredArgsConstructor
public class BankingCircuitBreakerService {

    private static final Logger log = LoggerFactory.getLogger(BankingCircuitBreakerService.class);
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;
    private final BankingExceptionCatalogue exceptionCatalogue;
    private final ChatClient chatClient;

    // Circuit Breaker Configurations for different banking services
    
    /**
     * Get circuit breaker for loan operations
     */
    public CircuitBreaker getLoanServiceCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("loan-service", 
            CircuitBreakerConfig.custom()
                .failureRateThreshold(60) // 60% failure rate
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallRateThreshold(80) // 80% slow calls
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .recordExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build());
    }

    /**
     * Get circuit breaker for payment operations (stricter)
     */
    public CircuitBreaker getPaymentServiceCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("payment-service",
            CircuitBreakerConfig.custom()
                .failureRateThreshold(40) // Lower threshold for critical payments
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .permittedNumberOfCallsInHalfOpenState(2)
                .slowCallRateThreshold(70)
                .slowCallDurationThreshold(Duration.ofSeconds(1))
                .recordExceptions(Exception.class)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build());
    }

    /**
     * Get circuit breaker for AI services (more tolerant)
     */
    public CircuitBreaker getAIServiceCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("ai-service",
            CircuitBreakerConfig.custom()
                .failureRateThreshold(75) // Higher tolerance for AI services
                .waitDurationInOpenState(Duration.ofSeconds(45))
                .slidingWindowSize(15)
                .minimumNumberOfCalls(8)
                .permittedNumberOfCallsInHalfOpenState(5)
                .slowCallRateThreshold(90)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
                .recordExceptions(Exception.class, TimeoutException.class)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build());
    }

    /**
     * Get circuit breaker for external services
     */
    public CircuitBreaker getExternalServiceCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("external-service",
            CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(2))
                .slidingWindowSize(25)
                .minimumNumberOfCalls(15)
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallRateThreshold(85)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .recordExceptions(Exception.class)
                .enableAutomaticTransitionFromOpenToHalfOpen()
                .build());
    }

    /**
     * Get retry configuration for banking operations
     */
    public Retry getBankingRetry(String serviceName) {
        return retryRegistry.retry(serviceName + "-retry",
            RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryOnException(throwable -> !(throwable instanceof IllegalArgumentException))
                .build());
    }

    /**
     * Get bulkhead for service isolation
     */
    public Bulkhead getBankingBulkhead(String serviceName, int maxConcurrency) {
        return bulkheadRegistry.bulkhead(serviceName + "-bulkhead",
            BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrency)
                .maxWaitDuration(Duration.ofSeconds(10))
                .writableStackTraceEnabled(true)
                .build());
    }

    /**
     * Get time limiter for operations
     */
    public TimeLimiter getBankingTimeLimiter(String serviceName, Duration timeout) {
        return timeLimiterRegistry.timeLimiter(serviceName + "-timelimiter",
            TimeLimiterConfig.custom()
                .timeoutDuration(timeout)
                .cancelRunningFuture(true)
                .build());
    }

    /**
     * Execute loan operation with circuit breaker protection
     */
    public <T> T executeLoanOperation(Supplier<T> operation, String operationName) {
        CircuitBreaker circuitBreaker = getLoanServiceCircuitBreaker();
        Retry retry = getBankingRetry("loan-service");
        
        Supplier<T> decoratedSupplier = CircuitBreaker
            .decorateSupplier(circuitBreaker, operation);
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        
        try {
            log.info("Executing loan operation: {}", operationName);
            T result = decoratedSupplier.get();
            log.info("Loan operation completed successfully: {}", operationName);
            return result;
        } catch (Exception e) {
            log.error("Loan operation failed: {} - {}", operationName, e.getMessage());
            handleCircuitBreakerFailure("loan-service", operationName, e);
            throw e;
        }
    }

    /**
     * Execute payment operation with strict circuit breaker protection
     */
    public <T> T executePaymentOperation(Supplier<T> operation, String operationName) {
        CircuitBreaker circuitBreaker = getPaymentServiceCircuitBreaker();
        Retry retry = getBankingRetry("payment-service");
        Bulkhead bulkhead = getBankingBulkhead("payment-service", 10);
        TimeLimiter timeLimiter = getBankingTimeLimiter("payment-service", Duration.ofSeconds(30));
        
        Supplier<CompletableFuture<T>> futureSupplier = () -> CompletableFuture.supplyAsync(operation);
        
        // Apply decorators using individual resilience patterns
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, operation);
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        decoratedSupplier = Bulkhead.decorateSupplier(bulkhead, decoratedSupplier);
        
        try {
            log.info("Executing payment operation: {}", operationName);
            T result = decoratedSupplier.get();
            log.info("Payment operation completed successfully: {}", operationName);
            return result;
        } catch (Exception e) {
            log.error("Payment operation failed: {} - {}", operationName, e.getMessage());
            handleCircuitBreakerFailure("payment-service", operationName, e);
            throw e;
        }
    }

    /**
     * Execute AI operation with tolerant circuit breaker
     */
    public <T> T executeAIOperation(Supplier<T> operation, String operationName) {
        CircuitBreaker circuitBreaker = getAIServiceCircuitBreaker();
        Retry retry = getBankingRetry("ai-service");
        TimeLimiter timeLimiter = getBankingTimeLimiter("ai-service", Duration.ofSeconds(15));
        
        // Apply decorators using individual resilience patterns
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, operation);
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        
        try {
            log.info("Executing AI operation: {}", operationName);
            T result = decoratedSupplier.get();
            log.info("AI operation completed successfully: {}", operationName);
            return result;
        } catch (Exception e) {
            log.error("AI operation failed: {} - {}", operationName, e.getMessage());
            handleCircuitBreakerFailure("ai-service", operationName, e);
            
            // Attempt AI-powered fallback strategy
            return executeAIFallbackStrategy(operation, operationName, e);
        }
    }

    /**
     * Execute external service operation with circuit breaker
     */
    public <T> T executeExternalServiceOperation(Supplier<T> operation, String serviceName, String operationName) {
        CircuitBreaker circuitBreaker = getExternalServiceCircuitBreaker();
        Retry retry = getBankingRetry("external-service");
        
        Supplier<T> decoratedSupplier = CircuitBreaker
            .decorateSupplier(circuitBreaker, operation);
        decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        
        try {
            log.info("Executing external service operation: {} - {}", serviceName, operationName);
            T result = decoratedSupplier.get();
            log.info("External service operation completed: {} - {}", serviceName, operationName);
            return result;
        } catch (Exception e) {
            log.error("External service operation failed: {} - {} - {}", serviceName, operationName, e.getMessage());
            handleCircuitBreakerFailure(serviceName, operationName, e);
            throw e;
        }
    }

    /**
     * AI-powered fallback strategy when circuit breaker opens
     */
    private <T> T executeAIFallbackStrategy(Supplier<T> originalOperation, String operationName, Exception originalException) {
        try {
            log.info("Executing AI-powered fallback strategy for: {}", operationName);
            
            // Use AI to determine best fallback approach
            String fallbackAdvice = chatClient.prompt()
                .user("Banking operation '" + operationName + "' failed with error: " + originalException.getMessage() + 
                      ". Suggest the best fallback strategy for this banking operation.")
                .call()
                .content();
            
            log.info("AI fallback strategy: {}", fallbackAdvice);
            
            // Apply simplified fallback based on operation type
            if (operationName.contains("fraud")) {
                // For fraud detection, default to cautious approach
                return (T) Boolean.TRUE; // Flag for manual review
            } else if (operationName.contains("recommendation")) {
                // For recommendations, return empty list
                return (T) java.util.Collections.emptyList();
            }
            
            // For other operations, try one more time with simplified parameters
            return originalOperation.get();
            
        } catch (Exception e) {
            log.error("AI fallback strategy also failed for: {}", operationName, e);
            throw new RuntimeException("Both primary operation and AI fallback failed", e);
        }
    }

    /**
     * Handle circuit breaker failure with intelligent alerting
     */
    private void handleCircuitBreakerFailure(String serviceName, String operationName, Exception exception) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        
        log.warn("Circuit breaker metrics for {}: State={}, Failure Rate={}, Slow Call Rate={}", 
            serviceName, 
            circuitBreaker.getState(),
            circuitBreaker.getMetrics().getFailureRate(),
            circuitBreaker.getMetrics().getSlowCallRate());
        
        // Use AI to analyze the failure pattern and suggest actions
        if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
            analyzeFailurePattern(serviceName, operationName, exception);
        }
    }

    /**
     * AI-powered failure pattern analysis
     */
    private void analyzeFailurePattern(String serviceName, String operationName, Exception exception) {
        try {
            String analysisPrompt = String.format(
                "Analyze this banking service failure: Service=%s, Operation=%s, Error=%s. " +
                "Provide recommendations for: 1) Immediate actions, 2) Circuit breaker tuning, " +
                "3) Service recovery strategies, 4) Prevention measures.",
                serviceName, operationName, exception.getMessage());
            
            String analysis = chatClient.prompt()
                .user(analysisPrompt)
                .call()
                .content();
            
            log.info("AI failure analysis for {}: {}", serviceName, analysis);
            
            // Could trigger alerts, auto-scaling, or other recovery mechanisms
            triggerIntelligentRecovery(serviceName, analysis);
            
        } catch (Exception e) {
            log.error("Failed to perform AI failure analysis for service: {}", serviceName, e);
        }
    }

    /**
     * Trigger intelligent recovery based on AI analysis
     */
    private void triggerIntelligentRecovery(String serviceName, String analysis) {
        // This could integrate with:
        // - Kubernetes auto-scaling
        // - Load balancer reconfiguration
        // - Alert management systems
        // - Capacity planning systems
        
        log.info("Triggering intelligent recovery for service: {} based on analysis: {}", serviceName, analysis);
        
        // Example: Adjust circuit breaker thresholds based on AI recommendations
        if (analysis.toLowerCase().contains("increase tolerance")) {
            adjustCircuitBreakerThresholds(serviceName, 1.2); // Increase thresholds by 20%
        } else if (analysis.toLowerCase().contains("stricter")) {
            adjustCircuitBreakerThresholds(serviceName, 0.8); // Decrease thresholds by 20%
        }
    }

    /**
     * Dynamically adjust circuit breaker thresholds
     */
    private void adjustCircuitBreakerThresholds(String serviceName, double adjustmentFactor) {
        log.info("Adjusting circuit breaker thresholds for {} by factor: {}", serviceName, adjustmentFactor);
        
        // This would require recreating the circuit breaker with new configuration
        // In production, this could be implemented with dynamic configuration management
        
        // For now, log the recommendation
        log.info("Recommended threshold adjustment for {}: factor={}", serviceName, adjustmentFactor);
    }

    /**
     * Get circuit breaker health status for monitoring
     */
    public String getCircuitBreakerHealthStatus() {
        StringBuilder status = new StringBuilder();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            status.append(String.format("Service: %s, State: %s, Failure Rate: %.2f%%, Slow Call Rate: %.2f%%\n",
                cb.getName(),
                cb.getState(),
                cb.getMetrics().getFailureRate(),
                cb.getMetrics().getSlowCallRate()));
        });
        
        return status.toString();
    }
}