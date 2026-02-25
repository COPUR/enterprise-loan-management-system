package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Debt-to-Income Check Value Object
 * Contains the result of debt-to-income ratio eligibility assessment
 */
@Value
@Builder(toBuilder = true)
public class DebtToIncomeCheck {
    
    boolean passed;
    BigDecimal currentDTI;
    BigDecimal projectedDTI;
    BigDecimal maxAllowedDTI;
    String riskLevel;
    String recommendations;
    
    public DebtToIncomeCheck(boolean passed, BigDecimal currentDTI, BigDecimal projectedDTI,
                            BigDecimal maxAllowedDTI, String riskLevel, String recommendations) {
        
        // Validation
        Objects.requireNonNull(currentDTI, "Current DTI cannot be null");
        Objects.requireNonNull(projectedDTI, "Projected DTI cannot be null");
        Objects.requireNonNull(maxAllowedDTI, "Max allowed DTI cannot be null");
        
        if (currentDTI.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Current DTI cannot be negative");
        }
        
        if (projectedDTI.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Projected DTI cannot be negative");
        }
        
        if (maxAllowedDTI.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max allowed DTI must be positive");
        }
        
        this.passed = passed;
        this.currentDTI = currentDTI;
        this.projectedDTI = projectedDTI;
        this.maxAllowedDTI = maxAllowedDTI;
        this.riskLevel = riskLevel != null ? riskLevel : determineRiskLevel(projectedDTI);
        this.recommendations = recommendations;
    }
    
    /**
     * Create a DTI check with basic information
     */
    public static DebtToIncomeCheck of(BigDecimal currentDTI, BigDecimal projectedDTI, BigDecimal maxAllowedDTI) {
        boolean passed = projectedDTI.compareTo(maxAllowedDTI) <= 0;
        return DebtToIncomeCheck.builder()
                .passed(passed)
                .currentDTI(currentDTI)
                .projectedDTI(projectedDTI)
                .maxAllowedDTI(maxAllowedDTI)
                .riskLevel(determineRiskLevel(projectedDTI))
                .build();
    }
    
    /**
     * Create a passing DTI check
     */
    public static DebtToIncomeCheck passing(BigDecimal currentDTI, BigDecimal projectedDTI, BigDecimal maxAllowedDTI) {
        return DebtToIncomeCheck.builder()
                .passed(true)
                .currentDTI(currentDTI)
                .projectedDTI(projectedDTI)
                .maxAllowedDTI(maxAllowedDTI)
                .riskLevel(determineRiskLevel(projectedDTI))
                .build();
    }
    
    /**
     * Create a failing DTI check
     */
    public static DebtToIncomeCheck failing(BigDecimal currentDTI, BigDecimal projectedDTI, BigDecimal maxAllowedDTI) {
        return DebtToIncomeCheck.builder()
                .passed(false)
                .currentDTI(currentDTI)
                .projectedDTI(projectedDTI)
                .maxAllowedDTI(maxAllowedDTI)
                .riskLevel(determineRiskLevel(projectedDTI))
                .recommendations(generateRecommendations(projectedDTI, maxAllowedDTI))
                .build();
    }
    
    /**
     * Get the DTI gap (positive if above limit, negative if below)
     */
    public BigDecimal getDTIGap() {
        return projectedDTI.subtract(maxAllowedDTI);
    }
    
    /**
     * Get the DTI increase from current to projected
     */
    public BigDecimal getDTIIncrease() {
        return projectedDTI.subtract(currentDTI);
    }
    
    /**
     * Get current DTI as percentage
     */
    public BigDecimal getCurrentDTIPercentage() {
        return currentDTI.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get projected DTI as percentage
     */
    public BigDecimal getProjectedDTIPercentage() {
        return projectedDTI.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get max allowed DTI as percentage
     */
    public BigDecimal getMaxAllowedDTIPercentage() {
        return maxAllowedDTI.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Check if DTI is excellent (below 20%)
     */
    public boolean isExcellentDTI() {
        return projectedDTI.compareTo(new BigDecimal("0.20")) < 0;
    }
    
    /**
     * Check if DTI is good (20-30%)
     */
    public boolean isGoodDTI() {
        return projectedDTI.compareTo(new BigDecimal("0.20")) >= 0 && 
               projectedDTI.compareTo(new BigDecimal("0.30")) < 0;
    }
    
    /**
     * Check if DTI is acceptable (30-40%)
     */
    public boolean isAcceptableDTI() {
        return projectedDTI.compareTo(new BigDecimal("0.30")) >= 0 && 
               projectedDTI.compareTo(new BigDecimal("0.40")) < 0;
    }
    
    /**
     * Check if DTI is concerning (40%+)
     */
    public boolean isConcerningDTI() {
        return projectedDTI.compareTo(new BigDecimal("0.40")) >= 0;
    }
    
    /**
     * Get percentage over/under the limit
     */
    public BigDecimal getPercentageFromLimit() {
        return getDTIGap().divide(maxAllowedDTI, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
    
    /**
     * Get formatted DTI display
     */
    public String getDTIDisplay() {
        return String.format("Current: %.2f%%, Projected: %.2f%%, Limit: %.2f%%",
                getCurrentDTIPercentage(), getProjectedDTIPercentage(), getMaxAllowedDTIPercentage());
    }
    
    /**
     * Get detailed assessment message
     */
    public String getAssessmentMessage() {
        if (passed) {
            return String.format("Projected DTI of %.2f%% is within the acceptable limit of %.2f%%",
                    getProjectedDTIPercentage(), getMaxAllowedDTIPercentage());
        } else {
            return String.format("Projected DTI of %.2f%% exceeds the maximum allowed %.2f%% by %.2f%%",
                    getProjectedDTIPercentage(), getMaxAllowedDTIPercentage(), 
                    Math.abs(getPercentageFromLimit().doubleValue()));
        }
    }
    
    /**
     * Check if DTI qualifies for premium rates
     */
    public boolean qualifiesForPremiumRates() {
        return projectedDTI.compareTo(new BigDecimal("0.25")) < 0; // Below 25%
    }
    
    /**
     * Check if DTI requires additional income verification
     */
    public boolean requiresAdditionalIncomeVerification() {
        return projectedDTI.compareTo(new BigDecimal("0.35")) >= 0; // 35% or higher
    }
    
    /**
     * Get monthly debt reduction needed to meet requirement
     */
    public BigDecimal getMonthlyDebtReductionNeeded(BigDecimal monthlyIncome) {
        if (passed) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal excessDTI = getDTIGap();
        return excessDTI.multiply(monthlyIncome).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Get additional monthly income needed to meet requirement
     */
    public BigDecimal getAdditionalIncomeNeeded(BigDecimal monthlyDebtPayments) {
        if (passed) {
            return BigDecimal.ZERO;
        }
        
        // Calculate required income: debt payments / max DTI
        BigDecimal requiredIncome = monthlyDebtPayments.divide(maxAllowedDTI, 2, RoundingMode.HALF_UP);
        BigDecimal currentIncome = monthlyDebtPayments.divide(projectedDTI, 2, RoundingMode.HALF_UP);
        
        return requiredIncome.subtract(currentIncome).max(BigDecimal.ZERO);
    }
    
    private static String determineRiskLevel(BigDecimal projectedDTI) {
        if (projectedDTI.compareTo(new BigDecimal("0.20")) < 0) return "Low";
        if (projectedDTI.compareTo(new BigDecimal("0.30")) < 0) return "Moderate";
        if (projectedDTI.compareTo(new BigDecimal("0.40")) < 0) return "High";
        return "Very High";
    }
    
    private static String generateRecommendations(BigDecimal projectedDTI, BigDecimal maxAllowedDTI) {
        BigDecimal gap = projectedDTI.subtract(maxAllowedDTI);
        BigDecimal gapPercentage = gap.multiply(new BigDecimal("100"));
        
        if (gapPercentage.compareTo(new BigDecimal("5")) <= 0) {
            return "Consider reducing monthly debt payments or increasing income slightly";
        } else if (gapPercentage.compareTo(new BigDecimal("10")) <= 0) {
            return "Reduce monthly debt obligations through debt consolidation or payoff, or increase income";
        } else {
            return "Significantly reduce debt obligations or increase income before reapplying";
        }
    }
}