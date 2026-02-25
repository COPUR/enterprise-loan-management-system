package com.loanmanagement.loan.domain.model;

/**
 * Enumeration of installment modification types.
 */
public enum InstallmentModificationType {
    
    /**
     * Payment amount modification.
     */
    PAYMENT_AMOUNT_CHANGE("Payment Amount Change"),
    
    /**
     * Interest rate modification.
     */
    INTEREST_RATE_CHANGE("Interest Rate Change"),
    
    /**
     * Payment due date modification.
     */
    DUE_DATE_CHANGE("Due Date Change"),
    
    /**
     * Payment frequency modification.
     */
    PAYMENT_FREQUENCY_CHANGE("Payment Frequency Change"),
    
    /**
     * Principal amount modification.
     */
    PRINCIPAL_AMOUNT_CHANGE("Principal Amount Change"),
    
    /**
     * Interest amount modification.
     */
    INTEREST_AMOUNT_CHANGE("Interest Amount Change"),
    
    /**
     * Fees modification.
     */
    FEES_CHANGE("Fees Change"),
    
    /**
     * Escrow amount modification.
     */
    ESCROW_AMOUNT_CHANGE("Escrow Amount Change"),
    
    /**
     * Term extension modification.
     */
    TERM_EXTENSION("Term Extension"),
    
    /**
     * Term reduction modification.
     */
    TERM_REDUCTION("Term Reduction"),
    
    /**
     * Payment deferral modification.
     */
    PAYMENT_DEFERRAL("Payment Deferral"),
    
    /**
     * Payment skip modification.
     */
    PAYMENT_SKIP("Payment Skip"),
    
    /**
     * Late fee waiver modification.
     */
    LATE_FEE_WAIVER("Late Fee Waiver"),
    
    /**
     * Late fee addition modification.
     */
    LATE_FEE_ADDITION("Late Fee Addition"),
    
    /**
     * Payment allocation modification.
     */
    PAYMENT_ALLOCATION_CHANGE("Payment Allocation Change"),
    
    /**
     * Forbearance modification.
     */
    FORBEARANCE("Forbearance"),
    
    /**
     * Refinancing modification.
     */
    REFINANCING("Refinancing"),
    
    /**
     * Restructuring modification.
     */
    RESTRUCTURING("Restructuring"),
    
    /**
     * Workout modification.
     */
    WORKOUT("Workout"),
    
    /**
     * Modification reversal.
     */
    MODIFICATION_REVERSAL("Modification Reversal"),
    
    /**
     * Temporary modification.
     */
    TEMPORARY_MODIFICATION("Temporary Modification"),
    
    /**
     * Permanent modification.
     */
    PERMANENT_MODIFICATION("Permanent Modification"),
    
    /**
     * Trial modification.
     */
    TRIAL_MODIFICATION("Trial Modification"),
    
    /**
     * Rate adjustment modification.
     */
    RATE_ADJUSTMENT("Rate Adjustment"),
    
    /**
     * Balloon payment modification.
     */
    BALLOON_PAYMENT_MODIFICATION("Balloon Payment Modification"),
    
    /**
     * Graduated payment modification.
     */
    GRADUATED_PAYMENT_MODIFICATION("Graduated Payment Modification"),
    
    /**
     * Interest-only modification.
     */
    INTEREST_ONLY_MODIFICATION("Interest-Only Modification"),
    
    /**
     * Amortization schedule modification.
     */
    AMORTIZATION_SCHEDULE_CHANGE("Amortization Schedule Change"),
    
    /**
     * Payment method modification.
     */
    PAYMENT_METHOD_CHANGE("Payment Method Change"),
    
    /**
     * Administrative modification.
     */
    ADMINISTRATIVE_CHANGE("Administrative Change"),
    
    /**
     * Regulatory compliance modification.
     */
    REGULATORY_COMPLIANCE("Regulatory Compliance"),
    
    /**
     * Error correction modification.
     */
    ERROR_CORRECTION("Error Correction"),
    
    /**
     * Customer request modification.
     */
    CUSTOMER_REQUEST("Customer Request"),
    
    /**
     * System-initiated modification.
     */
    SYSTEM_INITIATED("System Initiated"),
    
    /**
     * Court-ordered modification.
     */
    COURT_ORDERED("Court Ordered"),
    
    /**
     * Bankruptcy modification.
     */
    BANKRUPTCY("Bankruptcy"),
    
    /**
     * Hardship modification.
     */
    HARDSHIP("Hardship"),
    
    /**
     * Other modification type.
     */
    OTHER("Other");
    
    private final String description;
    
    InstallmentModificationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this modification type affects payment amount.
     */
    public boolean affectsPaymentAmount() {
        return this == PAYMENT_AMOUNT_CHANGE ||
               this == INTEREST_RATE_CHANGE ||
               this == PRINCIPAL_AMOUNT_CHANGE ||
               this == INTEREST_AMOUNT_CHANGE ||
               this == FEES_CHANGE ||
               this == ESCROW_AMOUNT_CHANGE ||
               this == LATE_FEE_ADDITION ||
               this == LATE_FEE_WAIVER;
    }
    
    /**
     * Checks if this modification type affects payment schedule.
     */
    public boolean affectsPaymentSchedule() {
        return this == DUE_DATE_CHANGE ||
               this == PAYMENT_FREQUENCY_CHANGE ||
               this == TERM_EXTENSION ||
               this == TERM_REDUCTION ||
               this == PAYMENT_DEFERRAL ||
               this == PAYMENT_SKIP ||
               this == AMORTIZATION_SCHEDULE_CHANGE;
    }
    
    /**
     * Checks if this modification type requires customer notification.
     */
    public boolean requiresCustomerNotification() {
        return this == PAYMENT_AMOUNT_CHANGE ||
               this == INTEREST_RATE_CHANGE ||
               this == DUE_DATE_CHANGE ||
               this == PAYMENT_FREQUENCY_CHANGE ||
               this == TERM_EXTENSION ||
               this == TERM_REDUCTION ||
               this == REFINANCING ||
               this == RESTRUCTURING ||
               this == RATE_ADJUSTMENT ||
               this == BALLOON_PAYMENT_MODIFICATION ||
               this == GRADUATED_PAYMENT_MODIFICATION ||
               this == INTEREST_ONLY_MODIFICATION;
    }
    
    /**
     * Checks if this modification type requires regulatory compliance review.
     */
    public boolean requiresComplianceReview() {
        return this == REFINANCING ||
               this == RESTRUCTURING ||
               this == WORKOUT ||
               this == BALLOON_PAYMENT_MODIFICATION ||
               this == GRADUATED_PAYMENT_MODIFICATION ||
               this == INTEREST_ONLY_MODIFICATION ||
               this == REGULATORY_COMPLIANCE;
    }
    
    /**
     * Checks if this modification type is temporary.
     */
    public boolean isTemporary() {
        return this == TEMPORARY_MODIFICATION ||
               this == TRIAL_MODIFICATION ||
               this == PAYMENT_DEFERRAL ||
               this == PAYMENT_SKIP ||
               this == FORBEARANCE;
    }
    
    /**
     * Checks if this modification type is permanent.
     */
    public boolean isPermanent() {
        return this == PERMANENT_MODIFICATION ||
               this == REFINANCING ||
               this == RESTRUCTURING ||
               this == TERM_EXTENSION ||
               this == TERM_REDUCTION ||
               this == INTEREST_RATE_CHANGE;
    }
}