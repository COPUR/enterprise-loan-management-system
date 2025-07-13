package com.bank.loan.domain.model;

/**
 * Enumeration representing different types of payments in the loan system
 */
public enum PaymentType {
    REGULAR_INSTALLMENT("Regular Installment"),
    EARLY_PAYMENT("Early Payment"),
    PARTIAL_PAYMENT("Partial Payment"),
    FULL_PREPAYMENT("Full Prepayment"),
    LATE_FEE("Late Fee"),
    PENALTY_PAYMENT("Penalty Payment"),
    INTEREST_ONLY("Interest Only"),
    PRINCIPAL_ONLY("Principal Only"),
    RESTRUCTURE_PAYMENT("Restructure Payment"),
    SETTLEMENT_PAYMENT("Settlement Payment");
    
    private final String displayName;
    
    PaymentType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean affectsOutstandingBalance() {
        return this != LATE_FEE && this != PENALTY_PAYMENT;
    }
    
    public boolean isPrincipalReduction() {
        return this == REGULAR_INSTALLMENT || this == EARLY_PAYMENT || 
               this == PARTIAL_PAYMENT || this == FULL_PREPAYMENT || 
               this == PRINCIPAL_ONLY || this == SETTLEMENT_PAYMENT;
    }
}