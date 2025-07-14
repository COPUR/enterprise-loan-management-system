package com.bank.infrastructure.error;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import com.bank.shared.domain.exception.BusinessException;
import com.bank.shared.domain.exception.InsufficientCreditException;
import com.bank.shared.domain.exception.FraudDetectedException;
import com.bank.shared.domain.exception.ComplianceViolationException;
import com.bank.shared.domain.exception.InvalidAccountException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.TimeoutException;

/**
 * Global Exception Handler for Banking Platform
 * 
 * Provides comprehensive error handling following RFC 9457 Problem Details:
 * - Business logic exceptions with proper HTTP status codes
 * - Validation errors with detailed field-level feedback
 * - Security exceptions with appropriate responses
 * - Resilience pattern exceptions (circuit breaker, bulkhead, rate limiter)
 * - Infrastructure exceptions with proper logging
 * - FAPI-compliant error responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private final ObjectMapper objectMapper;
    
    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handle business validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request validation failed"
        );
        
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/validation"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        
        // Add field-level validation errors
        List<Map<String, String>> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                "rejectedValue", error.getRejectedValue() != null ? error.getRejectedValue().toString() : "null"
            ))
            .collect(Collectors.toList());
        
        problemDetail.setProperty("errors", fieldErrors);
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        return ResponseEntity.badRequest().body(problemDetail);
    }
    
    /**
     * Handle business logic exceptions
     */
    @ExceptionHandler({
        InsufficientCreditException.class
    })
    public ResponseEntity<ProblemDetail> handleInsufficientFundsExceptions(
            Exception ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.PAYMENT_REQUIRED,
            ex.getMessage()
        );
        
        problemDetail.setTitle("Insufficient Funds");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/insufficient-funds"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(problemDetail);
    }
    
    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler({
        IllegalArgumentException.class
    })
    public ResponseEntity<ProblemDetail> handleNotFoundExceptions(
            Exception ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/not-found"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        return ResponseEntity.notFound().build();
    }
    
    /**
     * Handle fraud detection exceptions
     */
    @ExceptionHandler(FraudDetectedException.class)
    public ResponseEntity<ProblemDetail> handleFraudDetectedException(
            FraudDetectedException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Transaction blocked due to fraud detection"
        );
        
        problemDetail.setTitle("Fraud Alert");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/fraud-detected"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("riskScore", ex.getRiskScore());
        problemDetail.setProperty("fraudReason", ex.getFraudReason());
        problemDetail.setProperty("transactionId", ex.getTransactionId());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        // Log fraud detection for compliance
        logSecurityEvent("FRAUD_DETECTED", ex.getMessage(), request);
        
        return ResponseEntity.unprocessableEntity().body(problemDetail);
    }
    
    /**
     * Handle compliance violation exceptions
     */
    @ExceptionHandler(ComplianceViolationException.class)
    public ResponseEntity<ProblemDetail> handleComplianceViolationException(
            ComplianceViolationException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,
            "Transaction rejected due to compliance violation"
        );
        
        problemDetail.setTitle("Compliance Violation");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/compliance-violation"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("violationType", ex.getViolationType());
        problemDetail.setProperty("complianceRule", ex.getComplianceRule());
        problemDetail.setProperty("entityId", ex.getEntityId());
        problemDetail.setProperty("severityLevel", ex.getSeverityLevel());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        // Log compliance violation for regulatory reporting
        logComplianceEvent("COMPLIANCE_VIOLATION", ex.getMessage(), request);
        
        return ResponseEntity.unprocessableEntity().body(problemDetail);
    }
    
    /**
     * Handle security exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "Access denied - insufficient permissions"
        );
        
        problemDetail.setTitle("Access Denied");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/access-denied"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        // Log security event
        logSecurityEvent("ACCESS_DENIED", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }
    
    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Authentication failed - invalid credentials"
        );
        
        problemDetail.setTitle("Authentication Failed");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/authentication-failed"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        // Log failed authentication attempt
        logSecurityEvent("AUTHENTICATION_FAILED", ex.getMessage(), request);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }
    
    /**
     * Handle circuit breaker exceptions
     */
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemDetail> handleCircuitBreakerException(
            CallNotPermittedException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Service temporarily unavailable due to high failure rate"
        );
        
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/service-unavailable"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("reason", "Circuit breaker open");
        problemDetail.setProperty("retryAfter", "30 seconds");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Retry-After", "30")
            .body(problemDetail);
    }
    
    /**
     * Handle bulkhead full exceptions
     */
    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<ProblemDetail> handleBulkheadFullException(
            BulkheadFullException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Service is at capacity, please retry later"
        );
        
        problemDetail.setTitle("Service Capacity Exceeded");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/capacity-exceeded"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("reason", "Bulkhead full");
        problemDetail.setProperty("retryAfter", "5 seconds");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", "5")
            .body(problemDetail);
    }
    
    /**
     * Handle rate limiter exceptions
     */
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ProblemDetail> handleRateLimiterException(
            RequestNotPermitted ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            "Rate limit exceeded, please slow down your requests"
        );
        
        problemDetail.setTitle("Rate Limit Exceeded");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/rate-limit-exceeded"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("reason", "Rate limit exceeded");
        problemDetail.setProperty("retryAfter", "60 seconds");
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", "60")
            .body(problemDetail);
    }
    
    /**
     * Handle timeout exceptions
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ProblemDetail> handleTimeoutException(
            TimeoutException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.REQUEST_TIMEOUT,
            "Request timed out, please try again"
        );
        
        problemDetail.setTitle("Request Timeout");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/timeout"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(problemDetail);
    }
    
    /**
     * Handle account validation exceptions
     */
    @ExceptionHandler(InvalidAccountException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAccountException(
            InvalidAccountException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY,
            ex.getMessage()
        );
        
        problemDetail.setTitle("Invalid Account");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/invalid-account"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("accountId", ex.getAccountId());
        problemDetail.setProperty("accountType", ex.getAccountType());
        problemDetail.setProperty("reason", ex.getReason());
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        return ResponseEntity.unprocessableEntity().body(problemDetail);
    }
    
    /**
     * Handle generic runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ProblemDetail> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred"
        );
        
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(java.net.URI.create("https://banking.local/errors/internal-error"));
        problemDetail.setInstance(java.net.URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now().toString());
        problemDetail.setProperty("errorId", UUID.randomUUID().toString());
        
        // Log the full exception for debugging (but don't expose to client)
        System.err.println("Unexpected error in request " + request.getRequestURI() + ": " + ex.getMessage());
        ex.printStackTrace();
        
        return ResponseEntity.internalServerError().body(problemDetail);
    }
    
    /**
     * Log security events for audit and monitoring
     */
    private void logSecurityEvent(String eventType, String message, HttpServletRequest request) {
        String clientIP = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        String requestId = request.getHeader("X-Request-ID");
        
        System.err.println(String.format(
            "SECURITY_EVENT: %s | IP: %s | URI: %s | UserAgent: %s | RequestId: %s | Message: %s",
            eventType, clientIP, request.getRequestURI(), userAgent, requestId, message
        ));
        
        // In production, this would integrate with your SIEM/security monitoring system
    }
    
    /**
     * Log compliance events for regulatory reporting
     */
    private void logComplianceEvent(String eventType, String message, HttpServletRequest request) {
        String clientIP = getClientIP(request);
        String requestId = request.getHeader("X-Request-ID");
        
        System.err.println(String.format(
            "COMPLIANCE_EVENT: %s | IP: %s | URI: %s | RequestId: %s | Message: %s",
            eventType, clientIP, request.getRequestURI(), requestId, message
        ));
        
        // In production, this would integrate with your compliance reporting system
    }
    
    /**
     * Extract client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String clientIP = request.getHeader("X-Forwarded-For");
        if (clientIP == null || clientIP.isEmpty()) {
            clientIP = request.getHeader("X-Real-IP");
        }
        if (clientIP == null || clientIP.isEmpty()) {
            clientIP = request.getRemoteAddr();
        }
        
        // Handle comma-separated IPs from X-Forwarded-For
        if (clientIP != null && clientIP.contains(",")) {
            clientIP = clientIP.split(",")[0].trim();
        }
        
        return clientIP;
    }
}