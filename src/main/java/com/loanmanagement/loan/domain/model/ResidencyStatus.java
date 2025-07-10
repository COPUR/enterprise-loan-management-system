package com.loanmanagement.loan.domain.model;

/**
 * Residency Status Enumeration
 * Defines the residency/citizenship status of customers
 */
public enum ResidencyStatus {
    /**
     * US Citizen
     */
    CITIZEN("US Citizen", "United States citizen", true, false),
    
    /**
     * Permanent Resident (Green Card holder)
     */
    PERMANENT_RESIDENT("Permanent Resident", "Lawful permanent resident", true, true),
    
    /**
     * Temporary Resident (Visa holder)
     */
    TEMPORARY_RESIDENT("Temporary Resident", "Temporary resident with valid visa", true, true),
    
    /**
     * Work Visa Holder
     */
    WORK_VISA("Work Visa", "Non-resident with work authorization", true, true),
    
    /**
     * Student Visa Holder
     */
    STUDENT_VISA("Student Visa", "Student with valid study visa", false, true),
    
    /**
     * Tourist or Visitor
     */
    TOURIST("Tourist/Visitor", "Tourist or temporary visitor", false, false);

    private final String displayName;
    private final String description;
    private final boolean eligibleForLoans;
    private final boolean requiresAdditionalDocumentation;

    ResidencyStatus(String displayName, String description, boolean eligibleForLoans, 
                   boolean requiresAdditionalDocumentation) {
        this.displayName = displayName;
        this.description = description;
        this.eligibleForLoans = eligibleForLoans;
        this.requiresAdditionalDocumentation = requiresAdditionalDocumentation;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEligibleForLoans() {
        return eligibleForLoans;
    }

    public boolean requiresAdditionalDocumentation() {
        return requiresAdditionalDocumentation;
    }

    /**
     * Check if this status has the highest lending privileges
     */
    public boolean hasFullLendingPrivileges() {
        return this == CITIZEN;
    }

    /**
     * Check if this status requires special loan terms
     */
    public boolean requiresSpecialTerms() {
        return this == TEMPORARY_RESIDENT || this == WORK_VISA || this == STUDENT_VISA;
    }

    /**
     * Get additional documentation requirements
     */
    public java.util.List<String> getDocumentationRequirements() {
        return switch (this) {
            case CITIZEN -> java.util.List.of("Government-issued ID", "Social Security verification");
            case PERMANENT_RESIDENT -> java.util.List.of("Green card", "Social Security verification");
            case TEMPORARY_RESIDENT -> java.util.List.of("Valid visa", "I-94 record", "Social Security verification");
            case WORK_VISA -> java.util.List.of("Work authorization document", "Valid visa", "Employment verification");
            case STUDENT_VISA -> java.util.List.of("Student visa", "I-20 form", "School enrollment verification");
            case TOURIST -> java.util.List.of(); // Not eligible for loans
        };
    }

    /**
     * Get maximum loan term restriction in months (null if no restriction)
     */
    public Integer getMaxLoanTermMonths() {
        return switch (this) {
            case CITIZEN, PERMANENT_RESIDENT -> null; // No restriction
            case TEMPORARY_RESIDENT, WORK_VISA -> 84; // 7 years max
            case STUDENT_VISA -> 60; // 5 years max
            case TOURIST -> 0; // Not eligible
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}