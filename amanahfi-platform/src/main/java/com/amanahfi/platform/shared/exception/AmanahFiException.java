package com.amanahfi.platform.shared.exception;

import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base exception for all AmanahFi platform exceptions
 * Supports internationalization and comprehensive error context
 */
@Getter
public abstract class AmanahFiException extends RuntimeException {
    
    private final String errorCode;
    private final String correlationId;
    private final Instant timestamp;
    private final ErrorSeverity severity;
    private final ErrorCategory category;
    private final Map<String, Object> errorContext;
    private final String userMessage;
    private final String technicalMessage;
    
    protected AmanahFiException(
            String errorCode,
            String userMessage,
            String technicalMessage,
            ErrorSeverity severity,
            ErrorCategory category,
            Map<String, Object> errorContext,
            Throwable cause) {
        
        super(technicalMessage, cause);
        
        this.errorCode = errorCode;
        this.userMessage = userMessage;
        this.technicalMessage = technicalMessage;
        this.severity = severity;
        this.category = category;
        this.errorContext = errorContext != null ? Map.copyOf(errorContext) : Map.of();
        this.correlationId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
        
        validate();
    }
    
    protected AmanahFiException(
            String errorCode,
            String userMessage,
            String technicalMessage,
            ErrorSeverity severity,
            ErrorCategory category,
            Map<String, Object> errorContext) {
        
        this(errorCode, userMessage, technicalMessage, severity, category, errorContext, null);
    }
    
    protected AmanahFiException(
            String errorCode,
            String userMessage,
            String technicalMessage,
            ErrorSeverity severity,
            ErrorCategory category) {
        
        this(errorCode, userMessage, technicalMessage, severity, category, Map.of(), null);
    }
    
    private void validate() {
        if (errorCode == null || errorCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Error code cannot be null or empty");
        }
        
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message cannot be null or empty");
        }
        
        if (technicalMessage == null || technicalMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Technical message cannot be null or empty");
        }
        
        if (severity == null) {
            throw new IllegalArgumentException("Error severity cannot be null");
        }
        
        if (category == null) {
            throw new IllegalArgumentException("Error category cannot be null");
        }
    }
    
    /**
     * Get localized user message
     */
    public abstract String getLocalizedMessage(String languageCode);
    
    /**
     * Get error details for API response
     */
    public ErrorDetails getErrorDetails() {
        return ErrorDetails.builder()
            .errorCode(errorCode)
            .userMessage(userMessage)
            .technicalMessage(technicalMessage)
            .severity(severity)
            .category(category)
            .correlationId(correlationId)
            .timestamp(timestamp)
            .errorContext(errorContext)
            .build();
    }
    
    /**
     * Check if error is retryable
     */
    public boolean isRetryable() {
        return category == ErrorCategory.INFRASTRUCTURE || 
               category == ErrorCategory.EXTERNAL_SERVICE ||
               (category == ErrorCategory.SECURITY && severity != ErrorSeverity.CRITICAL);
    }
    
    /**
     * Check if error requires user notification
     */
    public boolean requiresUserNotification() {
        return severity == ErrorSeverity.HIGH || severity == ErrorSeverity.CRITICAL;
    }
    
    /**
     * Check if error requires admin notification
     */
    public boolean requiresAdminNotification() {
        return severity == ErrorSeverity.CRITICAL || 
               (category == ErrorCategory.SECURITY && severity == ErrorSeverity.HIGH);
    }
    
    /**
     * Get error context value
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key, Class<T> type) {
        Object value = errorContext.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Error severity levels
     */
    public enum ErrorSeverity {
        LOW,      // Minor issues, graceful degradation
        MEDIUM,   // Functionality impacted but system stable
        HIGH,     // Significant functionality affected
        CRITICAL  // System integrity or security compromised
    }
    
    /**
     * Error categories for classification
     */
    public enum ErrorCategory {
        BUSINESS,           // Business rule violations
        VALIDATION,         // Data validation errors
        SECURITY,          // Security-related errors
        AUTHENTICATION,    // Authentication failures
        AUTHORIZATION,     // Authorization failures
        INFRASTRUCTURE,    // Infrastructure/system errors
        EXTERNAL_SERVICE,  // External service errors
        CONFIGURATION,     // Configuration errors
        DATA_INTEGRITY,    // Data consistency errors
        COMPLIANCE,        // Regulatory compliance errors
        ISLAMIC_FINANCE,   // Islamic finance specific errors
        CBDC              // CBDC specific errors
    }
    
    /**
     * Error details for API responses
     */
    @lombok.Value
    @lombok.Builder
    public static class ErrorDetails {
        String errorCode;
        String userMessage;
        String technicalMessage;
        ErrorSeverity severity;
        ErrorCategory category;
        String correlationId;
        Instant timestamp;
        Map<String, Object> errorContext;
    }
}