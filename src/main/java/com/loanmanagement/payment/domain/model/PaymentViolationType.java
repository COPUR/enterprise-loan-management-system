package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different types of payment violations.
 */
public enum PaymentViolationType {
    
    LATE_PAYMENT("Payment made after due date"),
    MISSED_PAYMENT("Payment not made by due date"),
    INSUFFICIENT_FUNDS("Payment failed due to insufficient funds"),
    PAYMENT_REVERSAL("Payment was reversed after processing"),
    DUPLICATE_PAYMENT("Duplicate payment detected"),
    UNAUTHORIZED_PAYMENT("Payment made without proper authorization"),
    AMOUNT_MISMATCH("Payment amount does not match expected amount"),
    FREQUENCY_VIOLATION("Payment frequency violates agreement terms"),
    METHOD_VIOLATION("Payment method violates agreement terms"),
    CURRENCY_VIOLATION("Payment currency violates agreement terms"),
    GEOGRAPHIC_VIOLATION("Payment originates from restricted location"),
    REGULATORY_VIOLATION("Payment violates regulatory requirements"),
    COMPLIANCE_VIOLATION("Payment violates compliance policies"),
    FRAUD_VIOLATION("Payment identified as fraudulent"),
    SYSTEM_VIOLATION("Payment violates system rules");
    
    private final String description;
    
    PaymentViolationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isDelinquencyRelated() {
        return this == LATE_PAYMENT || this == MISSED_PAYMENT;
    }
    
    public boolean isFraudRelated() {
        return this == FRAUD_VIOLATION || this == UNAUTHORIZED_PAYMENT || 
               this == DUPLICATE_PAYMENT;
    }
    
    public boolean isComplianceRelated() {
        return this == REGULATORY_VIOLATION || this == COMPLIANCE_VIOLATION || 
               this == GEOGRAPHIC_VIOLATION;
    }
    
    public boolean isTechnicalRelated() {
        return this == SYSTEM_VIOLATION || this == INSUFFICIENT_FUNDS || 
               this == PAYMENT_REVERSAL;
    }
    
    public boolean isContractualRelated() {
        return this == AMOUNT_MISMATCH || this == FREQUENCY_VIOLATION || 
               this == METHOD_VIOLATION || this == CURRENCY_VIOLATION;
    }
}