package com.loanmanagement.loan.domain.model;

/**
 * Default Type Enumeration
 * Defines the various types of loan defaults
 */
public enum DefaultType {
    /**
     * Default due to missed payments
     */
    PAYMENT_DEFAULT("Payment Default", "Failure to make required payments", 10),
    
    /**
     * Default due to violation of loan covenants
     */
    COVENANT_DEFAULT("Covenant Default", "Violation of loan agreement terms", 15),
    
    /**
     * Default due to declared financial hardship
     */
    HARDSHIP_DEFAULT("Hardship Default", "Financial hardship declared by borrower", 5),
    
    /**
     * Default due to bankruptcy filing
     */
    BANKRUPTCY_DEFAULT("Bankruptcy Default", "Borrower filed for bankruptcy", 20),
    
    /**
     * Default due to fraudulent activity
     */
    FRAUD_DEFAULT("Fraud Default", "Fraudulent activity detected", 20);

    private final String displayName;
    private final String description;
    private final int severityWeight;

    DefaultType(String displayName, String description, int severityWeight) {
        this.displayName = displayName;
        this.description = description;
        this.severityWeight = severityWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getSeverityWeight() {
        return severityWeight;
    }

    /**
     * Check if this default type is curable (can be resolved)
     */
    public boolean isCurable() {
        return this == PAYMENT_DEFAULT || this == HARDSHIP_DEFAULT;
    }

    /**
     * Check if this default type requires immediate legal action
     */
    public boolean requiresLegalAction() {
        return this == FRAUD_DEFAULT || this == BANKRUPTCY_DEFAULT;
    }

    /**
     * Check if this default type allows for restructuring
     */
    public boolean allowsRestructuring() {
        return this == PAYMENT_DEFAULT || this == HARDSHIP_DEFAULT;
    }

    /**
     * Get standard cure period in days
     */
    public int getStandardCurePeriodDays() {
        return switch (this) {
            case PAYMENT_DEFAULT -> 30;
            case COVENANT_DEFAULT -> 15;
            case HARDSHIP_DEFAULT -> 60;
            case BANKRUPTCY_DEFAULT -> 0; // No cure period
            case FRAUD_DEFAULT -> 0; // No cure period
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}