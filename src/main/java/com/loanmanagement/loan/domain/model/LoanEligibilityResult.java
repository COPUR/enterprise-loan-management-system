package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Objects;

/**
 * Loan Eligibility Result Value Object
 * Contains the complete eligibility assessment result for a loan application
 */
@Value
@Builder(toBuilder = true)
public class LoanEligibilityResult {
    
    boolean eligible;
    CreditScoreCheck creditScoreCheck;
    DebtToIncomeCheck debtToIncomeCheck;
    LoanToValueCheck loanToValueCheck;
    EmploymentCheck employmentCheck;
    BankingHistoryCheck bankingHistoryCheck;
    List<BusinessRuleViolation> violations;
    List<String> additionalRequirements;
    
    public LoanEligibilityResult(boolean eligible, CreditScoreCheck creditScoreCheck,
                                DebtToIncomeCheck debtToIncomeCheck, LoanToValueCheck loanToValueCheck,
                                EmploymentCheck employmentCheck, BankingHistoryCheck bankingHistoryCheck,
                                List<BusinessRuleViolation> violations, List<String> additionalRequirements) {
        
        // Validation
        Objects.requireNonNull(creditScoreCheck, "Credit score check cannot be null");
        Objects.requireNonNull(debtToIncomeCheck, "Debt-to-income check cannot be null");
        Objects.requireNonNull(loanToValueCheck, "Loan-to-value check cannot be null");
        Objects.requireNonNull(employmentCheck, "Employment check cannot be null");
        Objects.requireNonNull(bankingHistoryCheck, "Banking history check cannot be null");
        Objects.requireNonNull(violations, "Violations list cannot be null");
        Objects.requireNonNull(additionalRequirements, "Additional requirements list cannot be null");
        
        this.eligible = eligible;
        this.creditScoreCheck = creditScoreCheck;
        this.debtToIncomeCheck = debtToIncomeCheck;
        this.loanToValueCheck = loanToValueCheck;
        this.employmentCheck = employmentCheck;
        this.bankingHistoryCheck = bankingHistoryCheck;
        this.violations = List.copyOf(violations);
        this.additionalRequirements = List.copyOf(additionalRequirements);
    }
    
    /**
     * Check if all major checks passed
     */
    public boolean allMajorChecksPassed() {
        return creditScoreCheck.isPassed() && 
               debtToIncomeCheck.isPassed() && 
               loanToValueCheck.isPassed() && 
               employmentCheck.isPassed();
    }
    
    /**
     * Check if only warnings exist (no error violations)
     */
    public boolean hasOnlyWarnings() {
        return violations.stream().allMatch(v -> v.getSeverity() == ViolationSeverity.WARNING);
    }
    
    /**
     * Check if there are any error violations
     */
    public boolean hasErrorViolations() {
        return violations.stream().anyMatch(v -> v.getSeverity() == ViolationSeverity.ERROR);
    }
    
    /**
     * Get count of violations by severity
     */
    public long getViolationCount(ViolationSeverity severity) {
        return violations.stream()
                .filter(v -> v.getSeverity() == severity)
                .count();
    }
    
    /**
     * Get violations by rule type
     */
    public List<BusinessRuleViolation> getViolationsByRuleType(BusinessRuleType ruleType) {
        return violations.stream()
                .filter(v -> v.getRuleType() == ruleType)
                .toList();
    }
    
    /**
     * Get overall eligibility status message
     */
    public String getEligibilityStatusMessage() {
        if (eligible) {
            return hasOnlyWarnings() ? 
                "Eligible with conditions" : 
                "Fully eligible";
        } else {
            return "Not eligible - " + getViolationCount(ViolationSeverity.ERROR) + " error(s) found";
        }
    }
    
    /**
     * Check if additional documentation is required
     */
    public boolean requiresAdditionalDocumentation() {
        return !additionalRequirements.isEmpty();
    }
    
    /**
     * Get summary of all check results
     */
    public String getCheckSummary() {
        return String.format(
            "Credit Score: %s, DTI: %s, LTV: %s, Employment: %s, Banking History: %s",
            creditScoreCheck.isPassed() ? "PASS" : "FAIL",
            debtToIncomeCheck.isPassed() ? "PASS" : "FAIL",
            loanToValueCheck.isPassed() ? "PASS" : "FAIL",
            employmentCheck.isPassed() ? "PASS" : "FAIL",
            bankingHistoryCheck.isPassed() ? "PASS" : "FAIL"
        );
    }
}