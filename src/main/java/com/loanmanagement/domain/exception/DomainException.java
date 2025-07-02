package com.loanmanagement.domain.exception;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for all domain exceptions following DDD principles.
 * Provides common functionality for business rule violations and domain errors.
 *
 * Supports 12-Factor App principles by:
 * - Structured error information for logging and monitoring
 * - Contextual metadata for observability
 * - Clear error categorization across application boundaries
 *
 * Follows clean code principles by:
 * - Single inheritance hierarchy for domain exceptions
 * - Rich context information for debugging
 * - Thread-safe operations for context management
 * - Comprehensive validation and error handling
 *
 * Implements hexagonal architecture by:
 * - Pure domain exception without infrastructure dependencies
 * - Can be used across all layers without coupling
 * - Provides structured data for different adapters
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;
    private final Instant occurredAt;
    private final Map<String, Object> context;
    private final String category;

    /**
     * Constructor with error code and message.
     * Follows clean code principles with clear parameter intent.
     */
    protected DomainException(String errorCode, String message) {
        super(validateMessage(message));
        this.errorCode = validateErrorCode(errorCode);
        this.occurredAt = Instant.now();
        this.context = new ConcurrentHashMap<>();
        this.category = determineCategory(errorCode);
    }

    /**
     * Constructor with error code, message, and cause.
     * Supports error chaining for complex scenarios.
     */
    protected DomainException(String errorCode, String message, Throwable cause) {
        super(validateMessage(message), cause);
        this.errorCode = validateErrorCode(errorCode);
        this.occurredAt = Instant.now();
        this.context = new ConcurrentHashMap<>();
        this.category = determineCategory(errorCode);
    }

    /**
     * Adds contextual information to the exception.
     * Thread-safe operation supporting 12-Factor principle of structured logging.
     */
    public void addContext(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Context key cannot be null or empty");
        }
        this.context.put(key, value);
    }

    /**
     * Adds multiple context entries at once.
     * Convenience method for bulk context addition.
     */
    public void addContext(Map<String, Object> contextMap) {
        if (contextMap != null) {
            contextMap.forEach(this::addContext);
        }
    }

    /**
     * Removes a context entry.
     * Supports dynamic context management.
     */
    public void removeContext(String key) {
        if (key != null) {
            this.context.remove(key);
        }
    }

    /**
     * Checks if a context key exists.
     * Utility method for conditional context handling.
     */
    public boolean hasContext(String key) {
        return key != null && this.context.containsKey(key);
    }

    /**
     * Returns the domain-specific error code.
     * Enables structured error handling across layers.
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the error category for grouping and classification.
     * Supports 12-Factor principle of error categorization.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns when the exception occurred.
     * Supports audit and debugging requirements.
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Returns contextual information as immutable map.
     * Follows clean code principles with defensive copying.
     */
    public Map<String, Object> getContext() {
        return Collections.unmodifiableMap(new HashMap<>(context));
    }

    /**
     * Returns a specific context value with type safety.
     * Utility method for typed context retrieval.
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextValue(String key, Class<T> type) {
        Object value = context.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Checks if this is a business rule violation.
     * Categorization method for different handling strategies.
     */
    public boolean isBusinessRuleViolation() {
        return "BUSINESS_RULE".equals(category);
    }

    /**
     * Checks if this is a validation error.
     * Categorization method for input validation failures.
     */
    public boolean isValidationError() {
        return "VALIDATION".equals(category);
    }

    /**
     * Creates a summary for logging and monitoring.
     * Supports 12-Factor principle of structured logging.
     */
    public ExceptionSummary createSummary() {
        return new ExceptionSummary(
            getClass().getSimpleName(),
            errorCode,
            category,
            getMessage(),
            occurredAt,
            context.size()
        );
    }

    /**
     * Validates error code parameter.
     * Private method ensuring data integrity.
     */
    private static String validateErrorCode(String errorCode) {
        if (errorCode == null || errorCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Error code cannot be null or empty");
        }
        return errorCode.trim().toUpperCase();
    }

    /**
     * Validates message parameter.
     * Private method ensuring meaningful error messages.
     */
    private static String validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Exception message cannot be null or empty");
        }
        return message.trim();
    }

    /**
     * Determines error category based on error code.
     * Business logic for error classification.
     */
    private String determineCategory(String errorCode) {
        if (errorCode.contains("VALIDATION") || errorCode.contains("INVALID")) {
            return "VALIDATION";
        } else if (errorCode.contains("BUSINESS") || errorCode.contains("RULE")) {
            return "BUSINESS_RULE";
        } else if (errorCode.contains("NOT_FOUND")) {
            return "NOT_FOUND";
        } else if (errorCode.contains("UNAUTHORIZED") || errorCode.contains("FORBIDDEN")) {
            return "SECURITY";
        }
        return "GENERAL";
    }

    @Override
    public String toString() {
        return String.format("%s{errorCode='%s', category='%s', message='%s', occurredAt=%s, contextSize=%d}",
                getClass().getSimpleName(), errorCode, category, getMessage(), occurredAt, context.size());
    }

    /**
     * Value object for exception summary information.
     * Follows DDD principles with immutable value objects.
     */
    public static class ExceptionSummary {
        private final String exceptionType;
        private final String errorCode;
        private final String category;
        private final String message;
        private final Instant occurredAt;
        private final int contextSize;

        public ExceptionSummary(String exceptionType, String errorCode, String category,
                               String message, Instant occurredAt, int contextSize) {
            this.exceptionType = exceptionType;
            this.errorCode = errorCode;
            this.category = category;
            this.message = message;
            this.occurredAt = occurredAt;
            this.contextSize = contextSize;
        }

        // Getters
        public String getExceptionType() { return exceptionType; }
        public String getErrorCode() { return errorCode; }
        public String getCategory() { return category; }
        public String getMessage() { return message; }
        public Instant getOccurredAt() { return occurredAt; }
        public int getContextSize() { return contextSize; }

        @Override
        public String toString() {
            return String.format("Summary{type=%s, code=%s, category=%s, time=%s, contextSize=%d}",
                    exceptionType, errorCode, category, occurredAt, contextSize);
        }
    }
}

/*
 * ARCHITECTURAL BENEFITS:
 *
 * 12-Factor App:
 * - Structured error information supporting logs as event streams
 * - Clear error categorization for monitoring and alerting
 * - Contextual metadata for observability and debugging
 * - Immutable state with controlled mutation for context
 *
 * DDD (Domain-Driven Design):
 * - Pure domain exception without infrastructure dependencies
 * - Rich domain context with business-relevant categorization
 * - Value objects (ExceptionSummary) for structured data
 * - Business logic methods for exception classification
 *
 * Hexagonal Architecture:
 * - Can be used across all layers without coupling
 * - Provides structured data for different adapters
 * - Clean separation between domain errors and technical errors
 * - Port-agnostic design for multiple output formats
 *
 * Clean Code:
 * - Thread-safe operations with ConcurrentHashMap
 * - Comprehensive validation and error handling
 * - Single responsibility with focused exception handling
 * - Meaningful method names expressing business intent
 * - Defensive programming with parameter validation
 */
