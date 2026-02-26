package com.loanmanagement.payment.domain.model;

import lombok.Getter;

/**
 * Enum representing the health status of a payment workflow.
 * Used for monitoring and alerting.
 */
@Getter
public enum PaymentWorkflowHealth {
    
    HEALTHY(
            "Healthy",
            "Workflow is operating normally",
            0,
            "green"),
    
    WARNING(
            "Warning",
            "Workflow has minor issues or warnings",
            1,
            "yellow"),
    
    DEGRADED(
            "Degraded",
            "Workflow performance is degraded",
            2,
            "orange"),
    
    UNHEALTHY(
            "Unhealthy",
            "Workflow has significant issues",
            3,
            "red"),
    
    CRITICAL(
            "Critical",
            "Workflow is in critical condition",
            4,
            "dark-red");
    
    private final String displayName;
    private final String description;
    private final int severityLevel;
    private final String colorCode;
    
    PaymentWorkflowHealth(
            String displayName,
            String description,
            int severityLevel,
            String colorCode) {
        
        this.displayName = displayName;
        this.description = description;
        this.severityLevel = severityLevel;
        this.colorCode = colorCode;
    }
    
    /**
     * Determines health based on error count and duration.
     */
    public static PaymentWorkflowHealth fromMetrics(
            int errorCount,
            long durationMillis,
            long expectedDurationMillis) {
        
        // Critical if too many errors
        if (errorCount > 5) {
            return CRITICAL;
        }
        
        // Unhealthy if multiple errors
        if (errorCount > 2) {
            return UNHEALTHY;
        }
        
        // Check duration against expected
        double durationRatio = (double) durationMillis / expectedDurationMillis;
        
        if (durationRatio > 3.0) {
            return UNHEALTHY;
        } else if (durationRatio > 2.0) {
            return DEGRADED;
        } else if (durationRatio > 1.5 || errorCount > 0) {
            return WARNING;
        }
        
        return HEALTHY;
    }
    
    /**
     * Determines health based on retry count.
     */
    public static PaymentWorkflowHealth fromRetryCount(
            int retryCount,
            int maxRetries) {
        
        if (retryCount >= maxRetries) {
            return CRITICAL;
        }
        
        double retryRatio = (double) retryCount / maxRetries;
        
        if (retryRatio > 0.75) {
            return UNHEALTHY;
        } else if (retryRatio > 0.5) {
            return DEGRADED;
        } else if (retryRatio > 0.25) {
            return WARNING;
        }
        
        return HEALTHY;
    }
    
    /**
     * Combines multiple health indicators.
     */
    public static PaymentWorkflowHealth combine(PaymentWorkflowHealth... healths) {
        if (healths == null || healths.length == 0) {
            return HEALTHY;
        }
        
        PaymentWorkflowHealth worst = HEALTHY;
        for (PaymentWorkflowHealth health : healths) {
            if (health != null && health.severityLevel > worst.severityLevel) {
                worst = health;
            }
        }
        
        return worst;
    }
    
    /**
     * Checks if this health is worse than another.
     */
    public boolean isWorseThan(PaymentWorkflowHealth other) {
        return this.severityLevel > other.severityLevel;
    }
    
    /**
     * Checks if this health is better than another.
     */
    public boolean isBetterThan(PaymentWorkflowHealth other) {
        return this.severityLevel < other.severityLevel;
    }
    
    /**
     * Checks if intervention is required.
     */
    public boolean requiresIntervention() {
        return this.severityLevel >= UNHEALTHY.severityLevel;
    }
    
    /**
     * Checks if alerts should be sent.
     */
    public boolean shouldAlert() {
        return this.severityLevel >= WARNING.severityLevel;
    }
    
    /**
     * Gets the monitoring interval based on health.
     */
    public long getMonitoringIntervalMillis() {
        return switch (this) {
            case HEALTHY -> 60000L;     // 1 minute
            case WARNING -> 30000L;     // 30 seconds
            case DEGRADED -> 15000L;    // 15 seconds
            case UNHEALTHY -> 5000L;    // 5 seconds
            case CRITICAL -> 1000L;     // 1 second
        };
    }
    
    /**
     * Gets recommended action based on health.
     */
    public String getRecommendedAction() {
        return switch (this) {
            case HEALTHY -> "No action required - continue monitoring";
            case WARNING -> "Monitor closely and investigate warnings";
            case DEGRADED -> "Investigate performance issues and consider scaling";
            case UNHEALTHY -> "Immediate investigation required - check error logs";
            case CRITICAL -> "Critical intervention required - escalate to operations team";
        };
    }
}