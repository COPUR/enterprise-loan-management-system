package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Objects;

/**
 * Employment Check Value Object
 * Contains the result of employment eligibility assessment
 */
@Value
@Builder(toBuilder = true)
public class EmploymentCheck {
    
    boolean passed;
    EmploymentType employmentType;
    int employmentDuration; // in months
    List<String> issues;
    String stabilityRating;
    String recommendations;
    
    public EmploymentCheck(boolean passed, EmploymentType employmentType, int employmentDuration,
                          List<String> issues, String stabilityRating, String recommendations) {
        
        // Validation
        Objects.requireNonNull(employmentType, "Employment type cannot be null");
        Objects.requireNonNull(issues, "Issues list cannot be null");
        
        if (employmentDuration < 0) {
            throw new IllegalArgumentException("Employment duration cannot be negative");
        }
        
        this.passed = passed;
        this.employmentType = employmentType;
        this.employmentDuration = employmentDuration;
        this.issues = List.copyOf(issues);
        this.stabilityRating = stabilityRating != null ? stabilityRating : determineStabilityRating(employmentType, employmentDuration);
        this.recommendations = recommendations;
    }
    
    /**
     * Create an employment check with basic information
     */
    public static EmploymentCheck of(EmploymentType employmentType, int employmentDuration, List<String> issues) {
        boolean passed = employmentType != EmploymentType.UNEMPLOYED && 
                        employmentDuration >= employmentType.getMinimumDurationMonths();
        
        return EmploymentCheck.builder()
                .passed(passed)
                .employmentType(employmentType)
                .employmentDuration(employmentDuration)
                .issues(issues)
                .stabilityRating(determineStabilityRating(employmentType, employmentDuration))
                .build();
    }
    
    /**
     * Create a passing employment check
     */
    public static EmploymentCheck passing(EmploymentType employmentType, int employmentDuration) {
        return EmploymentCheck.builder()
                .passed(true)
                .employmentType(employmentType)
                .employmentDuration(employmentDuration)
                .issues(List.of())
                .stabilityRating(determineStabilityRating(employmentType, employmentDuration))
                .build();
    }
    
    /**
     * Create a failing employment check
     */
    public static EmploymentCheck failing(EmploymentType employmentType, int employmentDuration, List<String> issues) {
        return EmploymentCheck.builder()
                .passed(false)
                .employmentType(employmentType)
                .employmentDuration(employmentDuration)
                .issues(issues)
                .stabilityRating(determineStabilityRating(employmentType, employmentDuration))
                .recommendations(generateRecommendations(employmentType, employmentDuration))
                .build();
    }
    
    /**
     * Check if employment is stable (full-time for 2+ years)
     */
    public boolean isStableEmployment() {
        return employmentType == EmploymentType.FULL_TIME && employmentDuration >= 24;
    }
    
    /**
     * Check if employment is acceptable for loan purposes
     */
    public boolean isAcceptableEmployment() {
        return employmentType.isEligibleForLoans() && 
               employmentDuration >= employmentType.getMinimumDurationMonths();
    }
    
    /**
     * Check if employment requires additional verification
     */
    public boolean requiresAdditionalVerification() {
        return employmentType == EmploymentType.SELF_EMPLOYED || 
               employmentType == EmploymentType.CONTRACT ||
               employmentDuration < 24;
    }
    
    /**
     * Get employment duration in years
     */
    public double getEmploymentDurationYears() {
        return employmentDuration / 12.0;
    }
    
    /**
     * Get minimum duration requirement for this employment type
     */
    public int getMinimumDurationRequired() {
        return employmentType.getMinimumDurationMonths();
    }
    
    /**
     * Get duration gap (positive if above requirement, negative if below)
     */
    public int getDurationGap() {
        return employmentDuration - getMinimumDurationRequired();
    }
    
    /**
     * Check if employment qualifies for premium rates
     */
    public boolean qualifiesForPremiumRates() {
        return employmentType == EmploymentType.FULL_TIME && employmentDuration >= 36; // 3+ years
    }
    
    /**
     * Get employment stability score (0-100)
     */
    public int getStabilityScore() {
        int baseScore = switch (employmentType) {
            case FULL_TIME -> 80;
            case PART_TIME -> 60;
            case CONTRACT -> 50;
            case SELF_EMPLOYED -> 70;
            case GOVERNMENT -> 90;
            case UNEMPLOYED -> 0;
        };
        
        // Adjust for duration
        if (employmentDuration >= 60) {
            baseScore += 20; // 5+ years bonus
        } else if (employmentDuration >= 36) {
            baseScore += 15; // 3+ years bonus
        } else if (employmentDuration >= 24) {
            baseScore += 10; // 2+ years bonus
        } else if (employmentDuration >= 12) {
            baseScore += 5; // 1+ year bonus
        }
        
        return Math.min(100, baseScore);
    }
    
    /**
     * Get formatted employment display
     */
    public String getEmploymentDisplay() {
        return String.format("%s (%.1f years)", 
                employmentType.getDisplayName(), getEmploymentDurationYears());
    }
    
    /**
     * Get detailed assessment message
     */
    public String getAssessmentMessage() {
        if (passed) {
            return String.format("Employment type %s with %.1f years duration meets requirements. Stability rating: %s",
                    employmentType.getDisplayName(), getEmploymentDurationYears(), stabilityRating);
        } else {
            return String.format("Employment type %s with %.1f years duration does not meet requirements. Issues: %s",
                    employmentType.getDisplayName(), getEmploymentDurationYears(), String.join(", ", issues));
        }
    }
    
    /**
     * Get required documentation for this employment type
     */
    public List<String> getRequiredDocumentation() {
        return switch (employmentType) {
            case FULL_TIME, PART_TIME, GOVERNMENT -> List.of(
                    "Recent pay stubs (last 30 days)",
                    "Employment verification letter",
                    "W-2 forms (last 2 years)",
                    "Bank statements showing direct deposits"
            );
            case CONTRACT -> List.of(
                    "Current contract agreement",
                    "Payment history (last 6 months)",
                    "1099 forms (last 2 years)",
                    "Bank statements showing contract payments"
            );
            case SELF_EMPLOYED -> List.of(
                    "Tax returns (last 2 years)",
                    "Business bank statements (last 6 months)",
                    "Profit and loss statements",
                    "Business license/registration",
                    "CPA-prepared financial statements"
            );
            case UNEMPLOYED -> List.of(
                    "Unemployment benefits documentation",
                    "Alternative income sources",
                    "Asset documentation",
                    "Co-signer information"
            );
        };
    }
    
    /**
     * Get employment risk factors
     */
    public List<String> getRiskFactors() {
        return switch (employmentType) {
            case FULL_TIME -> employmentDuration < 24 ? 
                    List.of("Recent employment change") : List.of();
            case PART_TIME -> List.of("Part-time employment - variable income");
            case CONTRACT -> List.of("Contract employment - temporary nature", "Income variability");
            case SELF_EMPLOYED -> List.of("Self-employment - income variability", "Business risk");
            case GOVERNMENT -> List.of(); // Government employment is stable
            case UNEMPLOYED -> List.of("No current employment", "No regular income");
        };
    }
    
    /**
     * Get time until meeting minimum requirements
     */
    public String getTimeToMeetRequirements() {
        if (passed) {
            return "Already meets requirements";
        }
        
        if (employmentType == EmploymentType.UNEMPLOYED) {
            return "Must obtain employment";
        }
        
        int shortfall = getMinimumDurationRequired() - employmentDuration;
        if (shortfall <= 0) {
            return "Already meets duration requirement";
        }
        
        return String.format("%d months until minimum duration requirement is met", shortfall);
    }
    
    /**
     * Check if employment has seasonal variations
     */
    public boolean hasSeasonalVariations() {
        return employmentType == EmploymentType.CONTRACT || 
               employmentType == EmploymentType.PART_TIME;
    }
    
    /**
     * Get employment verification requirements
     */
    public String getVerificationRequirements() {
        return employmentType.getIncomeVerificationRequirement();
    }
    
    private static String determineStabilityRating(EmploymentType employmentType, int employmentDuration) {
        return switch (employmentType) {
            case FULL_TIME, GOVERNMENT -> {
                if (employmentDuration >= 60) yield "Excellent";
                if (employmentDuration >= 36) yield "Very Good";
                if (employmentDuration >= 24) yield "Good";
                if (employmentDuration >= 12) yield "Fair";
                yield "Poor";
            }
            case PART_TIME -> {
                if (employmentDuration >= 36) yield "Good";
                if (employmentDuration >= 24) yield "Fair";
                yield "Poor";
            }
            case CONTRACT -> {
                if (employmentDuration >= 24) yield "Fair";
                yield "Poor";
            }
            case SELF_EMPLOYED -> {
                if (employmentDuration >= 36) yield "Good";
                if (employmentDuration >= 24) yield "Fair";
                yield "Poor";
            }
            case UNEMPLOYED -> "Unacceptable";
        };
    }
    
    private static String generateRecommendations(EmploymentType employmentType, int employmentDuration) {
        return switch (employmentType) {
            case UNEMPLOYED -> "Obtain employment before applying for loan";
            case PART_TIME -> "Consider transitioning to full-time employment or provide additional income sources";
            case CONTRACT -> "Provide evidence of contract renewal or transition to permanent employment";
            case SELF_EMPLOYED -> "Provide comprehensive business documentation and consider business stability improvements";
            case FULL_TIME, GOVERNMENT -> "Wait for longer employment history before reapplying";
        };
    }
}