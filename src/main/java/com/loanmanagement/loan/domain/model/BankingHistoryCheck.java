package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * Banking History Check Value Object
 * Contains the result of banking history eligibility assessment
 */
@Value
@Builder(toBuilder = true)
public class BankingHistoryCheck {
    
    boolean passed;
    int bankingHistoryMonths;
    int minimumRequired;
    String experienceLevel;
    String riskLevel;
    String recommendations;
    
    public BankingHistoryCheck(boolean passed, int bankingHistoryMonths, int minimumRequired,
                              String experienceLevel, String riskLevel, String recommendations) {
        
        // Validation
        if (bankingHistoryMonths < 0) {
            throw new IllegalArgumentException("Banking history months cannot be negative");
        }
        
        if (minimumRequired < 0) {
            throw new IllegalArgumentException("Minimum required months cannot be negative");
        }
        
        this.passed = passed;
        this.bankingHistoryMonths = bankingHistoryMonths;
        this.minimumRequired = minimumRequired;
        this.experienceLevel = experienceLevel != null ? experienceLevel : determineExperienceLevel(bankingHistoryMonths);
        this.riskLevel = riskLevel != null ? riskLevel : determineRiskLevel(bankingHistoryMonths);
        this.recommendations = recommendations;
    }
    
    /**
     * Create a banking history check with basic information
     */
    public static BankingHistoryCheck of(int bankingHistoryMonths, int minimumRequired) {
        boolean passed = bankingHistoryMonths >= minimumRequired;
        return BankingHistoryCheck.builder()
                .passed(passed)
                .bankingHistoryMonths(bankingHistoryMonths)
                .minimumRequired(minimumRequired)
                .experienceLevel(determineExperienceLevel(bankingHistoryMonths))
                .riskLevel(determineRiskLevel(bankingHistoryMonths))
                .build();
    }
    
    /**
     * Create a passing banking history check
     */
    public static BankingHistoryCheck passing(int bankingHistoryMonths, int minimumRequired) {
        return BankingHistoryCheck.builder()
                .passed(true)
                .bankingHistoryMonths(bankingHistoryMonths)
                .minimumRequired(minimumRequired)
                .experienceLevel(determineExperienceLevel(bankingHistoryMonths))
                .riskLevel(determineRiskLevel(bankingHistoryMonths))
                .build();
    }
    
    /**
     * Create a failing banking history check
     */
    public static BankingHistoryCheck failing(int bankingHistoryMonths, int minimumRequired) {
        return BankingHistoryCheck.builder()
                .passed(false)
                .bankingHistoryMonths(bankingHistoryMonths)
                .minimumRequired(minimumRequired)
                .experienceLevel(determineExperienceLevel(bankingHistoryMonths))
                .riskLevel(determineRiskLevel(bankingHistoryMonths))
                .recommendations(generateRecommendations(bankingHistoryMonths, minimumRequired))
                .build();
    }
    
    /**
     * Get banking history in years
     */
    public double getBankingHistoryYears() {
        return bankingHistoryMonths / 12.0;
    }
    
    /**
     * Get minimum required in years
     */
    public double getMinimumRequiredYears() {
        return minimumRequired / 12.0;
    }
    
    /**
     * Get history gap (positive if above requirement, negative if below)
     */
    public int getHistoryGap() {
        return bankingHistoryMonths - minimumRequired;
    }
    
    /**
     * Check if customer is a new banking customer (less than 12 months)
     */
    public boolean isNewCustomer() {
        return bankingHistoryMonths < 12;
    }
    
    /**
     * Check if customer has limited banking history (12-36 months)
     */
    public boolean hasLimitedHistory() {
        return bankingHistoryMonths >= 12 && bankingHistoryMonths < 36;
    }
    
    /**
     * Check if customer has established banking history (36-60 months)
     */
    public boolean hasEstablishedHistory() {
        return bankingHistoryMonths >= 36 && bankingHistoryMonths < 60;
    }
    
    /**
     * Check if customer has extensive banking history (60+ months)
     */
    public boolean hasExtensiveHistory() {
        return bankingHistoryMonths >= 60;
    }
    
    /**
     * Check if banking history qualifies for premium rates
     */
    public boolean qualifiesForPremiumRates() {
        return bankingHistoryMonths >= 36; // 3+ years
    }
    
    /**
     * Check if additional verification is required
     */
    public boolean requiresAdditionalVerification() {
        return bankingHistoryMonths < 12;
    }
    
    /**
     * Get banking history score (0-100)
     */
    public int getBankingHistoryScore() {
        if (bankingHistoryMonths >= 60) return 100; // 5+ years
        if (bankingHistoryMonths >= 36) return 80;  // 3+ years
        if (bankingHistoryMonths >= 24) return 60;  // 2+ years
        if (bankingHistoryMonths >= 12) return 40;  // 1+ year
        if (bankingHistoryMonths >= 6) return 20;   // 6+ months
        return 10; // Less than 6 months
    }
    
    /**
     * Get formatted banking history display
     */
    public String getBankingHistoryDisplay() {
        return String.format("%.1f years (%s)", getBankingHistoryYears(), experienceLevel);
    }
    
    /**
     * Get detailed assessment message
     */
    public String getAssessmentMessage() {
        if (passed) {
            return String.format("Banking history of %.1f years meets the minimum requirement of %.1f years. Experience level: %s",
                    getBankingHistoryYears(), getMinimumRequiredYears(), experienceLevel);
        } else {
            return String.format("Banking history of %.1f years is below the minimum requirement of %.1f years. Shortfall: %.1f years",
                    getBankingHistoryYears(), getMinimumRequiredYears(), 
                    Math.abs(getHistoryGap()) / 12.0);
        }
    }
    
    /**
     * Get required additional documentation for limited banking history
     */
    public java.util.List<String> getAdditionalDocumentationRequired() {
        if (bankingHistoryMonths >= 36) {
            return java.util.List.of(); // No additional documentation needed
        }
        
        if (bankingHistoryMonths >= 12) {
            return java.util.List.of(
                    "Bank statements for full history period",
                    "Previous banking references",
                    "Credit references from other financial institutions"
            );
        }
        
        return java.util.List.of(
                "Complete bank statements since account opening",
                "Previous banking relationships documentation",
                "Alternative credit references (utility bills, rent payments)",
                "Financial references from employers or other sources",
                "Savings account history if available"
        );
    }
    
    /**
     * Get risk mitigation measures
     */
    public java.util.List<String> getRiskMitigationMeasures() {
        if (bankingHistoryMonths >= 36) {
            return java.util.List.of(); // No additional measures needed
        }
        
        if (bankingHistoryMonths >= 12) {
            return java.util.List.of(
                    "Enhanced income verification",
                    "Lower initial credit limits",
                    "More frequent account monitoring"
            );
        }
        
        return java.util.List.of(
                "Comprehensive financial documentation",
                "Co-signer consideration",
                "Secured loan options",
                "Lower loan-to-value ratios",
                "Enhanced monitoring and reporting",
                "Shorter initial loan terms"
        );
    }
    
    /**
     * Get time until meeting minimum requirements
     */
    public String getTimeToMeetRequirements() {
        if (passed) {
            return "Already meets requirements";
        }
        
        int shortfall = minimumRequired - bankingHistoryMonths;
        if (shortfall <= 0) {
            return "Already meets requirements";
        }
        
        return String.format("%d months until minimum banking history requirement is met", shortfall);
    }
    
    /**
     * Get banking relationship value score
     */
    public String getRelationshipValue() {
        return switch (experienceLevel) {
            case "Extensive" -> "High Value - Long-term customer";
            case "Established" -> "Good Value - Stable customer";
            case "Limited" -> "Moderate Value - Developing relationship";
            case "New" -> "Limited Value - New relationship";
            default -> "Unknown";
        };
    }
    
    /**
     * Check if customer qualifies for relationship banking benefits
     */
    public boolean qualifiesForRelationshipBenefits() {
        return bankingHistoryMonths >= 24; // 2+ years
    }
    
    /**
     * Get recommended account monitoring frequency
     */
    public String getRecommendedMonitoringFrequency() {
        return switch (experienceLevel) {
            case "Extensive", "Established" -> "Quarterly";
            case "Limited" -> "Monthly";
            case "New" -> "Weekly";
            default -> "Monthly";
        };
    }
    
    private static String determineExperienceLevel(int bankingHistoryMonths) {
        if (bankingHistoryMonths >= 60) return "Extensive";  // 5+ years
        if (bankingHistoryMonths >= 36) return "Established"; // 3+ years
        if (bankingHistoryMonths >= 12) return "Limited";     // 1+ year
        return "New"; // Less than 1 year
    }
    
    private static String determineRiskLevel(int bankingHistoryMonths) {
        if (bankingHistoryMonths >= 60) return "Very Low";  // 5+ years
        if (bankingHistoryMonths >= 36) return "Low";       // 3+ years
        if (bankingHistoryMonths >= 24) return "Moderate";  // 2+ years
        if (bankingHistoryMonths >= 12) return "High";      // 1+ year
        return "Very High"; // Less than 1 year
    }
    
    private static String generateRecommendations(int bankingHistoryMonths, int minimumRequired) {
        int shortfall = minimumRequired - bankingHistoryMonths;
        
        if (shortfall <= 6) {
            return "Continue building banking relationship and consider reapplying in " + shortfall + " months";
        } else if (shortfall <= 12) {
            return "Establish longer banking relationship, consider alternative loan products, or provide additional financial references";
        } else {
            return "Build comprehensive banking relationship, consider secured loan products, or explore co-signer options";
        }
    }
}