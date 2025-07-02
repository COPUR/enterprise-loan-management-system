package com.loanmanagement.domain.exception;

import com.loanmanagement.sharedkernel.domain.value.Money;
import java.time.Instant;

/**
 * Domain exception thrown when a customer has insufficient credit for a loan operation.
 * Follows DDD principles by representing a business rule violation in the credit domain.
 *
 * Supports 12-Factor App principles by:
 * - Providing structured error information for logging
 * - Including contextual data for monitoring and observability
 * - Enabling proper error handling across application boundaries
 *
 * Follows clean code principles by:
 * - Using meaningful domain types (Money instead of String)
 * - Providing multiple constructor overloads for different scenarios
 * - Including rich context for debugging and error resolution
 */
public class InsufficientCreditException extends DomainException {

    public static final String ERROR_CODE = "INSUFFICIENT_CREDIT";

    private final Long customerId;
    private final Money requestedAmount;
    private final Money availableAmount;
    private final Instant occurredAt;

    /**
     * Simple constructor for basic error scenarios.
     * Follows clean code principles with clear intent.
     */
    public InsufficientCreditException(String message) {
        super(ERROR_CODE, message);
        this.customerId = null;
        this.requestedAmount = null;
        this.availableAmount = null;
        this.occurredAt = Instant.now();
    }

    /**
     * Rich constructor providing full context for business rule violations.
     * Follows DDD principles by using proper value objects and domain context.
     */
    public InsufficientCreditException(Long customerId, Money requestedAmount, Money availableAmount) {
        super(ERROR_CODE, buildDetailedMessage(customerId, requestedAmount, availableAmount));
        this.customerId = customerId;
        this.requestedAmount = requestedAmount;
        this.availableAmount = availableAmount;
        this.occurredAt = Instant.now();
    }

    /**
     * Factory method for creating exceptions with additional context.
     * Follows DDD principles with intention-revealing factory methods.
     * Enhanced with input validation following clean code principles.
     */
    public static InsufficientCreditException forCustomer(Long customerId, Money requested, Money available) {
        validateFactoryParameters(customerId, requested, available, "customer credit check");
        return new InsufficientCreditException(customerId, requested, available);
    }

    /**
     * Factory method for loan application scenarios.
     * Provides domain-specific context for different business operations.
     * Enhanced with EDA support for event-driven reactions and input validation.
     */
    public static InsufficientCreditException forLoanApplication(Long customerId, Money loanAmount, Money creditLimit) {
        validateFactoryParameters(customerId, loanAmount, creditLimit, "loan application");

        InsufficientCreditException exception = new InsufficientCreditException(customerId, loanAmount, creditLimit);
        exception.addContext("operation", "loan_application");
        exception.addContext("business_rule", "credit_limit_validation");

        // EDA enhancement: prepare for event generation
        exception.addContext("domain_event_type", "CreditLimitViolationDetected");
        exception.addContext("event_severity", exception.isCriticalViolation() ? "CRITICAL" : "NORMAL");
        exception.addContext("event_category", "BUSINESS_RULE_VIOLATION");

        return exception;
    }

    /**
     * Validates factory method parameters following defensive programming principles.
     * Private helper method ensuring data integrity and fail-fast validation.
     */
    private static void validateFactoryParameters(Long customerId, Money requested, Money available, String context) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive for " + context);
        }
        if (requested == null) {
            throw new IllegalArgumentException("Requested amount cannot be null for " + context);
        }
        if (available == null) {
            throw new IllegalArgumentException("Available amount cannot be null for " + context);
        }
        if (requested.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Requested amount cannot be negative for " + context);
        }
        if (available.getAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Available amount cannot be negative for " + context);
        }
    }

    /**
     * Builds a detailed error message following clean code principles.
     * Private method to centralize message formatting logic.
     */
    private static String buildDetailedMessage(Long customerId, Money requestedAmount, Money availableAmount) {
        if (requestedAmount != null && availableAmount != null) {
            Money shortfall = requestedAmount.subtract(availableAmount);
            return String.format(
                "Insufficient credit for customer %d. Requested: %s, Available: %s, Shortfall: %s",
                customerId, requestedAmount, availableAmount, shortfall
            );
        } else {
            return String.format("Insufficient credit for customer %d", customerId);
        }
    }

    /**
     * Returns the deficit amount for business logic and reporting.
     * Supports domain-driven design with calculated business values.
     */
    public Money getDeficitAmount() {
        if (requestedAmount != null && availableAmount != null) {
            return requestedAmount.subtract(availableAmount);
        }
        return Money.ZERO;
    }

    /**
     * Checks if this is a critical credit violation (large deficit).
     * Business logic method supporting different handling strategies.
     */
    public boolean isCriticalViolation() {
        Money deficit = getDeficitAmount();
        Money criticalThreshold = Money.of("10000"); // Could be configurable
        return deficit.isGreaterThan(criticalThreshold);
    }

    /**
     * Returns a structured representation for logging and monitoring.
     * Supports 12-Factor principle of treating logs as event streams.
     */
    public CreditViolationDetails getViolationDetails() {
        return new CreditViolationDetails(
            customerId,
            requestedAmount,
            availableAmount,
            getDeficitAmount(),
            isCriticalViolation(),
            occurredAt
        );
    }

    /**
     * Creates a domain event representation of this credit violation.
     * Supports EDA by enabling event-driven reactions to credit violations.
     * Following 12-Factor principle of structured data for event streams.
     */
    public java.util.Map<String, Object> toEventPayload() {
        java.util.Map<String, Object> eventData = new java.util.HashMap<>();
        eventData.put("eventType", "CreditLimitViolationDetected");
        eventData.put("customerId", customerId);
        eventData.put("requestedAmount", requestedAmount != null ? requestedAmount.getAmount() : null);
        eventData.put("availableAmount", availableAmount != null ? availableAmount.getAmount() : null);
        eventData.put("deficitAmount", getDeficitAmount().getAmount());
        eventData.put("isCritical", isCriticalViolation());
        eventData.put("occurredAt", occurredAt);
        eventData.put("errorCode", getErrorCode());
        eventData.put("category", getCategory());

        // Include all context for comprehensive event data
        eventData.putAll(getContext());

        return java.util.Collections.unmodifiableMap(eventData);
    }

    /**
     * Checks if this violation should trigger immediate alerts.
     * Business rule for EDA event routing and processing priorities.
     */
    public boolean shouldTriggerAlert() {
        return isCriticalViolation() ||
               (customerId != null && getContext().containsKey("repeat_violation"));
    }

    /**
     * Returns event routing key for EDA message distribution.
     * Supports event routing based on severity and customer type.
     */
    public String getEventRoutingKey() {
        StringBuilder routingKey = new StringBuilder("credit.violation");

        if (isCriticalViolation()) {
            routingKey.append(".critical");
        } else {
            routingKey.append(".normal");
        }

        if (getContext().containsKey("operation")) {
            routingKey.append(".").append(getContext().get("operation"));
        }

        return routingKey.toString();
    }

    // Getters with proper encapsulation
    public Long getCustomerId() {
        return customerId;
    }

    public Money getRequestedAmount() {
        return requestedAmount;
    }

    public Money getAvailableAmount() {
        return availableAmount;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * Value object for structured credit violation information.
     * Follows DDD principles with immutable value objects.
     */
    public static class CreditViolationDetails {
        private final Long customerId;
        private final Money requestedAmount;
        private final Money availableAmount;
        private final Money deficitAmount;
        private final boolean criticalViolation;
        private final Instant occurredAt;

        public CreditViolationDetails(Long customerId, Money requestedAmount, Money availableAmount,
                                    Money deficitAmount, boolean criticalViolation, Instant occurredAt) {
            this.customerId = customerId;
            this.requestedAmount = requestedAmount;
            this.availableAmount = availableAmount;
            this.deficitAmount = deficitAmount;
            this.criticalViolation = criticalViolation;
            this.occurredAt = occurredAt;
        }

        // Getters
        public Long getCustomerId() { return customerId; }
        public Money getRequestedAmount() { return requestedAmount; }
        public Money getAvailableAmount() { return availableAmount; }
        public Money getDeficitAmount() { return deficitAmount; }
        public boolean isCriticalViolation() { return criticalViolation; }
        public Instant getOccurredAt() { return occurredAt; }

        @Override
        public String toString() {
            return String.format("CreditViolation{customer=%d, requested=%s, available=%s, deficit=%s, critical=%s, time=%s}",
                customerId, requestedAmount, availableAmount, deficitAmount, criticalViolation, occurredAt);
        }
    }
}

/*
 * ARCHITECTURAL BENEFITS:
 *
 * 12-Factor App:
 * - Structured error information supporting logs as event streams
 * - Clear error categorization for monitoring and alerting
 * - Contextual data for observability and debugging
 *
 * DDD (Domain-Driven Design):
 * - Uses proper domain value objects (Money) instead of primitives
 * - Represents business rule violations with rich domain context
 * - Factory methods with intention-revealing names
 * - Business logic methods for domain-specific calculations
 *
 * Hexagonal Architecture:
 * - Domain exception that doesn't depend on infrastructure concerns
 * - Can be used across all layers without coupling
 * - Provides structured data for different adapters
 *
 * Clean Code:
 * - Single responsibility with focused exception handling
 * - Meaningful method names expressing business intent
 * - Immutable value objects for data consistency
 * - Comprehensive documentation and context
 */
