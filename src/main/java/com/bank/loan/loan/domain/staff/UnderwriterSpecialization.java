package com.bank.loanmanagement.loan.domain.staff;

public enum UnderwriterSpecialization {
    PERSONAL_LOANS("Personal Loans"),
    BUSINESS_LOANS("Business Loans"), 
    MORTGAGES("Mortgages");
    
    private final String displayName;
    
    UnderwriterSpecialization(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}