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
}