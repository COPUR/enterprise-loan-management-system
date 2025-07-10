package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * Credit Score Check Value Object
 * Contains the result of credit score eligibility assessment
 */
@Value
@Builder(toBuilder = true)
public class CreditScoreCheck {
    
    boolean passed;
    int actualScore;
    int requiredScore;
    String scoreCategory;
    String riskLevel;
    String recommendations;
    
    public CreditScoreCheck(boolean passed, int actualScore, int requiredScore, 
                           String scoreCategory, String riskLevel, String recommendations) {
        
        // Validation
        if (actualScore < 300 || actualScore > 850) {
            throw new IllegalArgumentException("Credit score must be between 300 and 850");
        }
        
        if (requiredScore < 300 || requiredScore > 850) {
            throw new IllegalArgumentException("Required credit score must be between 300 and 850");
        }
        
        this.passed = passed;
        this.actualScore = actualScore;
        this.requiredScore = requiredScore;
        this.scoreCategory = scoreCategory != null ? scoreCategory : determineCreditScoreCategory(actualScore);
        this.riskLevel = riskLevel != null ? riskLevel : determineRiskLevel(actualScore);
        this.recommendations = recommendations;
    }
    
    /**
     * Create a credit score check with basic information
     */
    public static CreditScoreCheck of(int actualScore, int requiredScore) {
        boolean passed = actualScore >= requiredScore;
        return CreditScoreCheck.builder()
                .passed(passed)
                .actualScore(actualScore)
                .requiredScore(requiredScore)
                .scoreCategory(determineCreditScoreCategory(actualScore))
                .riskLevel(determineRiskLevel(actualScore))
                .build();
    }
    
    /**
     * Create a passing credit score check
     */
    public static CreditScoreCheck passing(int actualScore, int requiredScore) {
        return CreditScoreCheck.builder()
                .passed(true)
                .actualScore(actualScore)
                .requiredScore(requiredScore)
                .scoreCategory(determineCreditScoreCategory(actualScore))
                .riskLevel(determineRiskLevel(actualScore))
                .build();
    }
    
    /**
     * Create a failing credit score check
     */
    public static CreditScoreCheck failing(int actualScore, int requiredScore) {
        return CreditScoreCheck.builder()
                .passed(false)
                .actualScore(actualScore)
                .requiredScore(requiredScore)
                .scoreCategory(determineCreditScoreCategory(actualScore))
                .riskLevel(determineRiskLevel(actualScore))
                .recommendations(generateRecommendations(actualScore, requiredScore))
                .build();
    }
    
    /**
     * Get the score gap (positive if above requirement, negative if below)
     */
    public int getScoreGap() {
        return actualScore - requiredScore;
    }
    
    /**
     * Check if the score is excellent (800+)
     */
    public boolean isExcellentScore() {
        return actualScore >= 800;
    }
    
    /**
     * Check if the score is very good (740-799)
     */
    public boolean isVeryGoodScore() {
        return actualScore >= 740 && actualScore < 800;
    }
    
    /**
     * Check if the score is good (670-739)
     */
    public boolean isGoodScore() {
        return actualScore >= 670 && actualScore < 740;
    }
    
    /**
     * Check if the score is fair (580-669)
     */
    public boolean isFairScore() {
        return actualScore >= 580 && actualScore < 670;
    }
    
    /**
     * Check if the score is poor (below 580)
     */
    public boolean isPoorScore() {
        return actualScore < 580;
    }
    
    /**
     * Get percentage above/below requirement
     */
    public double getPercentageFromRequirement() {
        return ((double) getScoreGap() / requiredScore) * 100;
    }
    
    /**
     * Get formatted score display
     */
    public String getScoreDisplay() {
        return String.format("%d (%s)", actualScore, scoreCategory);
    }
    
    /**
     * Get detailed assessment message
     */
    public String getAssessmentMessage() {
        if (passed) {
            return String.format("Credit score of %d meets the minimum requirement of %d. Score category: %s",
                    actualScore, requiredScore, scoreCategory);
        } else {
            return String.format("Credit score of %d is below the minimum requirement of %d. Gap: %d points. Score category: %s",
                    actualScore, requiredScore, Math.abs(getScoreGap()), scoreCategory);
        }
    }
    
    /**
     * Check if score qualifies for premium rates
     */
    public boolean qualifiesForPremiumRates() {
        return actualScore >= 750;
    }
    
    /**
     * Check if score requires additional scrutiny
     */
    public boolean requiresAdditionalScrutiny() {
        return actualScore < 650;
    }
    
    /**
     * Get estimated time to improve score to requirement
     */
    public String getEstimatedImprovementTime() {
        if (passed) {
            return "N/A - Score already meets requirement";
        }
        
        int gap = Math.abs(getScoreGap());
        if (gap <= 20) {
            return "3-6 months with good payment history";
        } else if (gap <= 50) {
            return "6-12 months with debt reduction and good payment history";
        } else {
            return "12+ months with comprehensive credit repair";
        }
    }
    
    private static String determineCreditScoreCategory(int score) {
        if (score >= 800) return "Excellent";
        if (score >= 740) return "Very Good";
        if (score >= 670) return "Good";
        if (score >= 580) return "Fair";
        return "Poor";
    }
    
    private static String determineRiskLevel(int score) {
        if (score >= 740) return "Low";
        if (score >= 670) return "Moderate";
        if (score >= 580) return "High";
        return "Very High";
    }
    
    private static String generateRecommendations(int actualScore, int requiredScore) {
        int gap = requiredScore - actualScore;
        
        if (gap <= 20) {
            return "Pay down existing debt, make all payments on time, and avoid new credit inquiries";
        } else if (gap <= 50) {
            return "Focus on paying down high-balance credit cards, ensure all payments are made on time, and consider debt consolidation";
        } else {
            return "Consider comprehensive credit repair, work with credit counseling services, and focus on debt reduction before reapplying";
        }
    }
}