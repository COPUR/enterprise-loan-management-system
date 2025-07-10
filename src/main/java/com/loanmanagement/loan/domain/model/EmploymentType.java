package com.loanmanagement.loan.domain.model;

/**
 * Employment Type Enumeration
 * Defines the types of employment status
 */
public enum EmploymentType {
    /**
     * Full-time employment
     */
    FULL_TIME("Full-Time", "Regular full-time employment", 1.0),
    
    /**
     * Part-time employment
     */
    PART_TIME("Part-Time", "Part-time employment", 0.7),
    
    /**
     * Contract or temporary employment
     */
    CONTRACT("Contract", "Contract or temporary employment", 0.6),
    
    /**
     * Self-employed
     */
    SELF_EMPLOYED("Self-Employed", "Self-employed or business owner", 0.8),
    
    /**
     * Government employment
     */
    GOVERNMENT("Government", "Government employee", 1.0),
    
    /**
     * Unemployed
     */
    UNEMPLOYED("Unemployed", "Currently unemployed", 0.0);

    private final String displayName;
    private final String description;
    private final double stabilityFactor; // 0.0 to 1.0

    EmploymentType(String displayName, String description, double stabilityFactor) {
        this.displayName = displayName;
        this.description = description;
        this.stabilityFactor = stabilityFactor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public double getStabilityFactor() {
        return stabilityFactor;
    }

    /**
     * Check if this employment type is considered stable
     */
    public boolean isStable() {
        return stabilityFactor >= 0.8;
    }

    /**
     * Check if this employment type allows for loan eligibility
     */
    public boolean isEligibleForLoans() {
        return this != UNEMPLOYED;
    }

    /**
     * Get minimum employment duration requirement in months
     */
    public int getMinimumDurationMonths() {
        return switch (this) {
            case FULL_TIME, PART_TIME, GOVERNMENT -> 12;
            case CONTRACT -> 6;
            case SELF_EMPLOYED -> 24;
            case UNEMPLOYED -> 0;
        };
    }

    /**
     * Get income verification requirements
     */
    public String getIncomeVerificationRequirement() {
        return switch (this) {
            case FULL_TIME, PART_TIME, GOVERNMENT -> "Pay stubs and employment verification";
            case CONTRACT -> "Contract agreement and recent payments";
            case SELF_EMPLOYED -> "Tax returns and business bank statements";
            case UNEMPLOYED -> "Alternative income sources required";
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}