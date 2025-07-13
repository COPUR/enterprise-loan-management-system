package com.amanahfi.compliance.domain.check;

/**
 * Risk score enumeration for compliance assessments
 */
public enum RiskScore {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);
    
    private final int numericValue;
    
    RiskScore(int numericValue) {
        this.numericValue = numericValue;
    }
    
    public int getNumericValue() {
        return numericValue;
    }
    
    public boolean isHigherThan(RiskScore other) {
        return this.numericValue > other.numericValue;
    }
    
    /**
     * Convert numeric risk score to enum value
     */
    public static RiskScore fromValue(Integer value) {
        if (value == null) {
            return LOW;
        }
        
        if (value <= 25) {
            return LOW;
        } else if (value <= 50) {
            return MEDIUM;
        } else if (value <= 75) {
            return HIGH;
        } else {
            return CRITICAL;
        }
    }
    
    /**
     * Convert numeric percentage (0-100) to risk score
     */
    public static RiskScore fromPercentage(int percentage) {
        return fromValue(percentage);
    }
}