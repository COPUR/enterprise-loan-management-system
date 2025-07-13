package com.bank.loan.domain.model;

/**
 * Enumeration of supported loan types in the system
 */
public enum LoanType {
    PERSONAL("Personal Loan", false),
    MORTGAGE("Mortgage Loan", true), 
    BUSINESS("Business Loan", true),
    AUTO("Auto Loan", true),
    EDUCATION("Education Loan", false),
    HOME_EQUITY("Home Equity Loan", true),
    ISLAMIC_MURABAHA("Islamic Murabaha", true),
    ISLAMIC_IJARA("Islamic Ijara", true);
    
    private final String displayName;
    private final boolean securedLoan;
    
    LoanType(String displayName, boolean securedLoan) {
        this.displayName = displayName;
        this.securedLoan = securedLoan;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public boolean isSecuredLoan() {
        return securedLoan;
    }
    
    public boolean requiresCollateral() {
        return securedLoan;
    }
}