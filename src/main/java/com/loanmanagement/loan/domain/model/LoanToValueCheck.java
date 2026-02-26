package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Loan-to-Value Check Value Object
 * Contains the result of loan-to-value ratio eligibility assessment
 */
@Value
@Builder(toBuilder = true)
public class LoanToValueCheck {
    
    boolean passed;
    BigDecimal ltvRatio;
    BigDecimal maxAllowedLTV;
    boolean unsecuredLoan;
    String riskLevel;
    String recommendations;
    
    public LoanToValueCheck(boolean passed, BigDecimal ltvRatio, BigDecimal maxAllowedLTV,
                           boolean unsecuredLoan, String riskLevel, String recommendations) {
        
        // Validation
        Objects.requireNonNull(ltvRatio, "LTV ratio cannot be null");
        Objects.requireNonNull(maxAllowedLTV, "Max allowed LTV cannot be null");
        
        if (ltvRatio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("LTV ratio cannot be negative");
        }
        
        if (maxAllowedLTV.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max allowed LTV must be positive");
        }
        
        this.passed = passed;
        this.ltvRatio = ltvRatio;
        this.maxAllowedLTV = maxAllowedLTV;
        this.unsecuredLoan = unsecuredLoan;
        this.riskLevel = riskLevel != null ? riskLevel : determineRiskLevel(ltvRatio, unsecuredLoan);
        this.recommendations = recommendations;
    }
    
    /**
     * Create an LTV check with basic information
     */
    public static LoanToValueCheck of(BigDecimal ltvRatio, BigDecimal maxAllowedLTV, boolean unsecuredLoan) {
        boolean passed = unsecuredLoan || ltvRatio.compareTo(maxAllowedLTV) <= 0;
        return LoanToValueCheck.builder()
                .passed(passed)
                .ltvRatio(ltvRatio)
                .maxAllowedLTV(maxAllowedLTV)
                .unsecuredLoan(unsecuredLoan)
                .riskLevel(determineRiskLevel(ltvRatio, unsecuredLoan))
                .build();
    }
    
    /**
     * Create an unsecured loan LTV check
     */
    public static LoanToValueCheck unsecured() {
        return LoanToValueCheck.builder()
                .passed(true)
                .ltvRatio(BigDecimal.ZERO)
                .maxAllowedLTV(new BigDecimal("1.00"))
                .unsecuredLoan(true)
                .riskLevel("Variable")
                .build();
    }
    
    /**
     * Create a passing secured loan LTV check
     */
    public static LoanToValueCheck passingSecured(BigDecimal ltvRatio, BigDecimal maxAllowedLTV) {
        return LoanToValueCheck.builder()
                .passed(true)
                .ltvRatio(ltvRatio)
                .maxAllowedLTV(maxAllowedLTV)
                .unsecuredLoan(false)
                .riskLevel(determineRiskLevel(ltvRatio, false))
                .build();
    }
    
    /**
     * Create a failing secured loan LTV check
     */
    public static LoanToValueCheck failingSecured(BigDecimal ltvRatio, BigDecimal maxAllowedLTV) {
        return LoanToValueCheck.builder()
                .passed(false)
                .ltvRatio(ltvRatio)
                .maxAllowedLTV(maxAllowedLTV)
                .unsecuredLoan(false)
                .riskLevel(determineRiskLevel(ltvRatio, false))
                .recommendations(generateRecommendations(ltvRatio, maxAllowedLTV))
                .build();
    }
    
    /**
     * Get the LTV gap (positive if above limit, negative if below)
     */
    public BigDecimal getLTVGap() {
        return ltvRatio.subtract(maxAllowedLTV);
    }
    
    /**
     * Get LTV ratio as percentage
     */
    public BigDecimal getLTVPercentage() {
        return ltvRatio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get max allowed LTV as percentage
     */
    public BigDecimal getMaxAllowedLTVPercentage() {
        return maxAllowedLTV.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Check if LTV is conservative (below 60%)
     */
    public boolean isConservativeLTV() {
        return !unsecuredLoan && ltvRatio.compareTo(new BigDecimal("0.60")) < 0;
    }
    
    /**
     * Check if LTV is moderate (60-80%)
     */
    public boolean isModerateLTV() {
        return !unsecuredLoan && 
               ltvRatio.compareTo(new BigDecimal("0.60")) >= 0 && 
               ltvRatio.compareTo(new BigDecimal("0.80")) < 0;
    }
    
    /**
     * Check if LTV is high (80-90%)
     */
    public boolean isHighLTV() {
        return !unsecuredLoan && 
               ltvRatio.compareTo(new BigDecimal("0.80")) >= 0 && 
               ltvRatio.compareTo(new BigDecimal("0.90")) < 0;
    }
    
    /**
     * Check if LTV is very high (90%+)
     */
    public boolean isVeryHighLTV() {
        return !unsecuredLoan && ltvRatio.compareTo(new BigDecimal("0.90")) >= 0;
    }
    
    /**
     * Get percentage over/under the limit
     */
    public BigDecimal getPercentageFromLimit() {
        if (unsecuredLoan) {
            return BigDecimal.ZERO;
        }
        return getLTVGap().divide(maxAllowedLTV, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
    
    /**
     * Get formatted LTV display
     */
    public String getLTVDisplay() {
        if (unsecuredLoan) {
            return "Unsecured loan - No LTV applicable";
        }
        return String.format("LTV: %.2f%%, Limit: %.2f%%",
                getLTVPercentage(), getMaxAllowedLTVPercentage());
    }
    
    /**
     * Get detailed assessment message
     */
    public String getAssessmentMessage() {
        if (unsecuredLoan) {
            return "Unsecured loan - No collateral evaluation required";
        }
        
        if (passed) {
            return String.format("LTV ratio of %.2f%% is within the acceptable limit of %.2f%%",
                    getLTVPercentage(), getMaxAllowedLTVPercentage());
        } else {
            return String.format("LTV ratio of %.2f%% exceeds the maximum allowed %.2f%% by %.2f%%",
                    getLTVPercentage(), getMaxAllowedLTVPercentage(), 
                    Math.abs(getPercentageFromLimit().doubleValue()));
        }
    }
    
    /**
     * Check if LTV qualifies for premium rates
     */
    public boolean qualifiesForPremiumRates() {
        return !unsecuredLoan && ltvRatio.compareTo(new BigDecimal("0.70")) < 0; // Below 70%
    }
    
    /**
     * Check if mortgage insurance is required
     */
    public boolean requiresMortgageInsurance() {
        return !unsecuredLoan && ltvRatio.compareTo(new BigDecimal("0.80")) > 0; // Above 80%
    }
    
    /**
     * Check if additional collateral evaluation is needed
     */
    public boolean requiresAdditionalCollateralEvaluation() {
        return !unsecuredLoan && ltvRatio.compareTo(new BigDecimal("0.85")) >= 0; // 85% or higher
    }
    
    /**
     * Get additional down payment needed to meet requirement
     */
    public BigDecimal getAdditionalDownPaymentNeeded(BigDecimal loanAmount) {
        if (passed || unsecuredLoan) {
            return BigDecimal.ZERO;
        }
        
        // Calculate required collateral value: loan amount / max LTV
        BigDecimal requiredCollateralValue = loanAmount.divide(maxAllowedLTV, 2, RoundingMode.HALF_UP);
        BigDecimal currentCollateralValue = loanAmount.divide(ltvRatio, 2, RoundingMode.HALF_UP);
        
        return requiredCollateralValue.subtract(currentCollateralValue).max(BigDecimal.ZERO);
    }
    
    /**
     * Get loan amount reduction needed to meet requirement
     */
    public BigDecimal getLoanReductionNeeded(BigDecimal collateralValue) {
        if (passed || unsecuredLoan) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal maxLoanAmount = collateralValue.multiply(maxAllowedLTV);
        BigDecimal currentLoanAmount = collateralValue.multiply(ltvRatio);
        
        return currentLoanAmount.subtract(maxLoanAmount).max(BigDecimal.ZERO);
    }
    
    /**
     * Get equity percentage (inverse of LTV)
     */
    public BigDecimal getEquityPercentage() {
        if (unsecuredLoan) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ONE.subtract(ltvRatio)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private static String determineRiskLevel(BigDecimal ltvRatio, boolean unsecuredLoan) {
        if (unsecuredLoan) {
            return "Variable"; // Risk depends on other factors for unsecured loans
        }
        
        if (ltvRatio.compareTo(new BigDecimal("0.60")) < 0) return "Low";
        if (ltvRatio.compareTo(new BigDecimal("0.80")) < 0) return "Moderate";
        if (ltvRatio.compareTo(new BigDecimal("0.90")) < 0) return "High";
        return "Very High";
    }
    
    private static String generateRecommendations(BigDecimal ltvRatio, BigDecimal maxAllowedLTV) {
        BigDecimal gap = ltvRatio.subtract(maxAllowedLTV);
        BigDecimal gapPercentage = gap.multiply(new BigDecimal("100"));
        
        if (gapPercentage.compareTo(new BigDecimal("5")) <= 0) {
            return "Consider increasing down payment or reducing loan amount slightly";
        } else if (gapPercentage.compareTo(new BigDecimal("10")) <= 0) {
            return "Increase down payment significantly or reduce loan amount to meet LTV requirements";
        } else {
            return "Substantial down payment increase or loan amount reduction required to meet LTV limits";
        }
    }
}