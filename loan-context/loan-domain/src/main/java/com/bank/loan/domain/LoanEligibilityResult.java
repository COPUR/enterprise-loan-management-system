package com.bank.loan.domain;

import java.util.List;
import java.util.Objects;

/**
 * Value Object representing the result of a loan eligibility assessment
 * 
 * Immutable value object that encapsulates:
 * - Approval decision
 * - Reasoning behind the decision
 * - Detailed eligibility check results
 * 
 * GRASP Principles:
 * - Information Expert: Knows everything about eligibility decision
 * - Low Coupling: Self-contained with minimal dependencies
 * - High Cohesion: All data related to eligibility result
 */
public final class LoanEligibilityResult {
    
    private final boolean approved;
    private final String primaryReason;
    private final List<String> passedChecks;
    private final List<String> failedChecks;
    private final EligibilityDecision decision;
    
    private LoanEligibilityResult(boolean approved, String primaryReason, 
                                List<String> passedChecks, List<String> failedChecks,
                                EligibilityDecision decision) {
        this.approved = approved;
        this.primaryReason = Objects.requireNonNull(primaryReason, "Primary reason cannot be null");
        this.passedChecks = List.copyOf(Objects.requireNonNull(passedChecks, "Passed checks cannot be null"));
        this.failedChecks = List.copyOf(Objects.requireNonNull(failedChecks, "Failed checks cannot be null"));
        this.decision = Objects.requireNonNull(decision, "Decision cannot be null");
    }
    
    /**
     * Create an approved eligibility result
     */
    public static LoanEligibilityResult approved(String reason, List<String> passedChecks) {
        return new LoanEligibilityResult(true, reason, passedChecks, List.of(), EligibilityDecision.APPROVED);
    }
    
    /**
     * Create a rejected eligibility result
     */
    public static LoanEligibilityResult rejected(String reason) {
        return new LoanEligibilityResult(false, reason, List.of(), List.of(reason), EligibilityDecision.REJECTED);
    }
    
    /**
     * Create a rejected eligibility result with detailed checks
     */
    public static LoanEligibilityResult rejected(String primaryReason, 
                                                List<String> passedChecks, 
                                                List<String> failedChecks) {
        return new LoanEligibilityResult(false, primaryReason, passedChecks, failedChecks, EligibilityDecision.REJECTED);
    }
    
    /**
     * Create a conditional approval result (approved with conditions)
     */
    public static LoanEligibilityResult conditionalApproval(String conditions, 
                                                           List<String> passedChecks, 
                                                           List<String> failedChecks) {
        return new LoanEligibilityResult(true, conditions, passedChecks, failedChecks, EligibilityDecision.CONDITIONAL);
    }
    
    /**
     * Create a result requiring further review
     */
    public static LoanEligibilityResult requiresReview(String reason, 
                                                      List<String> passedChecks, 
                                                      List<String> failedChecks) {
        return new LoanEligibilityResult(false, reason, passedChecks, failedChecks, EligibilityDecision.REQUIRES_REVIEW);
    }
    
    // Getters
    
    public boolean isApproved() {
        return approved;
    }
    
    public boolean isRejected() {
        return !approved && decision == EligibilityDecision.REJECTED;
    }
    
    public boolean isConditionalApproval() {
        return approved && decision == EligibilityDecision.CONDITIONAL;
    }
    
    public boolean requiresReview() {
        return decision == EligibilityDecision.REQUIRES_REVIEW;
    }
    
    public String getPrimaryReason() {
        return primaryReason;
    }
    
    public List<String> getPassedChecks() {
        return passedChecks;
    }
    
    public List<String> getFailedChecks() {
        return failedChecks;
    }
    
    public EligibilityDecision getDecision() {
        return decision;
    }
    
    /**
     * Get a summary of the eligibility assessment
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Eligibility Assessment: ").append(decision.getDisplayName());
        summary.append("\nReason: ").append(primaryReason);
        
        if (!passedChecks.isEmpty()) {
            summary.append("\nPassed Checks (").append(passedChecks.size()).append("):");
            passedChecks.forEach(check -> summary.append("\n  ✓ ").append(check));
        }
        
        if (!failedChecks.isEmpty()) {
            summary.append("\nFailed Checks (").append(failedChecks.size()).append("):");
            failedChecks.forEach(check -> summary.append("\n  ✗ ").append(check));
        }
        
        return summary.toString();
    }
    
    /**
     * Check if all eligibility criteria were met
     */
    public boolean hasPassedAllChecks() {
        return failedChecks.isEmpty();
    }
    
    /**
     * Get the number of checks that passed
     */
    public int getPassedCheckCount() {
        return passedChecks.size();
    }
    
    /**
     * Get the number of checks that failed
     */
    public int getFailedCheckCount() {
        return failedChecks.size();
    }
    
    /**
     * Get the total number of checks performed
     */
    public int getTotalCheckCount() {
        return passedChecks.size() + failedChecks.size();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        LoanEligibilityResult that = (LoanEligibilityResult) obj;
        return approved == that.approved &&
               Objects.equals(primaryReason, that.primaryReason) &&
               Objects.equals(passedChecks, that.passedChecks) &&
               Objects.equals(failedChecks, that.failedChecks) &&
               decision == that.decision;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(approved, primaryReason, passedChecks, failedChecks, decision);
    }
    
    @Override
    public String toString() {
        return String.format("LoanEligibilityResult{approved=%s, decision=%s, primaryReason='%s', " +
                           "passedChecks=%d, failedChecks=%d}", 
                           approved, decision, primaryReason, passedChecks.size(), failedChecks.size());
    }
    
    /**
     * Enumeration of possible eligibility decisions
     */
    public enum EligibilityDecision {
        APPROVED("Approved"),
        REJECTED("Rejected"), 
        CONDITIONAL("Conditional Approval"),
        REQUIRES_REVIEW("Requires Manual Review");
        
        private final String displayName;
        
        EligibilityDecision(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}