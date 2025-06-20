package com.bank.loanmanagement.domain.loan;

import java.util.List;
import java.util.Objects;

/**
 * Domain entity for risk assessment results
 */
public class RiskAssessment {

    private final RiskLevel overallRiskLevel;
    private final int riskScore; // 0-100
    private final double defaultProbability; // 0.0-1.0
    private final List<String> riskFactors;
    private final List<String> mitigatingFactors;
    private final double confidenceLevel;

    public RiskAssessment(
            RiskLevel overallRiskLevel,
            int riskScore,
            double defaultProbability,
            List<String> riskFactors,
            List<String> mitigatingFactors,
            double confidenceLevel) {
        
        this.overallRiskLevel = Objects.requireNonNull(overallRiskLevel, "Risk level cannot be null");
        
        if (riskScore < 0 || riskScore > 100) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100");
        }
        this.riskScore = riskScore;
        
        if (defaultProbability < 0.0 || defaultProbability > 1.0) {
            throw new IllegalArgumentException("Default probability must be between 0.0 and 1.0");
        }
        this.defaultProbability = defaultProbability;
        
        if (confidenceLevel < 0.0 || confidenceLevel > 1.0) {
            throw new IllegalArgumentException("Confidence level must be between 0.0 and 1.0");
        }
        this.confidenceLevel = confidenceLevel;
        
        this.riskFactors = riskFactors != null ? List.copyOf(riskFactors) : List.of();
        this.mitigatingFactors = mitigatingFactors != null ? List.copyOf(mitigatingFactors) : List.of();
    }

    /**
     * Factory method for low risk assessment
     */
    public static RiskAssessment lowRisk(List<String> mitigatingFactors) {
        return new RiskAssessment(
                RiskLevel.LOW,
                25,
                0.05,
                List.of(),
                mitigatingFactors,
                0.9
        );
    }

    /**
     * Factory method for high risk assessment
     */
    public static RiskAssessment highRisk(List<String> riskFactors) {
        return new RiskAssessment(
                RiskLevel.HIGH,
                80,
                0.25,
                riskFactors,
                List.of(),
                0.85
        );
    }

    /**
     * Check if the risk is acceptable for lending
     */
    public boolean isAcceptableForLending() {
        return overallRiskLevel != RiskLevel.HIGH && confidenceLevel >= 0.7;
    }

    /**
     * Get recommended loan approval action
     */
    public LoanApprovalRecommendation getApprovalRecommendation() {
        if (overallRiskLevel == RiskLevel.LOW && confidenceLevel >= 0.9) {
            return LoanApprovalRecommendation.AUTO_APPROVE;
        }
        if (overallRiskLevel == RiskLevel.HIGH || confidenceLevel < 0.6) {
            return LoanApprovalRecommendation.DECLINE;
        }
        return LoanApprovalRecommendation.MANUAL_REVIEW;
    }

    // Getters
    public RiskLevel getOverallRiskLevel() { return overallRiskLevel; }
    public int getRiskScore() { return riskScore; }
    public double getDefaultProbability() { return defaultProbability; }
    public List<String> getRiskFactors() { return List.copyOf(riskFactors); }
    public List<String> getMitigatingFactors() { return List.copyOf(mitigatingFactors); }
    public double getConfidenceLevel() { return confidenceLevel; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RiskAssessment that)) return false;
        return riskScore == that.riskScore &&
               Double.compare(that.defaultProbability, defaultProbability) == 0 &&
               Double.compare(that.confidenceLevel, confidenceLevel) == 0 &&
               overallRiskLevel == that.overallRiskLevel &&
               Objects.equals(riskFactors, that.riskFactors) &&
               Objects.equals(mitigatingFactors, that.mitigatingFactors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(overallRiskLevel, riskScore, defaultProbability, 
                          riskFactors, mitigatingFactors, confidenceLevel);
    }

    public enum LoanApprovalRecommendation {
        AUTO_APPROVE,
        MANUAL_REVIEW,
        DECLINE
    }
}