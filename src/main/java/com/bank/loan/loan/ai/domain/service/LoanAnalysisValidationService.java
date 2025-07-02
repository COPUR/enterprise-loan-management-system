package com.bank.loan.loan.ai.domain.service;

import com.bank.loan.loan.ai.domain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain service for loan analysis validation and business rule enforcement
 * Acts as guardrails for AI analysis within the domain
 * Following DDD Domain Service pattern
 */
@Service
@Slf4j
public class LoanAnalysisValidationService {

    private static final BigDecimal MAX_DEBT_TO_INCOME_RATIO = new BigDecimal("0.50"); // 50%
    private static final BigDecimal MIN_ACCEPTABLE_CREDIT_SCORE = new BigDecimal("580");
    private static final BigDecimal MAX_LOAN_TO_INCOME_RATIO = new BigDecimal("6.0"); // 6 times annual income
    private static final BigDecimal MIN_EMPLOYMENT_MONTHS = new BigDecimal("6");

    /**
     * Validate AI analysis result against business rules and regulatory requirements
     * This acts as a guardrail to ensure AI recommendations comply with business policies
     */
    public LoanAnalysisValidationResult validateAnalysisResult(LoanAnalysisRequest request, 
                                                              LoanAnalysisResult result) {
        log.debug("Validating AI analysis result for request: {}", request.getId());
        
        List<ValidationIssue> issues = new ArrayList<>();
        List<RiskFactor> additionalRiskFactors = new ArrayList<>();
        
        // Validate debt-to-income ratio
        validateDebtToIncomeRatio(request, result, issues, additionalRiskFactors);
        
        // Validate loan-to-income ratio
        validateLoanToIncomeRatio(request, result, issues, additionalRiskFactors);
        
        // Validate employment stability
        validateEmploymentStability(request, issues, additionalRiskFactors);
        
        // Validate credit score requirements
        validateCreditScore(request, result, issues, additionalRiskFactors);
        
        // Validate loan purpose appropriateness
        validateLoanPurpose(request, result, issues);
        
        // Validate AI confidence levels
        validateAiConfidence(result, issues);
        
        // Check for regulatory compliance
        validateRegulatoryCompliance(request, result, issues);
        
        boolean isValid = issues.stream().noneMatch(issue -> issue.severity() == ValidationSeverity.CRITICAL);
        
        log.debug("Validation completed for request: {} - Valid: {}, Issues: {}", 
                request.getId(), isValid, issues.size());
        
        return new LoanAnalysisValidationResult(isValid, issues, additionalRiskFactors);
    }

    /**
     * Validate business rules before AI analysis
     * Pre-analysis guardrails
     */
    public BusinessRuleValidationResult validateBusinessRules(LoanAnalysisRequest request) {
        log.debug("Validating business rules for request: {}", request.getId());
        
        List<String> violations = new ArrayList<>();
        
        // Minimum loan amount check
        if (request.getRequestedAmount() != null && 
            request.getRequestedAmount().compareTo(new BigDecimal("1000")) < 0) {
            violations.add("Requested loan amount below minimum threshold ($1,000)");
        }
        
        // Maximum loan amount check
        if (request.getRequestedAmount() != null && 
            request.getRequestedAmount().compareTo(new BigDecimal("10000000")) > 0) {
            violations.add("Requested loan amount exceeds maximum threshold ($10,000,000)");
        }
        
        // Employment tenure for certain loan types
        if (request.getLoanPurpose() == LoanPurpose.HOME_PURCHASE && 
            request.getEmploymentTenureMonths() != null &&
            request.getEmploymentTenureMonths() < 12) {
            violations.add("Home purchase loans require minimum 12 months employment history");
        }
        
        // Self-employment additional requirements
        if (request.getEmploymentType() == EmploymentType.SELF_EMPLOYED &&
            request.getRequestedAmount() != null &&
            request.getRequestedAmount().compareTo(new BigDecimal("100000")) > 0) {
            violations.add("Self-employed applicants for loans > $100,000 require additional documentation");
        }
        
        boolean isValid = violations.isEmpty();
        
        log.debug("Business rule validation completed for request: {} - Valid: {}, Violations: {}", 
                request.getId(), isValid, violations.size());
        
        return new BusinessRuleValidationResult(isValid, violations);
    }

    private void validateDebtToIncomeRatio(LoanAnalysisRequest request, LoanAnalysisResult result,
                                         List<ValidationIssue> issues, List<RiskFactor> riskFactors) {
        if (result.getDebtToIncomeRatio() != null && 
            result.getDebtToIncomeRatio().compareTo(MAX_DEBT_TO_INCOME_RATIO) > 0) {
            
            issues.add(new ValidationIssue(
                ValidationSeverity.HIGH,
                "Debt-to-income ratio exceeds maximum allowed (50%): " + result.getDebtToIncomeRatio(),
                "Consider reducing loan amount or requiring debt consolidation"
            ));
            riskFactors.add(RiskFactor.HIGH_DEBT_TO_INCOME);
        }
    }

    private void validateLoanToIncomeRatio(LoanAnalysisRequest request, LoanAnalysisResult result,
                                         List<ValidationIssue> issues, List<RiskFactor> riskFactors) {
        if (request.getMonthlyIncome() != null && request.getRequestedAmount() != null) {
            BigDecimal annualIncome = request.getMonthlyIncome().multiply(new BigDecimal("12"));
            BigDecimal loanToIncomeRatio = request.getRequestedAmount().divide(annualIncome, 2, BigDecimal.ROUND_HALF_UP);
            
            if (loanToIncomeRatio.compareTo(MAX_LOAN_TO_INCOME_RATIO) > 0) {
                issues.add(new ValidationIssue(
                    ValidationSeverity.HIGH,
                    "Loan-to-income ratio exceeds guidelines: " + loanToIncomeRatio,
                    "Consider reducing loan amount to align with income capacity"
                ));
                riskFactors.add(RiskFactor.LARGE_LOAN_AMOUNT);
            }
        }
    }

    private void validateEmploymentStability(LoanAnalysisRequest request, List<ValidationIssue> issues,
                                           List<RiskFactor> riskFactors) {
        if (request.getEmploymentType() == EmploymentType.UNEMPLOYED) {
            issues.add(new ValidationIssue(
                ValidationSeverity.CRITICAL,
                "Unemployed applicants are not eligible for loans",
                "Require proof of employment or alternative income source"
            ));
            riskFactors.add(RiskFactor.UNSTABLE_EMPLOYMENT);
        }
        
        if (request.getEmploymentTenureMonths() != null && 
            request.getEmploymentTenureMonths() < MIN_EMPLOYMENT_MONTHS.intValue()) {
            issues.add(new ValidationIssue(
                ValidationSeverity.MEDIUM,
                "Employment tenure below minimum requirement: " + request.getEmploymentTenureMonths() + " months",
                "Consider requiring employment verification or job offer letter"
            ));
            riskFactors.add(RiskFactor.SHORT_EMPLOYMENT_TENURE);
        }
    }

    private void validateCreditScore(LoanAnalysisRequest request, LoanAnalysisResult result,
                                   List<ValidationIssue> issues, List<RiskFactor> riskFactors) {
        if (request.getCreditScore() != null && 
            request.getCreditScore() < MIN_ACCEPTABLE_CREDIT_SCORE.intValue()) {
            issues.add(new ValidationIssue(
                ValidationSeverity.HIGH,
                "Credit score below minimum acceptable threshold: " + request.getCreditScore(),
                "Consider requiring co-signer or higher interest rate"
            ));
            riskFactors.add(RiskFactor.LOW_CREDIT_SCORE);
        }
    }

    private void validateLoanPurpose(LoanAnalysisRequest request, LoanAnalysisResult result,
                                   List<ValidationIssue> issues) {
        // Additional validation based on loan purpose
        if (request.getLoanPurpose() == LoanPurpose.BUSINESS_LOAN && 
            request.getEmploymentType() != EmploymentType.SELF_EMPLOYED) {
            issues.add(new ValidationIssue(
                ValidationSeverity.MEDIUM,
                "Business loan for non-self-employed applicant may require additional scrutiny",
                "Verify business purpose and relationship to employment"
            ));
        }
    }

    private void validateAiConfidence(LoanAnalysisResult result, List<ValidationIssue> issues) {
        if (result.getConfidenceScore().compareTo(new BigDecimal("0.6")) < 0) {
            issues.add(new ValidationIssue(
                ValidationSeverity.MEDIUM,
                "AI confidence score below acceptable threshold: " + result.getConfidenceScore(),
                "Consider manual review due to low AI confidence"
            ));
        }
    }

    private void validateRegulatoryCompliance(LoanAnalysisRequest request, LoanAnalysisResult result,
                                            List<ValidationIssue> issues) {
        // Check for potential fair lending concerns
        if (result.getOverallRecommendation() == LoanRecommendation.DENY && 
            !hasObjectiveReason(result)) {
            issues.add(new ValidationIssue(
                ValidationSeverity.HIGH,
                "Denial recommendation requires documented objective reasons",
                "Ensure denial is based on quantifiable risk factors"
            ));
        }
    }

    private boolean hasObjectiveReason(LoanAnalysisResult result) {
        return result.getRiskFactors() != null && !result.getRiskFactors().isEmpty();
    }

    // Record types for validation results
    public record LoanAnalysisValidationResult(
        boolean isValid,
        List<ValidationIssue> issues,
        List<RiskFactor> additionalRiskFactors
    ) {}

    public record BusinessRuleValidationResult(
        boolean isValid,
        List<String> violations
    ) {}

    public record ValidationIssue(
        ValidationSeverity severity,
        String description,
        String recommendation
    ) {}

    public enum ValidationSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}