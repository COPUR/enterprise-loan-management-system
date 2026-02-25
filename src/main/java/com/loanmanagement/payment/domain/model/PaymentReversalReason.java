package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different reasons for payment reversals.
 */
public enum PaymentReversalReason {
    DUPLICATE_PAYMENT("Duplicate payment"),
    INCORRECT_AMOUNT("Incorrect amount"),
    WRONG_ACCOUNT("Wrong account"),
    FRAUD_DETECTION("Fraud detection"),
    CUSTOMER_REQUEST("Customer request"),
    BANK_ERROR("Bank error"),
    SYSTEM_ERROR("System error"),
    REGULATORY_REQUIREMENT("Regulatory requirement"),
    CHARGEBACK("Chargeback"),
    INSUFFICIENT_FUNDS("Insufficient funds"),
    PAYMENT_DISPUTE("Payment dispute"),
    ACCOUNT_CLOSURE("Account closure"),
    COMPLIANCE_VIOLATION("Compliance violation"),
    TECHNICAL_ERROR("Technical error"),
    AUTHORIZATION_REVOKED("Authorization revoked"),
    PROCESSING_ERROR("Processing error"),
    CURRENCY_CONVERSION_ERROR("Currency conversion error"),
    NETWORK_ERROR("Network error"),
    TIMEOUT("Timeout"),
    OTHER("Other");

    private final String description;

    PaymentReversalReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSystemInitiated() {
        return this == FRAUD_DETECTION || this == SYSTEM_ERROR || this == TECHNICAL_ERROR || 
               this == PROCESSING_ERROR || this == NETWORK_ERROR || this == TIMEOUT;
    }

    public boolean isCustomerInitiated() {
        return this == CUSTOMER_REQUEST || this == PAYMENT_DISPUTE || this == CHARGEBACK;
    }
}