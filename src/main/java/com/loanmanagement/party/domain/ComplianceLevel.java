package com.loanmanagement.party.domain;

/**
 * Compliance level enumeration for the banking system
 * Defines different compliance levels for parties
 */
public enum ComplianceLevel {
    BASIC("Basic compliance level", 1),
    STANDARD("Standard compliance level", 2),
    HIGH("High compliance level", 3),
    PREMIUM("Premium compliance level with enhanced checks", 4);
    
    private final String description;
    private final int level;
    
    ComplianceLevel(String description, int level) {
        this.description = description;
        this.level = level;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isAtLeast(ComplianceLevel other) {
        return this.level >= other.level;
    }
    
    public boolean isHigherThan(ComplianceLevel other) {
        return this.level > other.level;
    }
    
    public boolean requiresEnhancedDueDiligence() {
        return this == HIGH || this == PREMIUM;
    }
}