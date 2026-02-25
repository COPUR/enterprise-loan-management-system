package com.loanmanagement.payment.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Enum representing workflow execution priorities.
 * Determines processing order and resource allocation.
 */
@Getter
public enum PaymentWorkflowPriority {
    
    CRITICAL(
            1,
            "Critical",
            "Highest priority - immediate processing required",
            1000,
            0.95),
    
    HIGH(
            2,
            "High",
            "High priority - expedited processing",
            500,
            0.85),
    
    MEDIUM(
            3,
            "Medium",
            "Normal priority - standard processing",
            100,
            0.70),
    
    LOW(
            4,
            "Low",
            "Low priority - can be delayed",
            10,
            0.50),
    
    DEFERRED(
            5,
            "Deferred",
            "Lowest priority - process when resources available",
            1,
            0.30);
    
    private final int level;
    private final String displayName;
    private final String description;
    private final int weight;
    private final double resourceAllocationFactor;
    
    PaymentWorkflowPriority(
            int level,
            String displayName,
            String description,
            int weight,
            double resourceAllocationFactor) {
        
        this.level = level;
        this.displayName = displayName;
        this.description = description;
        this.weight = weight;
        this.resourceAllocationFactor = resourceAllocationFactor;
    }
    
    /**
     * Determines priority based on payment amount.
     */
    public static PaymentWorkflowPriority fromAmount(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("1000000")) >= 0) {
            return CRITICAL;
        } else if (amount.compareTo(new BigDecimal("100000")) >= 0) {
            return HIGH;
        } else if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            return MEDIUM;
        } else if (amount.compareTo(new BigDecimal("1000")) >= 0) {
            return LOW;
        } else {
            return DEFERRED;
        }
    }
    
    /**
     * Determines priority based on workflow type and urgency.
     */
    public static PaymentWorkflowPriority fromTypeAndUrgency(
            PaymentWorkflowType type,
            boolean isUrgent) {
        
        if (isUrgent) {
            return type == PaymentWorkflowType.INSTANT_PAYMENT ? CRITICAL : HIGH;
        }
        
        return switch (type) {
            case INSTANT_PAYMENT -> HIGH;
            case REVERSAL -> HIGH;
            case INTERNATIONAL_PAYMENT -> MEDIUM;
            case STANDARD_PAYMENT -> MEDIUM;
            case RECURRING_PAYMENT -> LOW;
            case BATCH_PAYMENT -> LOW;
            case BULK_TRANSFER -> DEFERRED;
            default -> MEDIUM;
        };
    }
    
    /**
     * Checks if this priority is higher than another.
     */
    public boolean isHigherThan(PaymentWorkflowPriority other) {
        return this.level < other.level;
    }
    
    /**
     * Checks if this priority is lower than another.
     */
    public boolean isLowerThan(PaymentWorkflowPriority other) {
        return this.level > other.level;
    }
    
    /**
     * Gets the queue timeout for this priority.
     */
    public long getQueueTimeoutMillis() {
        return switch (this) {
            case CRITICAL -> 60000L;      // 1 minute
            case HIGH -> 300000L;          // 5 minutes
            case MEDIUM -> 900000L;        // 15 minutes
            case LOW -> 3600000L;          // 1 hour
            case DEFERRED -> 86400000L;    // 24 hours
        };
    }
    
    /**
     * Gets the retry delay for this priority.
     */
    public long getRetryDelayMillis() {
        return switch (this) {
            case CRITICAL -> 1000L;    // 1 second
            case HIGH -> 5000L;        // 5 seconds
            case MEDIUM -> 30000L;     // 30 seconds
            case LOW -> 60000L;        // 1 minute
            case DEFERRED -> 300000L;  // 5 minutes
        };
    }
    
    /**
     * Gets the maximum concurrent executions for this priority.
     */
    public int getMaxConcurrentExecutions() {
        return switch (this) {
            case CRITICAL -> 50;
            case HIGH -> 30;
            case MEDIUM -> 20;
            case LOW -> 10;
            case DEFERRED -> 5;
        };
    }
    
    /**
     * Escalates priority based on wait time.
     */
    public PaymentWorkflowPriority escalate(long waitTimeMillis) {
        long escalationThreshold = getQueueTimeoutMillis() / 2;
        
        if (waitTimeMillis > escalationThreshold && this != CRITICAL) {
            return values()[Math.max(0, this.ordinal() - 1)];
        }
        
        return this;
    }
    
    /**
     * Gets a score for priority-based sorting.
     */
    public int getScore(long ageMillis, BigDecimal amount) {
        // Base score from weight
        int score = weight * 1000;
        
        // Age factor (older items get higher score)
        long ageMinutes = ageMillis / 60000;
        score += (int) (ageMinutes * 10);
        
        // Amount factor for same priority level
        if (amount != null) {
            score += amount.intValue() / 1000;
        }
        
        return score;
    }
}