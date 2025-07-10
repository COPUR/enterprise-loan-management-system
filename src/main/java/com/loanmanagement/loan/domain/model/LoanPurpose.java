package com.loanmanagement.loan.domain.model;

/**
 * Loan Purpose Enumeration
 * Defines the intended use of loan funds
 */
public enum LoanPurpose {
    /**
     * Home mortgage loan
     */
    HOME("Home/Mortgage", "Purchase or refinance of residential property"),
    
    /**
     * Vehicle financing
     */
    AUTO("Auto/Vehicle", "Purchase of new or used vehicle"),
    
    /**
     * Personal loan for general purposes
     */
    PERSONAL("Personal", "Personal expenses and general use"),
    
    /**
     * Business loan for commercial purposes
     */
    BUSINESS("Business", "Business operations and commercial use"),
    
    /**
     * Education loan for tuition and related expenses
     */
    EDUCATION("Education", "Educational expenses and student loans"),
    
    /**
     * Debt consolidation loan
     */
    DEBT_CONSOLIDATION("Debt Consolidation", "Consolidation of existing debts"),
    
    /**
     * Home improvement and renovation
     */
    HOME_IMPROVEMENT("Home Improvement", "Property improvements and renovations"),
    
    /**
     * Medical expenses
     */
    MEDICAL("Medical", "Healthcare and medical expenses"),
    
    /**
     * Wedding and special events
     */
    WEDDING("Wedding", "Wedding and special event expenses"),
    
    /**
     * Vacation and travel
     */
    VACATION("Vacation", "Travel and vacation expenses"),
    
    /**
     * Investment purposes
     */
    INVESTMENT("Investment", "Investment and wealth building"),
    
    /**
     * Emergency expenses
     */
    EMERGENCY("Emergency", "Emergency and unexpected expenses");

    private final String displayName;
    private final String description;

    LoanPurpose(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this purpose is for a secured loan (typically requires collateral)
     */
    public boolean isSecuredLoanPurpose() {
        return this == HOME || this == AUTO || this == BUSINESS;
    }

    /**
     * Check if this purpose is for an unsecured loan
     */
    public boolean isUnsecuredLoanPurpose() {
        return !isSecuredLoanPurpose();
    }

    /**
     * Check if this purpose typically has lower interest rates
     */
    public boolean hasLowerRates() {
        return this == HOME || this == EDUCATION;
    }

    /**
     * Get typical loan term range in months
     */
    public String getTypicalTermRange() {
        return switch (this) {
            case HOME -> "180-360 months";
            case AUTO -> "36-84 months";
            case BUSINESS -> "12-120 months";
            case EDUCATION -> "60-240 months";
            case DEBT_CONSOLIDATION -> "24-84 months";
            case HOME_IMPROVEMENT -> "24-180 months";
            case PERSONAL, MEDICAL, WEDDING, VACATION, EMERGENCY -> "12-60 months";
            case INVESTMENT -> "12-240 months";
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}