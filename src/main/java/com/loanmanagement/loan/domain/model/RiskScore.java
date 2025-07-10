package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Objects;

/**
 * Risk Score Value Object
 * Contains the calculated risk score and contributing factors for loan assessment
 */
@Value
@Builder(toBuilder = true)
public class RiskScore {
    
    int score; // 0-1000 scale
    String riskCategory;
    List<RiskFactor> factors;
    String assessment;
    String recommendations;
    
    public RiskScore(int score, String riskCategory, List<RiskFactor> factors,
                    String assessment, String recommendations) {
        
        // Validation
        if (score < 0 || score > 1000) {
            throw new IllegalArgumentException("Risk score must be between 0 and 1000");
        }
        
        Objects.requireNonNull(riskCategory, "Risk category cannot be null");
        Objects.requireNonNull(factors, "Risk factors list cannot be null");
        
        this.score = score;
        this.riskCategory = riskCategory;
        this.factors = List.copyOf(factors);
        this.assessment = assessment != null ? assessment : generateAssessment(score, riskCategory);
        this.recommendations = recommendations != null ? recommendations : generateRecommendations(score, riskCategory);
    }
    
    /**
     * Create a risk score with basic information
     */
    public static RiskScore of(int score, String riskCategory, List<RiskFactor> factors) {
        return RiskScore.builder()
                .score(score)
                .riskCategory(riskCategory)
                .factors(factors)
                .assessment(generateAssessment(score, riskCategory))
                .recommendations(generateRecommendations(score, riskCategory))
                .build();
    }
    
    /**
     * Create a low risk score
     */
    public static RiskScore lowRisk(int score, List<RiskFactor> factors) {
        return RiskScore.builder()
                .score(score)
                .riskCategory("LOW")
                .factors(factors)
                .assessment(generateAssessment(score, "LOW"))
                .recommendations(generateRecommendations(score, "LOW"))
                .build();
    }
    
    /**
     * Create a high risk score
     */
    public static RiskScore highRisk(int score, List<RiskFactor> factors) {
        return RiskScore.builder()
                .score(score)
                .riskCategory("HIGH")
                .factors(factors)
                .assessment(generateAssessment(score, "HIGH"))
                .recommendations(generateRecommendations(score, "HIGH"))
                .build();
    }
    
    /**
     * Check if this is a very low risk score (0-200)
     */
    public boolean isVeryLowRisk() {
        return score <= 200;
    }
    
    /**
     * Check if this is a low risk score (201-400)
     */
    public boolean isLowRisk() {
        return score > 200 && score <= 400;
    }
    
    /**
     * Check if this is a moderate risk score (401-600)
     */
    public boolean isModerateRisk() {
        return score > 400 && score <= 600;
    }
    
    /**
     * Check if this is a high risk score (601-800)
     */
    public boolean isHighRisk() {
        return score > 600 && score <= 800;
    }
    
    /**
     * Check if this is a very high risk score (801-1000)
     */
    public boolean isVeryHighRisk() {
        return score > 800;
    }
    
    /**
     * Check if risk is acceptable for loan approval
     */
    public boolean isAcceptableRisk() {
        return score <= 600; // Moderate risk or lower
    }
    
    /**
     * Check if risk requires additional review
     */
    public boolean requiresAdditionalReview() {
        return score > 500; // Above moderate risk
    }
    
    /**
     * Check if risk qualifies for premium rates
     */
    public boolean qualifiesForPremiumRates() {
        return score <= 300; // Low risk or very low risk
    }
    
    /**
     * Get risk score as percentage (0-100%)
     */
    public double getRiskPercentage() {
        return (score / 1000.0) * 100;
    }
    
    /**
     * Get risk factor by name
     */
    public RiskFactor getRiskFactor(String factorName) {
        return factors.stream()
                .filter(factor -> factor.getFactor().equals(factorName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get highest contributing risk factor
     */
    public RiskFactor getHighestRiskFactor() {
        return factors.stream()
                .max((f1, f2) -> Double.compare(f1.getContribution(), f2.getContribution()))
                .orElse(null);
    }
    
    /**
     * Get total contribution from all factors
     */
    public double getTotalContribution() {
        return factors.stream()
                .mapToDouble(RiskFactor::getContribution)
                .sum();
    }
    
    /**
     * Get factors with contribution above threshold
     */
    public List<RiskFactor> getSignificantFactors(double minContribution) {
        return factors.stream()
                .filter(factor -> factor.getContribution() >= minContribution)
                .toList();
    }
    
    /**
     * Get formatted risk display
     */
    public String getRiskDisplay() {
        return String.format("%d (%s)", score, riskCategory);
    }
    
    /**
     * Get risk category with score range
     */
    public String getRiskCategoryWithRange() {
        return switch (riskCategory.toUpperCase()) {
            case "VERY_LOW" -> "Very Low Risk (0-200)";
            case "LOW" -> "Low Risk (201-400)";
            case "MODERATE" -> "Moderate Risk (401-600)";
            case "HIGH" -> "High Risk (601-800)";
            case "VERY_HIGH" -> "Very High Risk (801-1000)";
            default -> riskCategory;
        };
    }
    
    /**
     * Get loan approval recommendation
     */
    public String getApprovalRecommendation() {
        return switch (riskCategory.toUpperCase()) {
            case "VERY_LOW" -> "Recommend approval with premium terms";
            case "LOW" -> "Recommend approval with standard terms";
            case "MODERATE" -> "Approve with enhanced monitoring";
            case "HIGH" -> "Requires additional review and risk mitigation";
            case "VERY_HIGH" -> "Recommend decline or significant risk mitigation";
            default -> "Manual review required";
        };
    }
    
    /**
     * Get suggested interest rate adjustment (basis points)
     */
    public int getSuggestedRateAdjustment() {
        return switch (riskCategory.toUpperCase()) {
            case "VERY_LOW" -> -50; // 0.5% discount
            case "LOW" -> -25; // 0.25% discount
            case "MODERATE" -> 0; // Base rate
            case "HIGH" -> 100; // 1% premium
            case "VERY_HIGH" -> 250; // 2.5% premium
            default -> 0;
        };
    }
    
    /**
     * Get required monitoring level
     */
    public String getMonitoringLevel() {
        return switch (riskCategory.toUpperCase()) {
            case "VERY_LOW", "LOW" -> "Standard";
            case "MODERATE" -> "Enhanced";
            case "HIGH" -> "Intensive";
            case "VERY_HIGH" -> "Continuous";
            default -> "Standard";
        };
    }
    
    /**
     * Get risk mitigation measures
     */
    public List<String> getRiskMitigationMeasures() {
        return switch (riskCategory.toUpperCase()) {
            case "VERY_LOW", "LOW" -> List.of("Standard loan terms");
            case "MODERATE" -> List.of(
                    "Enhanced credit monitoring",
                    "Quarterly review cycles",
                    "Payment performance tracking"
            );
            case "HIGH" -> List.of(
                    "Lower loan-to-value ratio",
                    "Enhanced income verification",
                    "Monthly monitoring",
                    "Early intervention protocols",
                    "Co-signer consideration"
            );
            case "VERY_HIGH" -> List.of(
                    "Secured loan structure",
                    "Significant down payment",
                    "Co-signer requirement",
                    "Weekly monitoring",
                    "Debt counseling requirement",
                    "Alternative loan products"
            );
            default -> List.of("Manual assessment required");
        };
    }
    
    /**
     * Get factors summary for reporting
     */
    public String getFactorsSummary() {
        return factors.stream()
                .map(factor -> String.format("%s: %.1f%%", 
                        factor.getFactor(), factor.getContribution() * 100))
                .reduce((f1, f2) -> f1 + ", " + f2)
                .orElse("No factors");
    }
    
    /**
     * Compare with another risk score
     */
    public int compareTo(RiskScore other) {
        return Integer.compare(this.score, other.score);
    }
    
    /**
     * Check if this risk score is better (lower) than another
     */
    public boolean isBetterThan(RiskScore other) {
        return this.score < other.score;
    }
    
    /**
     * Check if this risk score is worse (higher) than another
     */
    public boolean isWorseThan(RiskScore other) {
        return this.score > other.score;
    }
    
    private static String generateAssessment(int score, String riskCategory) {
        return switch (riskCategory.toUpperCase()) {
            case "VERY_LOW" -> String.format("Excellent risk profile (Score: %d). Minimal default risk expected.", score);
            case "LOW" -> String.format("Good risk profile (Score: %d). Low default risk expected.", score);
            case "MODERATE" -> String.format("Acceptable risk profile (Score: %d). Moderate monitoring recommended.", score);
            case "HIGH" -> String.format("Elevated risk profile (Score: %d). Enhanced risk management required.", score);
            case "VERY_HIGH" -> String.format("High risk profile (Score: %d). Significant risk mitigation needed.", score);
            default -> String.format("Risk score: %d, Category: %s", score, riskCategory);
        };
    }
    
    private static String generateRecommendations(int score, String riskCategory) {
        return switch (riskCategory.toUpperCase()) {
            case "VERY_LOW" -> "Consider premium rate offerings and cross-selling opportunities";
            case "LOW" -> "Standard approval process with competitive rates";
            case "MODERATE" -> "Approve with standard terms and enhanced monitoring";
            case "HIGH" -> "Consider risk mitigation measures or alternative loan products";
            case "VERY_HIGH" -> "Recommend decline or require significant risk mitigation";
            default -> "Manual review and assessment required";
        };
    }
}