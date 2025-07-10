package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different levels of payment violation severity.
 */
public enum PaymentViolationSeverity {
    
    MINOR("Minor violation - warning issued"),
    MODERATE("Moderate violation - review required"),
    MAJOR("Major violation - immediate action required"),
    SEVERE("Severe violation - urgent escalation required"),
    CRITICAL("Critical violation - emergency response required");
    
    private final String description;
    
    PaymentViolationSeverity(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean requiresImmediateAction() {
        return this == MAJOR || this == SEVERE || this == CRITICAL;
    }
    
    public boolean requiresUrgentAction() {
        return this == SEVERE || this == CRITICAL;
    }
    
    public boolean requiresEmergencyResponse() {
        return this == CRITICAL;
    }
    
    public boolean requiresReview() {
        return this == MODERATE || this == MAJOR || this == SEVERE || this == CRITICAL;
    }
    
    public int getSeverityLevel() {
        return switch (this) {
            case MINOR -> 1;
            case MODERATE -> 2;
            case MAJOR -> 3;
            case SEVERE -> 4;
            case CRITICAL -> 5;
        };
    }
    
    public boolean isMoreSevereThan(PaymentViolationSeverity other) {
        return this.getSeverityLevel() > other.getSeverityLevel();
    }
}