package com.amanahfi.risk.domain.assessment;

/**
 * Risk levels for risk assessment
 */
public enum RiskLevel {
    
    LOW(1, "Low Risk", "Minimal risk - standard processing"),
    MEDIUM(2, "Medium Risk", "Moderate risk - enhanced monitoring required"),
    HIGH(3, "High Risk", "High risk - manual review and approval required"),
    CRITICAL(4, "Critical Risk", "Critical risk - senior management approval required");

    private final int level;
    private final String displayName;
    private final String description;

    RiskLevel(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHigherThan(RiskLevel other) {
        return this.level > other.level;
    }

    public boolean isLowerThan(RiskLevel other) {
        return this.level < other.level;
    }

    public boolean requiresManualReview() {
        return this == HIGH || this == CRITICAL;
    }

    public boolean requiresSeniorApproval() {
        return this == CRITICAL;
    }

    public static RiskLevel fromScore(int score) {
        if (score >= 90) return CRITICAL;
        if (score >= 75) return HIGH;
        if (score >= 50) return MEDIUM;
        return LOW;
    }
}