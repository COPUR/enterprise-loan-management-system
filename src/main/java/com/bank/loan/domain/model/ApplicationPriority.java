package com.bank.loan.domain.model;

/**
 * Enumeration representing the priority level of a loan application
 */
public enum ApplicationPriority {
    LOW("Low", 1, 10),
    STANDARD("Standard", 2, 7),
    HIGH("High", 3, 3),
    URGENT("Urgent", 4, 1);
    
    private final String displayName;
    private final int level;
    private final int targetProcessingDays;
    
    ApplicationPriority(String displayName, int level, int targetProcessingDays) {
        this.displayName = displayName;
        this.level = level;
        this.targetProcessingDays = targetProcessingDays;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getTargetProcessingDays() {
        return targetProcessingDays;
    }
    
    public boolean isHigherPriorityThan(ApplicationPriority other) {
        return this.level > other.level;
    }
    
    public ApplicationPriority escalate() {
        return switch (this) {
            case LOW -> STANDARD;
            case STANDARD -> HIGH;
            case HIGH, URGENT -> URGENT;
        };
    }
    
    public boolean requiresManagerApproval() {
        return this == HIGH || this == URGENT;
    }
}