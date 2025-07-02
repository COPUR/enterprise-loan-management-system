package com.bank.loan.loan.ai.domain.model;

/**
 * Employment Type enumeration for loan analysis
 */
public enum EmploymentType {
    FULL_TIME("Full-time employment"),
    PART_TIME("Part-time employment"),
    SELF_EMPLOYED("Self-employed"),
    CONTRACT("Contract worker"),
    UNEMPLOYED("Unemployed"),
    RETIRED("Retired"),
    STUDENT("Student"),
    GOVERNMENT("Government employee"),
    MILITARY("Military service"),
    FREELANCER("Freelancer");

    private final String description;

    EmploymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if employment type is considered stable for loan purposes
     */
    public boolean isStableEmployment() {
        return this == FULL_TIME || this == GOVERNMENT || this == MILITARY;
    }

    /**
     * Check if employment type requires additional income verification
     */
    public boolean requiresAdditionalVerification() {
        return this == SELF_EMPLOYED || this == CONTRACT || this == FREELANCER;
    }

    /**
     * Get employment stability score (higher is better)
     */
    public int getStabilityScore() {
        return switch (this) {
            case GOVERNMENT, MILITARY -> 10;
            case FULL_TIME -> 9;
            case PART_TIME -> 6;
            case SELF_EMPLOYED -> 5;
            case CONTRACT -> 4;
            case FREELANCER -> 3;
            case RETIRED -> 2;
            case STUDENT -> 1;
            case UNEMPLOYED -> 0;
        };
    }
}