package com.banking.loan.domain.loan;

/**
 * Value Object representing loan rejection reasons
 * Follows DDD principles for domain modeling
 */
public record RejectionReason(
    String code,
    String description,
    String category,
    boolean appealable
) {
    
    public static RejectionReason creditScore(String details) {
        return new RejectionReason(
            "CREDIT_SCORE",
            "Insufficient credit score: " + details,
            "CREDIT",
            true
        );
    }
    
    public static RejectionReason insufficientIncome(String details) {
        return new RejectionReason(
            "INSUFFICIENT_INCOME",
            "Insufficient income: " + details,
            "FINANCIAL",
            true
        );
    }
    
    public static RejectionReason complianceViolation(String details) {
        return new RejectionReason(
            "COMPLIANCE_VIOLATION",
            "Compliance violation: " + details,
            "COMPLIANCE",
            false
        );
    }
    
    public static RejectionReason fraudSuspected(String details) {
        return new RejectionReason(
            "FRAUD_SUSPECTED",
            "Fraud indicators detected: " + details,
            "FRAUD",
            false
        );
    }
}