package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different reasons for payment failures.
 */
public enum PaymentFailureReason {
    INSUFFICIENT_FUNDS("Insufficient funds in account"),
    INVALID_ACCOUNT("Invalid account information"),
    ACCOUNT_CLOSED("Account is closed"),
    ACCOUNT_FROZEN("Account is frozen"),
    PAYMENT_LIMIT_EXCEEDED("Payment limit exceeded"),
    NETWORK_ERROR("Network connectivity error"),
    PROCESSING_ERROR("Payment processing error"),
    INVALID_PAYMENT_METHOD("Invalid payment method"),
    EXPIRED_PAYMENT_METHOD("Payment method expired"),
    DUPLICATE_PAYMENT("Duplicate payment detected"),
    FRAUD_DETECTION("Fraud detection triggered"),
    VALIDATION_ERROR("Payment validation failed"),
    SYSTEM_MAINTENANCE("System under maintenance"),
    CURRENCY_CONVERSION_ERROR("Currency conversion failed"),
    REGULATORY_RESTRICTION("Regulatory restriction"),
    UNKNOWN("Unknown error");

    private final String description;

    PaymentFailureReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}