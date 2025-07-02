package com.bank.loanmanagement.loan.domain.application;

/**
 * Loan type enumeration
 */
public enum LoanType {
    PERSONAL("Personal Loan"),
    BUSINESS("Business Loan"),
    MORTGAGE("Mortgage"),
    AUTO_LOAN("Auto Loan");
    
    private final String displayName;
    
    LoanType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}