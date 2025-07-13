package com.amanahfi.compliance.domain.check;

/**
 * Severity levels for compliance violations
 */
public enum SeverityLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);
    
    private final int level;
    
    SeverityLevel(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    public boolean isHigherThan(SeverityLevel other) {
        return this.level > other.level;
    }
}