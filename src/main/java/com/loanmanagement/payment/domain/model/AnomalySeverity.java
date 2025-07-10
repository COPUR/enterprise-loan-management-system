package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different levels of anomaly severity.
 */
public enum AnomalySeverity {
    
    LOW("Low severity anomaly - monitoring required"),
    MEDIUM("Medium severity anomaly - review recommended"),
    HIGH("High severity anomaly - immediate attention required"),
    CRITICAL("Critical severity anomaly - urgent action required");
    
    private final String description;
    
    AnomalySeverity(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean requiresImmediateAction() {
        return this == HIGH || this == CRITICAL;
    }
    
    public boolean requiresUrgentAction() {
        return this == CRITICAL;
    }
    
    public boolean requiresReview() {
        return this == MEDIUM || this == HIGH || this == CRITICAL;
    }
    
    public int getSeverityLevel() {
        return switch (this) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }
}