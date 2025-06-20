package com.bank.loanmanagement.domain.loan;

/**
 * Domain enum for employment status
 */
public enum EmploymentStatus {
    FULL_TIME("Full Time", true, 20),
    PART_TIME("Part Time", false, 10),
    SELF_EMPLOYED("Self Employed", false, 15),
    CONTRACT("Contract", false, 8),
    RETIRED("Retired", true, 12),
    UNEMPLOYED("Unemployed", false, 0),
    STUDENT("Student", false, 5);

    private final String displayName;
    private final boolean stable;
    private final int stabilityPoints;

    EmploymentStatus(String displayName, boolean stable, int stabilityPoints) {
        this.displayName = displayName;
        this.stable = stable;
        this.stabilityPoints = stabilityPoints;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isStable() {
        return stable;
    }

    public int getStabilityPoints() {
        return stabilityPoints;
    }

    public boolean allowsLending() {
        return this != UNEMPLOYED;
    }
}