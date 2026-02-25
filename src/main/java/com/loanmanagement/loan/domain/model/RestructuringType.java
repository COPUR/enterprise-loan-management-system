package com.loanmanagement.loan.domain.model;

/**
 * Restructuring Type Enumeration
 * Defines the various types of loan restructuring
 */
public enum RestructuringType {
    /**
     * Modification of payment amount or schedule
     */
    PAYMENT_MODIFICATION("Payment Modification", "Changes to payment amount or frequency"),
    
    /**
     * Extension of loan term
     */
    TERM_EXTENSION("Term Extension", "Extending the loan maturity date"),
    
    /**
     * Modification of interest rate
     */
    RATE_MODIFICATION("Rate Modification", "Changes to interest rate"),
    
    /**
     * Temporary deferral of payments
     */
    PAYMENT_DEFERRAL("Payment Deferral", "Temporary suspension of payments"),
    
    /**
     * Principal reduction or forgiveness
     */
    PRINCIPAL_REDUCTION("Principal Reduction", "Reduction of outstanding principal"),
    
    /**
     * Modification due to financial hardship
     */
    HARDSHIP_MODIFICATION("Hardship Modification", "Modification due to borrower hardship"),
    
    /**
     * Modification required by regulation
     */
    REGULATORY_MODIFICATION("Regulatory Modification", "Modification required by regulation"),
    
    /**
     * Comprehensive restructuring of all terms
     */
    COMPREHENSIVE_RESTRUCTURING("Comprehensive Restructuring", "Complete restructuring of loan terms");

    private final String displayName;
    private final String description;

    RestructuringType(String displayName, String description) {
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
     * Check if this restructuring type affects payment amount
     */
    public boolean affectsPaymentAmount() {
        return this == PAYMENT_MODIFICATION || 
               this == TERM_EXTENSION || 
               this == RATE_MODIFICATION ||
               this == PRINCIPAL_REDUCTION ||
               this == COMPREHENSIVE_RESTRUCTURING;
    }

    /**
     * Check if this restructuring type is temporary
     */
    public boolean isTemporary() {
        return this == PAYMENT_DEFERRAL || this == HARDSHIP_MODIFICATION;
    }

    /**
     * Check if this restructuring type requires regulatory approval
     */
    public boolean requiresRegulatoryApproval() {
        return this == PRINCIPAL_REDUCTION || 
               this == COMPREHENSIVE_RESTRUCTURING ||
               this == REGULATORY_MODIFICATION;
    }

    /**
     * Check if this restructuring type improves borrower's position
     */
    public boolean improvesBorrowerPosition() {
        return this == RATE_MODIFICATION || 
               this == PAYMENT_DEFERRAL ||
               this == PRINCIPAL_REDUCTION ||
               this == HARDSHIP_MODIFICATION;
    }

    /**
     * Get typical approval timeframe in days
     */
    public int getTypicalApprovalDays() {
        return switch (this) {
            case PAYMENT_MODIFICATION, RATE_MODIFICATION -> 7;
            case TERM_EXTENSION, PAYMENT_DEFERRAL -> 5;
            case HARDSHIP_MODIFICATION -> 14;
            case PRINCIPAL_REDUCTION -> 30;
            case REGULATORY_MODIFICATION -> 21;
            case COMPREHENSIVE_RESTRUCTURING -> 45;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}