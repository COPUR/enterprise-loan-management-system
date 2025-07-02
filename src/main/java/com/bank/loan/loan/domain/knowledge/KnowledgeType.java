package com.bank.loanmanagement.loan.domain.knowledge;

public enum KnowledgeType {
    POLICY("Banking Policy"),
    REGULATION("Banking Regulation"),
    PROCEDURE("Banking Procedure"),
    FAQ("Frequently Asked Questions"),
    BUSINESS_RULE("Business Rule"),
    LOAN_CRITERIA("Loan Criteria"),
    RISK_ASSESSMENT("Risk Assessment"),
    COMPLIANCE("Compliance Requirement"),
    PRODUCT_INFO("Product Information"),
    MARKET_DATA("Market Data"),
    CUSTOMER_GUIDANCE("Customer Guidance"),
    TECHNICAL_SPEC("Technical Specification");
    
    private final String description;
    
    KnowledgeType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}