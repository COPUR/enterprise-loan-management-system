package com.bank.loan.loan.ai.application.port.in;

import com.bank.loan.loan.ai.domain.model.EmploymentType;
import com.bank.loan.loan.ai.domain.model.LoanPurpose;
import com.bank.loan.loan.sharedkernel.application.command.Command;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Command to analyze a loan request using AI
 */
@Value
public class AnalyzeLoanRequestCommand implements Command {
    
    String requestId;
    BigDecimal requestedAmount;
    String applicantName;
    String applicantId;
    BigDecimal monthlyIncome;
    BigDecimal monthlyExpenses;
    EmploymentType employmentType;
    Integer employmentTenureMonths;
    LoanPurpose loanPurpose;
    Integer requestedTermMonths;
    BigDecimal currentDebt;
    Integer creditScore;
    String naturalLanguageRequest;
    Map<String, Object> additionalData;

    /**
     * Check if this is a natural language request (requires NLP processing)
     */
    public boolean requiresNlpProcessing() {
        return naturalLanguageRequest != null && !naturalLanguageRequest.trim().isEmpty();
    }

    /**
     * Check if all required structured data is provided
     */
    public boolean hasCompleteStructuredData() {
        return requestedAmount != null &&
               applicantName != null && !applicantName.trim().isEmpty() &&
               applicantId != null && !applicantId.trim().isEmpty() &&
               monthlyIncome != null &&
               employmentType != null &&
               loanPurpose != null;
    }

    /**
     * Calculate debt-to-income ratio if data is available
     */
    public BigDecimal calculateDebtToIncomeRatio() {
        if (currentDebt != null && monthlyIncome != null && monthlyIncome.compareTo(BigDecimal.ZERO) > 0) {
            return currentDebt.divide(monthlyIncome, 4, BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }
}