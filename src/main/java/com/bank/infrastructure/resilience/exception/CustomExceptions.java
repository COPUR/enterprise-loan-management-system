package com.bank.infrastructure.resilience.exception;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Custom exceptions for the banking system with rich context information
 */

// Business Logic Exceptions

@Getter
public class CustomerNotFoundException extends RuntimeException {
    private final String customerId;
    
    public CustomerNotFoundException(String customerId) {
        super("Customer not found with ID: " + customerId);
        this.customerId = customerId;
    }
}

@Getter
public class InsufficientFundsException extends RuntimeException {
    private final String accountId;
    private final BigDecimal requiredAmount;
    private final BigDecimal availableAmount;
    
    public InsufficientFundsException(String accountId, BigDecimal requiredAmount, BigDecimal availableAmount) {
        super(String.format("Insufficient funds in account %s. Required: %s, Available: %s", 
                accountId, requiredAmount, availableAmount));
        this.accountId = accountId;
        this.requiredAmount = requiredAmount;
        this.availableAmount = availableAmount;
    }
}

@Getter
public class FraudDetectedException extends RuntimeException {
    private final String transactionId;
    private final double riskScore;
    private final String reason;
    
    public FraudDetectedException(String transactionId, double riskScore, String reason) {
        super(String.format("Fraud detected for transaction %s. Risk score: %.2f. Reason: %s", 
                transactionId, riskScore, reason));
        this.transactionId = transactionId;
        this.riskScore = riskScore;
        this.reason = reason;
    }
}

@Getter
public class LoanNotEligibleException extends RuntimeException {
    private final String customerId;
    private final String reason;
    private final double creditScore;
    
    public LoanNotEligibleException(String customerId, String reason, double creditScore) {
        super(String.format("Customer %s not eligible for loan. Credit score: %.0f. Reason: %s", 
                customerId, creditScore, reason));
        this.customerId = customerId;
        this.reason = reason;
        this.creditScore = creditScore;
    }
}

// External Service Exceptions

@Getter
public class ExternalServiceException extends RuntimeException {
    private final String serviceName;
    private final int statusCode;
    private final String errorCode;
    private final int retryAfter;
    
    public ExternalServiceException(String serviceName, int statusCode, String errorCode, String message) {
        super(String.format("External service error - %s: %s (Status: %d, Code: %s)", 
                serviceName, message, statusCode, errorCode));
        this.serviceName = serviceName;
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.retryAfter = calculateRetryAfter(statusCode);
    }
    
    public boolean isRetryable() {
        return statusCode >= 500 || statusCode == 429 || statusCode == 0;
    }
    
    private int calculateRetryAfter(int statusCode) {
        return switch (statusCode) {
            case 429 -> 60; // Rate limited - wait 1 minute
            case 503 -> 30; // Service unavailable - wait 30 seconds
            default -> 10;  // Default retry after 10 seconds
        };
    }
}

@Getter
public class CircuitBreakerOpenException extends RuntimeException {
    private final String serviceName;
    private final Instant estimatedRecoveryTime;
    private final boolean fallbackAvailable;
    private final int failureCount;
    
    public CircuitBreakerOpenException(String serviceName, Instant estimatedRecoveryTime, 
                                     boolean fallbackAvailable, int failureCount) {
        super(String.format("Circuit breaker OPEN for service: %s. Estimated recovery: %s", 
                serviceName, estimatedRecoveryTime));
        this.serviceName = serviceName;
        this.estimatedRecoveryTime = estimatedRecoveryTime;
        this.fallbackAvailable = fallbackAvailable;
        this.failureCount = failureCount;
    }
}

// Rate Limiting Exception

@Getter
public class RateLimitExceededException extends RuntimeException {
    private final String clientId;
    private final int limit;
    private final int windowMs;
    private final long resetTime;
    private final long retryAfterMs;
    
    public RateLimitExceededException(String clientId, int limit, int windowMs, long resetTime) {
        super(String.format("Rate limit exceeded for client %s. Limit: %d per %dms", 
                clientId, limit, windowMs));
        this.clientId = clientId;
        this.limit = limit;
        this.windowMs = windowMs;
        this.resetTime = resetTime;
        this.retryAfterMs = resetTime - System.currentTimeMillis();
    }
}

// Database Exceptions

@Getter
public class OptimisticLockException extends RuntimeException {
    private final String entityType;
    private final String entityId;
    private final int expectedVersion;
    private final int actualVersion;
    
    public OptimisticLockException(String entityType, String entityId, 
                                   int expectedVersion, int actualVersion) {
        super(String.format("Optimistic lock failure for %s with ID %s. Expected version: %d, Actual: %d", 
                entityType, entityId, expectedVersion, actualVersion));
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
}

@Getter
public class DatabaseException extends RuntimeException {
    private final String operation;
    private final String table;
    private final boolean isRetryable;
    
    public DatabaseException(String operation, String table, String message, boolean isRetryable) {
        super(String.format("Database error during %s on %s: %s", operation, table, message));
        this.operation = operation;
        this.table = table;
        this.isRetryable = isRetryable;
    }
}

// Network Exception

@Getter
public class NetworkException extends RuntimeException {
    private final String host;
    private final int port;
    private final long latencyMs;
    
    public NetworkException(String host, int port, String message, long latencyMs) {
        super(String.format("Network error connecting to %s:%d (latency: %dms): %s", 
                host, port, latencyMs, message));
        this.host = host;
        this.port = port;
        this.latencyMs = latencyMs;
    }
}

// Validation Exceptions

public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}

public class ClientException extends RuntimeException {
    public ClientException(String message) {
        super(message);
    }
}

// Authentication/Authorization

public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}

public class AuthorizationException extends RuntimeException {
    private final String requiredPermission;
    
    public AuthorizationException(String message, String requiredPermission) {
        super(message);
        this.requiredPermission = requiredPermission;
    }
    
    public String getRequiredPermission() {
        return requiredPermission;
    }
}

// Service Availability

@Getter
public class ServiceUnavailableException extends RuntimeException {
    private final String serviceName;
    private final String fallbackMessage;
    
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.serviceName = extractServiceName(message);
        this.fallbackMessage = "Service temporarily unavailable. Please try again later.";
    }
    
    private String extractServiceName(String message) {
        // Extract service name from message pattern
        if (message.contains("Service ") && message.contains(" is unavailable")) {
            return message.substring(message.indexOf("Service ") + 8, 
                                   message.indexOf(" is unavailable"));
        }
        return "Unknown";
    }
}

// Retryable/Non-retryable markers

public class RetryableException extends RuntimeException {
    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class NonRetryableException extends RuntimeException {
    public NonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}