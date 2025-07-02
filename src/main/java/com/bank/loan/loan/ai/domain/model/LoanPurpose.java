package com.bank.loan.loan.ai.domain.model;

import java.math.BigDecimal;

/**
 * Loan Purpose enumeration for categorizing loan requests
 */
public enum LoanPurpose {
    HOME_PURCHASE("Home Purchase", true, new BigDecimal("1000000"), 360),
    HOME_REFINANCE("Home Refinance", true, new BigDecimal("1000000"), 360),
    HOME_IMPROVEMENT("Home Improvement", true, new BigDecimal("200000"), 240),
    AUTO_PURCHASE("Auto Purchase", true, new BigDecimal("100000"), 84),
    DEBT_CONSOLIDATION("Debt Consolidation", false, new BigDecimal("75000"), 84),
    BUSINESS_LOAN("Business Loan", false, new BigDecimal("500000"), 120),
    EDUCATION("Education", false, new BigDecimal("150000"), 240),
    PERSONAL("Personal", false, new BigDecimal("50000"), 60),
    MEDICAL_EXPENSES("Medical Expenses", false, new BigDecimal("75000"), 84),
    VACATION("Vacation", false, new BigDecimal("25000"), 36),
    WEDDING("Wedding", false, new BigDecimal("50000"), 60),
    EMERGENCY("Emergency", false, new BigDecimal("25000"), 36),
    INVESTMENT("Investment", false, new BigDecimal("100000"), 120),
    OTHER("Other", false, new BigDecimal("50000"), 60);

    private final String description;
    private final boolean hasCollateral;
    private final BigDecimal typicalMaxAmount;
    private final int maxTermMonths;

    LoanPurpose(String description, boolean hasCollateral, BigDecimal typicalMaxAmount, int maxTermMonths) {
        this.description = description;
        this.hasCollateral = hasCollateral;
        this.typicalMaxAmount = typicalMaxAmount;
        this.maxTermMonths = maxTermMonths;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasCollateral() {
        return hasCollateral;
    }

    public BigDecimal getTypicalMaxAmount() {
        return typicalMaxAmount;
    }

    public int getMaxTermMonths() {
        return maxTermMonths;
    }

    /**
     * Get risk level associated with loan purpose (1-10, higher is riskier)
     */
    public int getRiskLevel() {
        return switch (this) {
            case HOME_PURCHASE, HOME_REFINANCE -> 2;
            case AUTO_PURCHASE, HOME_IMPROVEMENT -> 3;
            case EDUCATION, DEBT_CONSOLIDATION -> 4;
            case BUSINESS_LOAN, MEDICAL_EXPENSES -> 5;
            case PERSONAL, INVESTMENT -> 6;
            case WEDDING, EMERGENCY -> 7;
            case VACATION -> 8;
            case OTHER -> 9;
        };
    }

    /**
     * Check if purpose typically requires income verification
     */
    public boolean requiresIncomeVerification() {
        return this == HOME_PURCHASE || this == HOME_REFINANCE || this == BUSINESS_LOAN;
    }

    /**
     * Check if purpose is considered essential
     */
    public boolean isEssentialPurpose() {
        return this == HOME_PURCHASE || this == AUTO_PURCHASE || this == EDUCATION || 
               this == MEDICAL_EXPENSES || this == EMERGENCY || this == DEBT_CONSOLIDATION;
    }

    /**
     * Get interest rate adjustment factor (multiplier for base rate)
     */
    public BigDecimal getInterestRateAdjustment() {
        return switch (this) {
            case HOME_PURCHASE, HOME_REFINANCE -> new BigDecimal("0.95"); // 5% discount
            case AUTO_PURCHASE -> new BigDecimal("0.98"); // 2% discount
            case EDUCATION, DEBT_CONSOLIDATION -> new BigDecimal("1.00"); // No adjustment
            case BUSINESS_LOAN, HOME_IMPROVEMENT -> new BigDecimal("1.02"); // 2% premium
            case PERSONAL, MEDICAL_EXPENSES -> new BigDecimal("1.05"); // 5% premium
            case INVESTMENT, WEDDING -> new BigDecimal("1.10"); // 10% premium
            case VACATION, OTHER -> new BigDecimal("1.15"); // 15% premium
            case EMERGENCY -> new BigDecimal("1.08"); // 8% premium
        };
    }
}