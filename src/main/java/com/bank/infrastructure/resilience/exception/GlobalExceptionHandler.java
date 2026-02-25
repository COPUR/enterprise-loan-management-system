package com.bank.infrastructure.resilience.exception;

import com.bank.infrastructure.resilience.monitoring.ExceptionMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * Global exception handler that provides consistent error responses across the application.
 * Features:
 * - Structured error responses using RFC 7807 Problem Details
 * - Correlation IDs for distributed tracing
 * - Exception metrics collection
 * - Sensitive data masking
 * - Client-friendly error messages
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MeterRegistry meterRegistry;
    private final ExceptionMetrics exceptionMetrics;
    
    private static final String ERROR_URI_BASE = "https://api.enterprise-banking.com/errors/";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    // Business Logic Exceptions
    
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCustomerNotFound(
            CustomerNotFoundException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.error("Customer not found. CorrelationId: {}, CustomerId: {}", 
                correlationId, ex.getCustomerId());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, 
                "Customer not found"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "customer-not-found"));
        problemDetail.setTitle("Customer Not Found");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("customerId", ex.getCustomerId());
        
        exceptionMetrics.recordException("customer_not_found", ex);
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }
    
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ProblemDetail> handleInsufficientFunds(
            InsufficientFundsException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.warn("Insufficient funds. CorrelationId: {}, AccountId: {}, Required: {}, Available: {}", 
                correlationId, ex.getAccountId(), ex.getRequiredAmount(), ex.getAvailableAmount());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Insufficient funds to complete the transaction"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "insufficient-funds"));
        problemDetail.setTitle("Insufficient Funds");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        // Don't expose exact amounts to prevent information leakage
        problemDetail.setProperty("accountId", maskAccountId(ex.getAccountId()));
        
        exceptionMetrics.recordException("insufficient_funds", ex);
        
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }
    
    @ExceptionHandler(FraudDetectedException.class)
    public ResponseEntity<ProblemDetail> handleFraudDetected(
            FraudDetectedException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.error("FRAUD DETECTED! CorrelationId: {}, TransactionId: {}, RiskScore: {}", 
                correlationId, ex.getTransactionId(), ex.getRiskScore());
        
        // Alert security team immediately
        exceptionMetrics.alertSecurityTeam(ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Transaction blocked for security reasons"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "security-block"));
        problemDetail.setTitle("Security Block");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("referenceNumber", generateReferenceNumber());
        problemDetail.setProperty("supportMessage", 
                "Please contact customer support with reference number");
        
        exceptionMetrics.recordException("fraud_detected", ex);
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }
    
    // Validation Exceptions
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.warn("Validation failed. CorrelationId: {}", correlationId);
        
        Map<String, String> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation failed"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "validation-failed"));
        problemDetail.setTitle("Validation Failed");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("validationErrors", validationErrors);
        
        exceptionMetrics.recordException("validation_failed", ex);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.warn("Constraint violation. CorrelationId: {}", correlationId);
        
        Map<String, String> violations = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            violations.put(
                    violation.getPropertyPath().toString(),
                    violation.getMessage()
            );
        });
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Constraint violation"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "constraint-violation"));
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("violations", violations);
        
        exceptionMetrics.recordException("constraint_violation", ex);
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }
    
    // External Service Exceptions
    
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ProblemDetail> handleExternalServiceException(
            ExternalServiceException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.error("External service failure. CorrelationId: {}, Service: {}, Error: {}", 
                correlationId, ex.getServiceName(), ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "External service temporarily unavailable"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "external-service-error"));
        problemDetail.setTitle("Service Temporarily Unavailable");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("retryAfter", ex.getRetryAfter());
        problemDetail.setProperty("alternativeAction", 
                "Please try again later or contact support");
        
        exceptionMetrics.recordException("external_service_error", ex);
        
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(CORRELATION_ID_HEADER, correlationId)
                .header("Retry-After", String.valueOf(ex.getRetryAfter()))
                .body(problemDetail);
    }
    
    @ExceptionHandler(CircuitBreakerOpenException.class)
    public ResponseEntity<ProblemDetail> handleCircuitBreakerOpen(
            CircuitBreakerOpenException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.error("Circuit breaker OPEN. CorrelationId: {}, Service: {}", 
                correlationId, ex.getServiceName());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service temporarily suspended due to high error rate"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "circuit-breaker-open"));
        problemDetail.setTitle("Service Circuit Breaker Open");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("estimatedRecoveryTime", ex.getEstimatedRecoveryTime());
        problemDetail.setProperty("fallbackAvailable", ex.isFallbackAvailable());
        
        if (ex.isFallbackAvailable()) {
            problemDetail.setProperty("fallbackMessage", 
                    "Limited functionality is available. Some features may be restricted.");
        }
        
        exceptionMetrics.recordException("circuit_breaker_open", ex);
        
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }
    
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ProblemDetail> handleTimeout(
            TimeoutException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.error("Operation timeout. CorrelationId: {}", correlationId);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.GATEWAY_TIMEOUT,
                "Operation timed out"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "timeout"));
        problemDetail.setTitle("Operation Timeout");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("suggestion", 
                "The operation is taking longer than expected. Please try again.");
        
        exceptionMetrics.recordException("timeout", ex);
        
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }
    
    // Rate Limiting
    
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(
            RateLimitExceededException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.warn("Rate limit exceeded. CorrelationId: {}, ClientId: {}", 
                correlationId, ex.getClientId());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Rate limit exceeded"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "rate-limit-exceeded"));
        problemDetail.setTitle("Too Many Requests");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("limit", ex.getLimit());
        problemDetail.setProperty("windowMs", ex.getWindowMs());
        problemDetail.setProperty("retryAfterMs", ex.getRetryAfterMs());
        
        exceptionMetrics.recordException("rate_limit_exceeded", ex);
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(CORRELATION_ID_HEADER, correlationId)
                .header("X-RateLimit-Limit", String.valueOf(ex.getLimit()))
                .header("X-RateLimit-Remaining", "0")
                .header("X-RateLimit-Reset", String.valueOf(ex.getResetTime()))
                .header("Retry-After", String.valueOf(ex.getRetryAfterMs() / 1000))
                .body(problemDetail);
    }
    
    // Database Exceptions
    
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(
            OptimisticLockException ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        log.warn("Optimistic lock conflict. CorrelationId: {}, Entity: {}", 
                correlationId, ex.getEntityType());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The resource was modified by another user"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "concurrent-modification"));
        problemDetail.setTitle("Concurrent Modification");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("suggestion", 
                "Please refresh and try again with the latest data");
        
        exceptionMetrics.recordException("optimistic_lock", ex);
        
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }
    
    // Generic Exception Handler (Catch-all)
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, WebRequest request) {
        
        String correlationId = getOrCreateCorrelationId(request);
        String errorId = UUID.randomUUID().toString();
        
        // Log full exception details for debugging
        log.error("Unexpected error. CorrelationId: {}, ErrorId: {}", 
                correlationId, errorId, ex);
        
        // Send alert for unexpected errors
        exceptionMetrics.alertOnUnexpectedError(errorId, ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred"
        );
        
        problemDetail.setType(URI.create(ERROR_URI_BASE + "internal-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("correlationId", correlationId);
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errorId", errorId);
        problemDetail.setProperty("message", 
                "An unexpected error occurred. Please contact support with the error ID.");
        
        // In development environment, include stack trace
        if (isDevelopmentEnvironment()) {
            problemDetail.setProperty("debug", Map.of(
                    "exception", ex.getClass().getSimpleName(),
                    "message", ex.getMessage()
            ));
        }
        
        exceptionMetrics.recordException("unexpected_error", ex);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(problemDetail);
    }
    
    // Helper Methods
    
    private String getOrCreateCorrelationId(WebRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }
    
    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 8) {
            return "****";
        }
        return accountId.substring(0, 4) + "****" + accountId.substring(accountId.length() - 4);
    }
    
    private String generateReferenceNumber() {
        return "REF-" + System.currentTimeMillis() + "-" + 
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private boolean isDevelopmentEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("dev") || profile.contains("local");
    }
}