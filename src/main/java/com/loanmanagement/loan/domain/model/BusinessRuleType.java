package com.loanmanagement.loan.domain.model;

/**
 * Business Rule Type Enumeration
 * Defines the types of business rules that can be violated during loan assessment
 */
public enum BusinessRuleType {
    
    // Credit Assessment Rules
    CREDIT_SCORE_MINIMUM("CREDIT_001", "Credit Score Minimum", "Credit Assessment", 
                         "Minimum credit score requirement not met"),
    
    // Debt and Income Rules
    DEBT_TO_INCOME_RATIO("DTI_001", "Debt-to-Income Ratio", "Financial Assessment",
                         "Debt-to-income ratio exceeds maximum allowed"),
    
    // Collateral Rules
    LOAN_TO_VALUE_RATIO("LTV_001", "Loan-to-Value Ratio", "Collateral Assessment",
                        "Loan-to-value ratio exceeds maximum allowed"),
    
    // Employment Rules
    EMPLOYMENT_STABILITY("EMP_001", "Employment Stability", "Employment Assessment",
                        "Employment requirements not met"),
    
    // Banking History Rules
    BANKING_HISTORY("BANK_001", "Banking History", "Banking Assessment",
                   "Banking history requirements not met"),
    
    // Loan Amount Rules
    MINIMUM_LOAN_AMOUNT("AMOUNT_001", "Minimum Loan Amount", "Loan Parameters",
                       "Requested amount below minimum threshold"),
    
    MAXIMUM_LOAN_AMOUNT("AMOUNT_002", "Maximum Loan Amount", "Loan Parameters",
                       "Requested amount exceeds maximum threshold"),
    
    // Personal Requirements
    MINIMUM_AGE("AGE_001", "Minimum Age", "Personal Requirements",
               "Applicant does not meet minimum age requirement"),
    
    RESIDENCY_REQUIREMENT("RES_001", "Residency Requirement", "Personal Requirements",
                         "Residency status does not meet eligibility criteria"),
    
    // Documentation Rules
    DOCUMENTATION_INCOMPLETE("DOC_001", "Documentation Incomplete", "Documentation",
                            "Required documentation is missing or incomplete"),
    
    INCOME_VERIFICATION("DOC_002", "Income Verification", "Documentation",
                       "Income verification documentation required"),
    
    // Risk Assessment Rules
    RISK_SCORE_THRESHOLD("RISK_001", "Risk Score Threshold", "Risk Assessment",
                        "Overall risk score exceeds acceptable threshold"),
    
    FRAUD_INDICATOR("FRAUD_001", "Fraud Indicator", "Risk Assessment",
                   "Potential fraud indicators detected"),
    
    // Regulatory Compliance
    REGULATORY_COMPLIANCE("REG_001", "Regulatory Compliance", "Compliance",
                         "Regulatory compliance requirements not met"),
    
    AML_CHECK("AML_001", "Anti-Money Laundering", "Compliance",
             "Anti-money laundering checks failed"),
    
    // Policy Rules
    POLICY_VIOLATION("POL_001", "Policy Violation", "Policy",
                    "Internal policy requirements not met"),
    
    LENDING_LIMIT("POL_002", "Lending Limit", "Policy",
                 "Lending limits exceeded");

    private final String code;
    private final String displayName;
    private final String category;
    private final String description;

    BusinessRuleType(String code, String displayName, String category, String description) {
        this.code = code;
        this.displayName = displayName;
        this.category = category;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get rule type by code
     */
    public static BusinessRuleType fromCode(String code) {
        for (BusinessRuleType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown business rule type code: " + code);
    }

    /**
     * Check if this rule type is related to financial assessment
     */
    public boolean isFinancialAssessment() {
        return category.equals("Financial Assessment") || 
               category.equals("Credit Assessment") || 
               category.equals("Collateral Assessment");
    }

    /**
     * Check if this rule type is related to compliance
     */
    public boolean isComplianceRule() {
        return category.equals("Compliance");
    }

    /**
     * Check if this rule type is related to risk assessment
     */
    public boolean isRiskAssessment() {
        return category.equals("Risk Assessment");
    }

    /**
     * Check if this rule type is related to documentation
     */
    public boolean isDocumentationRule() {
        return category.equals("Documentation");
    }

    /**
     * Check if this rule type is typically blocking (prevents loan approval)
     */
    public boolean isTypicallyBlocking() {
        return switch (this) {
            case CREDIT_SCORE_MINIMUM, DEBT_TO_INCOME_RATIO, LOAN_TO_VALUE_RATIO,
                 MINIMUM_AGE, RESIDENCY_REQUIREMENT, FRAUD_INDICATOR,
                 AML_CHECK, REGULATORY_COMPLIANCE -> true;
            default -> false;
        };
    }

    /**
     * Get default severity for this rule type
     */
    public ViolationSeverity getDefaultSeverity() {
        return isTypicallyBlocking() ? ViolationSeverity.ERROR : ViolationSeverity.WARNING;
    }

    /**
     * Get remediation advice for this rule type
     */
    public String getRemediationAdvice() {
        return switch (this) {
            case CREDIT_SCORE_MINIMUM -> "Improve credit score by paying down debts and maintaining good payment history";
            case DEBT_TO_INCOME_RATIO -> "Reduce monthly debt obligations or increase income";
            case LOAN_TO_VALUE_RATIO -> "Increase down payment or reduce loan amount";
            case EMPLOYMENT_STABILITY -> "Provide additional employment verification or wait for longer employment history";
            case BANKING_HISTORY -> "Establish longer banking relationship or provide additional financial references";
            case MINIMUM_LOAN_AMOUNT -> "Increase loan amount to meet minimum requirements";
            case MAXIMUM_LOAN_AMOUNT -> "Reduce loan amount to within acceptable limits";
            case MINIMUM_AGE -> "Applicant must meet minimum age requirement";
            case RESIDENCY_REQUIREMENT -> "Provide documentation of acceptable residency status";
            case DOCUMENTATION_INCOMPLETE -> "Provide all required documentation";
            case INCOME_VERIFICATION -> "Provide additional income verification documents";
            default -> "Contact loan officer for specific guidance";
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}