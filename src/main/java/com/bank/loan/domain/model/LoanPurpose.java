package com.bank.loan.domain.model;

/**
 * Enumeration representing different purposes for taking a loan
 */
public enum LoanPurpose {
    HOME_PURCHASE("Home Purchase"),
    HOME_IMPROVEMENT("Home Improvement"),
    DEBT_CONSOLIDATION("Debt Consolidation"),
    VEHICLE_PURCHASE("Vehicle Purchase"),
    BUSINESS_EXPANSION("Business Expansion"),
    EQUIPMENT_PURCHASE("Equipment Purchase"),
    EDUCATION("Education"),
    MEDICAL_EXPENSES("Medical Expenses"),
    VACATION("Vacation"),
    WEDDING("Wedding"),
    INVESTMENT("Investment"),
    OTHER("Other");
    
    private final String displayName;
    
    LoanPurpose(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}