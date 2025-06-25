package com.bank.loanmanagement.ai.domain.model;

/**
 * Risk Factor enumeration for loan analysis
 */
public enum RiskFactor {
    LOW_CREDIT_SCORE("Credit score below acceptable threshold"),
    HIGH_DEBT_TO_INCOME("Debt-to-income ratio too high"),
    INSUFFICIENT_INCOME("Income insufficient for requested loan amount"),
    UNSTABLE_EMPLOYMENT("Employment history shows instability"),
    RECENT_CREDIT_INQUIRIES("Multiple recent credit inquiries detected"),
    EXISTING_DELINQUENCIES("Current delinquent accounts on credit report"),
    BANKRUPTCY_HISTORY("Previous bankruptcy on record"),
    INSUFFICIENT_CREDIT_HISTORY("Limited credit history available"),
    HIGH_LOAN_TO_VALUE("Loan-to-value ratio exceeds guidelines"),
    VOLATILE_INCOME("Income shows high volatility"),
    INDUSTRY_RISK("Employment in high-risk industry"),
    GEOGRAPHIC_RISK("Property located in declining market area"),
    LARGE_LOAN_AMOUNT("Loan amount significantly above normal range"),
    SHORT_EMPLOYMENT_TENURE("Current employment tenure too short"),
    SELF_EMPLOYMENT_RISK("Self-employed without sufficient documentation"),
    MULTIPLE_LOAN_APPLICATIONS("Multiple concurrent loan applications"),
    FRAUD_INDICATORS("Potential fraud indicators detected"),
    UNUSUAL_SPENDING_PATTERNS("Unusual spending patterns detected"),
    NEGATIVE_BANK_ACCOUNT_HISTORY("History of overdrafts or negative balances"),
    HIGH_PAYMENT_TO_INCOME("Monthly payment would be too high relative to income");

    private final String description;

    RiskFactor(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get the severity level of this risk factor (1-10, higher is more severe)
     */
    public int getSeverityLevel() {
        return switch (this) {
            case FRAUD_INDICATORS, BANKRUPTCY_HISTORY -> 10;
            case EXISTING_DELINQUENCIES, HIGH_DEBT_TO_INCOME -> 8;
            case LOW_CREDIT_SCORE, INSUFFICIENT_INCOME -> 7;
            case UNSTABLE_EMPLOYMENT, VOLATILE_INCOME -> 6;
            case HIGH_LOAN_TO_VALUE, HIGH_PAYMENT_TO_INCOME -> 6;
            case RECENT_CREDIT_INQUIRIES, MULTIPLE_LOAN_APPLICATIONS -> 5;
            case SELF_EMPLOYMENT_RISK, SHORT_EMPLOYMENT_TENURE -> 4;
            case INDUSTRY_RISK, GEOGRAPHIC_RISK -> 4;
            case LARGE_LOAN_AMOUNT, UNUSUAL_SPENDING_PATTERNS -> 3;
            case INSUFFICIENT_CREDIT_HISTORY, NEGATIVE_BANK_ACCOUNT_HISTORY -> 3;
            case MULTIPLE_LOAN_APPLICATIONS -> 2;
        };
    }

    /**
     * Check if this risk factor is considered critical (automatic denial)
     */
    public boolean isCritical() {
        return getSeverityLevel() >= 8;
    }

    /**
     * Check if this risk factor can be mitigated with conditions
     */
    public boolean canBeMitigated() {
        return switch (this) {
            case FRAUD_INDICATORS, BANKRUPTCY_HISTORY, EXISTING_DELINQUENCIES -> false;
            case LOW_CREDIT_SCORE, HIGH_DEBT_TO_INCOME, INSUFFICIENT_INCOME -> true;
            case UNSTABLE_EMPLOYMENT, SELF_EMPLOYMENT_RISK -> true;
            case HIGH_LOAN_TO_VALUE, HIGH_PAYMENT_TO_INCOME -> true;
            default -> true;
        };
    }

    /**
     * Get suggested mitigation strategy
     */
    public String getMitigationStrategy() {
        return switch (this) {
            case LOW_CREDIT_SCORE -> "Require co-signer or higher interest rate";
            case HIGH_DEBT_TO_INCOME -> "Reduce loan amount or require debt consolidation";
            case INSUFFICIENT_INCOME -> "Provide additional income documentation or reduce loan amount";
            case UNSTABLE_EMPLOYMENT -> "Require longer employment verification or job offer letter";
            case SELF_EMPLOYMENT_RISK -> "Require 2+ years tax returns and bank statements";
            case HIGH_LOAN_TO_VALUE -> "Require larger down payment or mortgage insurance";
            case HIGH_PAYMENT_TO_INCOME -> "Extend loan term or reduce loan amount";
            case RECENT_CREDIT_INQUIRIES -> "Provide explanation letters for credit inquiries";
            case INSUFFICIENT_CREDIT_HISTORY -> "Consider alternative credit data or co-signer";
            case SHORT_EMPLOYMENT_TENURE -> "Require employment verification and offer letter";
            case MULTIPLE_LOAN_APPLICATIONS -> "Clarify purpose and consolidate applications";
            case LARGE_LOAN_AMOUNT -> "Provide additional asset verification";
            case INDUSTRY_RISK -> "Require additional reserves or lower loan amount";
            case GEOGRAPHIC_RISK -> "Require property inspection and updated appraisal";
            case VOLATILE_INCOME -> "Use conservative income calculation or require reserves";
            case UNUSUAL_SPENDING_PATTERNS -> "Provide explanation and documentation";
            case NEGATIVE_BANK_ACCOUNT_HISTORY -> "Demonstrate improved financial management";
            case FRAUD_INDICATORS -> "No mitigation - requires investigation";
            case BANKRUPTCY_HISTORY -> "No mitigation - automatic denial";
            case EXISTING_DELINQUENCIES -> "No mitigation - resolve delinquencies first";
        };
    }

    /**
     * Get the category of this risk factor
     */
    public RiskCategory getCategory() {
        return switch (this) {
            case LOW_CREDIT_SCORE, INSUFFICIENT_CREDIT_HISTORY, RECENT_CREDIT_INQUIRIES,
                 EXISTING_DELINQUENCIES, BANKRUPTCY_HISTORY -> RiskCategory.CREDIT;
            case HIGH_DEBT_TO_INCOME, INSUFFICIENT_INCOME, VOLATILE_INCOME,
                 HIGH_PAYMENT_TO_INCOME -> RiskCategory.INCOME;
            case UNSTABLE_EMPLOYMENT, SHORT_EMPLOYMENT_TENURE, SELF_EMPLOYMENT_RISK,
                 INDUSTRY_RISK -> RiskCategory.EMPLOYMENT;
            case HIGH_LOAN_TO_VALUE, LARGE_LOAN_AMOUNT, GEOGRAPHIC_RISK -> RiskCategory.LOAN_TERMS;
            case FRAUD_INDICATORS, UNUSUAL_SPENDING_PATTERNS, MULTIPLE_LOAN_APPLICATIONS -> RiskCategory.BEHAVIORAL;
            case NEGATIVE_BANK_ACCOUNT_HISTORY -> RiskCategory.BANKING;
        };
    }

    public enum RiskCategory {
        CREDIT, INCOME, EMPLOYMENT, LOAN_TERMS, BEHAVIORAL, BANKING
    }
}