package com.loanmanagement.loan.application.port.out;

import com.loanmanagement.loan.application.port.in.GetLoanQuery.LoanEligibilityQuery;
import com.loanmanagement.loan.application.port.in.GetLoanQuery.LoanEligibilityResult;

/**
 * Outbound Port for Loan Eligibility Assessment
 * Abstracts external credit checking and eligibility assessment services
 */
public interface LoanEligibilityPort {
    
    /**
     * Assess loan eligibility for a customer
     */
    LoanEligibilityResult assessEligibility(LoanEligibilityQuery query);
    
    /**
     * Get credit score for a customer
     */
    CreditScoreResult getCreditScore(Long customerId);
    
    /**
     * Validate customer income
     */
    IncomeValidationResult validateIncome(Long customerId, java.math.BigDecimal declaredIncome);
    
    record CreditScoreResult(
            Long customerId,
            Integer creditScore,
            String creditRating,
            java.time.LocalDateTime assessmentDate,
            String assessmentProvider
    ) {}
    
    record IncomeValidationResult(
            boolean valid,
            java.math.BigDecimal verifiedIncome,
            String validationMethod,
            java.time.LocalDateTime validationDate
    ) {}
}