package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * Risk Factor Value Object
 * Represents an individual risk factor contributing to the overall risk score
 */
@Value
@Builder(toBuilder = true)
public class RiskFactor {
    
    String factor;
    double value;
    double weight;
    double contribution;
    String description;
    String category;
    
    public RiskFactor(String factor, double value, double weight, double contribution,
                     String description, String category) {
        
        // Validation
        Objects.requireNonNull(factor, "Factor name cannot be null");
        
        if (factor.trim().isEmpty()) {
            throw new IllegalArgumentException("Factor name cannot be empty");
        }
        
        if (weight < 0 || weight > 1) {
            throw new IllegalArgumentException("Weight must be between 0 and 1");
        }
        
        if (contribution < 0) {
            throw new IllegalArgumentException("Contribution cannot be negative");
        }
        
        this.factor = factor.trim();
        this.value = value;
        this.weight = weight;
        this.contribution = contribution;
        this.description = description;
        this.category = category != null ? category : determineCategoryFromFactor(factor);
    }
    
    /**
     * Create a risk factor with basic information
     */
    public static RiskFactor of(String factor, double value, double weight, double contribution) {
        return RiskFactor.builder()
                .factor(factor)
                .value(value)
                .weight(weight)
                .contribution(contribution)
                .category(determineCategoryFromFactor(factor))
                .build();
    }
    
    /**
     * Create a risk factor with description
     */
    public static RiskFactor withDescription(String factor, double value, double weight, 
                                           double contribution, String description) {
        return RiskFactor.builder()
                .factor(factor)
                .value(value)
                .weight(weight)
                .contribution(contribution)
                .description(description)
                .category(determineCategoryFromFactor(factor))
                .build();
    }
    
    /**
     * Create a credit score risk factor
     */
    public static RiskFactor creditScore(int creditScore, double weight, double contribution) {
        return RiskFactor.builder()
                .factor("CREDIT_SCORE")
                .value(creditScore)
                .weight(weight)
                .contribution(contribution)
                .description(String.format("Credit score of %d", creditScore))
                .category("Credit Assessment")
                .build();
    }
    
    /**
     * Create a debt-to-income risk factor
     */
    public static RiskFactor debtToIncome(double dtiRatio, double weight, double contribution) {
        return RiskFactor.builder()
                .factor("DEBT_TO_INCOME")
                .value(dtiRatio)
                .weight(weight)
                .contribution(contribution)
                .description(String.format("DTI ratio of %.2f%%", dtiRatio * 100))
                .category("Financial Assessment")
                .build();
    }
    
    /**
     * Create an employment stability risk factor
     */
    public static RiskFactor employmentStability(int employmentMonths, double weight, double contribution) {
        return RiskFactor.builder()
                .factor("EMPLOYMENT_STABILITY")
                .value(employmentMonths)
                .weight(weight)
                .contribution(contribution)
                .description(String.format("Employment duration of %d months", employmentMonths))
                .category("Employment Assessment")
                .build();
    }
    
    /**
     * Create a banking history risk factor
     */
    public static RiskFactor bankingHistory(int bankingMonths, double weight, double contribution) {
        return RiskFactor.builder()
                .factor("BANKING_HISTORY")
                .value(bankingMonths)
                .weight(weight)
                .contribution(contribution)
                .description(String.format("Banking history of %d months", bankingMonths))
                .category("Banking Assessment")
                .build();
    }
    
    /**
     * Create a loan-to-value risk factor
     */
    public static RiskFactor loanToValue(double ltvRatio, double weight, double contribution) {
        return RiskFactor.builder()
                .factor("LOAN_TO_VALUE")
                .value(ltvRatio)
                .weight(weight)
                .contribution(contribution)
                .description(String.format("LTV ratio of %.2f%%", ltvRatio * 100))
                .category("Collateral Assessment")
                .build();
    }
    
    /**
     * Get contribution as percentage
     */
    public double getContributionPercentage() {
        return contribution * 100;
    }
    
    /**
     * Get weight as percentage
     */
    public double getWeightPercentage() {
        return weight * 100;
    }
    
    /**
     * Check if this is a significant risk contributor (above 10%)
     */
    public boolean isSignificantContributor() {
        return contribution >= 0.1; // 10% or more
    }
    
    /**
     * Check if this is a major risk contributor (above 20%)
     */
    public boolean isMajorContributor() {
        return contribution >= 0.2; // 20% or more
    }
    
    /**
     * Check if this is a critical risk contributor (above 30%)
     */
    public boolean isCriticalContributor() {
        return contribution >= 0.3; // 30% or more
    }
    
    /**
     * Get risk level based on contribution
     */
    public String getRiskLevel() {
        if (contribution >= 0.3) return "Critical";
        if (contribution >= 0.2) return "Major";
        if (contribution >= 0.1) return "Significant";
        if (contribution >= 0.05) return "Moderate";
        return "Minor";
    }
    
    /**
     * Get formatted factor display
     */
    public String getFactorDisplay() {
        return String.format("%s: %.1f%% (Weight: %.1f%%)", 
                formatFactorName(), getContributionPercentage(), getWeightPercentage());
    }
    
    /**
     * Get formatted factor name for display
     */
    public String formatFactorName() {
        String formatted = factor.replace("_", " ").toLowerCase();
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }
    
    /**
     * Get detailed factor information
     */
    public String getDetailedInfo() {
        String baseInfo = String.format("Factor: %s, Value: %.2f, Weight: %.1f%%, Contribution: %.1f%%",
                formatFactorName(), value, getWeightPercentage(), getContributionPercentage());
        
        if (description != null && !description.trim().isEmpty()) {
            baseInfo += ", Description: " + description;
        }
        
        return baseInfo;
    }
    
    /**
     * Get risk impact description
     */
    public String getRiskImpact() {
        return switch (getRiskLevel()) {
            case "Critical" -> "This factor has a critical impact on the overall risk assessment";
            case "Major" -> "This factor has a major impact on the overall risk assessment";
            case "Significant" -> "This factor has a significant impact on the overall risk assessment";
            case "Moderate" -> "This factor has a moderate impact on the overall risk assessment";
            case "Minor" -> "This factor has a minor impact on the overall risk assessment";
            default -> "Impact level unknown";
        };
    }
    
    /**
     * Get improvement recommendations for this factor
     */
    public String getImprovementRecommendations() {
        return switch (factor.toUpperCase()) {
            case "CREDIT_SCORE" -> "Improve credit score by paying down debts, making timely payments, and avoiding new credit inquiries";
            case "DEBT_TO_INCOME" -> "Reduce monthly debt obligations or increase income to improve debt-to-income ratio";
            case "EMPLOYMENT_STABILITY" -> "Maintain stable employment and avoid job changes before loan application";
            case "BANKING_HISTORY" -> "Build longer banking relationship and maintain good account standing";
            case "LOAN_TO_VALUE" -> "Increase down payment or reduce loan amount to improve loan-to-value ratio";
            default -> "Consult with loan officer for specific improvement strategies";
        };
    }
    
    /**
     * Check if this factor is related to credit assessment
     */
    public boolean isCreditFactor() {
        return "Credit Assessment".equals(category) || 
               factor.toUpperCase().contains("CREDIT");
    }
    
    /**
     * Check if this factor is related to financial assessment
     */
    public boolean isFinancialFactor() {
        return "Financial Assessment".equals(category) || 
               factor.toUpperCase().contains("INCOME") || 
               factor.toUpperCase().contains("DEBT");
    }
    
    /**
     * Check if this factor is related to employment assessment
     */
    public boolean isEmploymentFactor() {
        return "Employment Assessment".equals(category) || 
               factor.toUpperCase().contains("EMPLOYMENT");
    }
    
    /**
     * Compare with another risk factor by contribution
     */
    public int compareByContribution(RiskFactor other) {
        return Double.compare(this.contribution, other.contribution);
    }
    
    /**
     * Compare with another risk factor by weight
     */
    public int compareByWeight(RiskFactor other) {
        return Double.compare(this.weight, other.weight);
    }
    
    private static String determineCategoryFromFactor(String factor) {
        String upperFactor = factor.toUpperCase();
        
        if (upperFactor.contains("CREDIT")) {
            return "Credit Assessment";
        } else if (upperFactor.contains("INCOME") || upperFactor.contains("DEBT")) {
            return "Financial Assessment";
        } else if (upperFactor.contains("EMPLOYMENT")) {
            return "Employment Assessment";
        } else if (upperFactor.contains("BANKING")) {
            return "Banking Assessment";
        } else if (upperFactor.contains("LTV") || upperFactor.contains("COLLATERAL")) {
            return "Collateral Assessment";
        } else {
            return "General Assessment";
        }
    }
}