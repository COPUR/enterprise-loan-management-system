package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different types of payment anomalies.
 */
public enum PaymentAnomalyType {
    
    UNUSUAL_AMOUNT("Payment amount significantly differs from historical patterns"),
    UNUSUAL_FREQUENCY("Payment frequency differs from established patterns"),
    UNUSUAL_TIMING("Payment timing is outside normal patterns"),
    SUSPICIOUS_SOURCE("Payment source appears suspicious or unfamiliar"),
    DUPLICATE_PAYMENT("Potential duplicate payment detected"),
    CIRCULAR_PAYMENT("Payment appears to be part of a circular transaction"),
    VELOCITY_ANOMALY("Payment velocity exceeds normal thresholds"),
    GEOGRAPHIC_ANOMALY("Payment originates from unusual geographic location"),
    AMOUNT_STRUCTURING("Payment amount suggests potential structuring activity"),
    BEHAVIORAL_ANOMALY("Payment behavior differs from customer's historical patterns"),
    TECHNICAL_ANOMALY("Payment processed through unusual technical means"),
    REGULATORY_FLAG("Payment triggers regulatory monitoring flags"),
    FRAUD_INDICATOR("Payment shows potential fraud indicators"),
    COMPLIANCE_CONCERN("Payment raises compliance concerns"),
    SYSTEM_ERROR("Payment appears to be result of system error");
    
    private final String description;
    
    PaymentAnomalyType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isSecurityRelated() {
        return this == SUSPICIOUS_SOURCE || this == CIRCULAR_PAYMENT || 
               this == FRAUD_INDICATOR || this == AMOUNT_STRUCTURING;
    }
    
    public boolean isComplianceRelated() {
        return this == REGULATORY_FLAG || this == COMPLIANCE_CONCERN || 
               this == AMOUNT_STRUCTURING;
    }
    
    public boolean isBehavioralRelated() {
        return this == BEHAVIORAL_ANOMALY || this == UNUSUAL_FREQUENCY || 
               this == UNUSUAL_TIMING || this == VELOCITY_ANOMALY;
    }
    
    public boolean isTechnicalRelated() {
        return this == TECHNICAL_ANOMALY || this == SYSTEM_ERROR || 
               this == DUPLICATE_PAYMENT;
    }
}