package com.bank.loanmanagement.loan.domain.loan;

/**
 * Domain enum for risk levels
 */
public enum RiskLevel {
    LOW("Low Risk", 1),
    MEDIUM("Medium Risk", 2),
    HIGH("High Risk", 3);

    private final String displayName;
    private final int severity;

    RiskLevel(String displayName, int severity) {
        this.displayName = displayName;
        this.severity = severity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSeverity() {
        return severity;
    }

    public boolean isHigherThan(RiskLevel other) {
        return this.severity > other.severity;
    }

    public boolean isLowerThan(RiskLevel other) {
        return this.severity < other.severity;
    }
}