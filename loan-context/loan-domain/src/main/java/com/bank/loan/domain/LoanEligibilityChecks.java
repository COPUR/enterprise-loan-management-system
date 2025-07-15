package com.bank.loan.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder-style helper class for accumulating eligibility check results
 * 
 * This class serves as a collector for eligibility assessment results,
 * providing a clean way to accumulate passed and failed checks before
 * building the final LoanEligibilityResult.
 * 
 * GRASP Principles:
 * - Creator: Creates LoanEligibilityResult instances
 * - Low Coupling: Simple data collection without complex dependencies
 * - High Cohesion: Focused on collecting and organizing check results
 */
public class LoanEligibilityChecks {
    
    private final List<String> passedChecks;
    private final List<String> failedChecks;
    
    public LoanEligibilityChecks() {
        this.passedChecks = new ArrayList<>();
        this.failedChecks = new ArrayList<>();
    }
    
    /**
     * Add a check that passed
     */
    public void addPassedCheck(String checkDescription) {
        if (checkDescription != null && !checkDescription.trim().isEmpty()) {
            passedChecks.add(checkDescription.trim());
        }
    }
    
    /**
     * Add a check that failed
     */
    public void addFailedCheck(String checkDescription) {
        if (checkDescription != null && !checkDescription.trim().isEmpty()) {
            failedChecks.add(checkDescription.trim());
        }
    }
    
    /**
     * Check if all eligibility checks passed
     */
    public boolean hasPassedAllChecks() {
        return failedChecks.isEmpty() && !passedChecks.isEmpty();
    }
    
    /**
     * Check if any checks failed
     */
    public boolean hasFailedChecks() {
        return !failedChecks.isEmpty();
    }
    
    /**
     * Get immutable copy of passed checks
     */
    public List<String> getPassedChecks() {
        return List.copyOf(passedChecks);
    }
    
    /**
     * Get immutable copy of failed checks
     */
    public List<String> getFailedChecks() {
        return List.copyOf(failedChecks);
    }
    
    /**
     * Get count of passed checks
     */
    public int getPassedCheckCount() {
        return passedChecks.size();
    }
    
    /**
     * Get count of failed checks
     */
    public int getFailedCheckCount() {
        return failedChecks.size();
    }
    
    /**
     * Get total number of checks performed
     */
    public int getTotalCheckCount() {
        return passedChecks.size() + failedChecks.size();
    }
    
    /**
     * Build the final LoanEligibilityResult based on accumulated checks
     */
    public LoanEligibilityResult buildResult() {
        if (failedChecks.isEmpty() && !passedChecks.isEmpty()) {
            // All checks passed
            return LoanEligibilityResult.approved(
                "All eligibility criteria met", 
                getPassedChecks()
            );
        } else if (!failedChecks.isEmpty()) {
            // Some checks failed
            String primaryReason = determinePrimaryReason();
            
            if (hasHighPriorityFailures()) {
                return LoanEligibilityResult.rejected(
                    primaryReason, 
                    getPassedChecks(), 
                    getFailedChecks()
                );
            } else if (hasModeratePriorityFailures()) {
                return LoanEligibilityResult.requiresReview(
                    primaryReason + " - Manual review required", 
                    getPassedChecks(), 
                    getFailedChecks()
                );
            } else {
                return LoanEligibilityResult.conditionalApproval(
                    primaryReason + " - Conditional approval with requirements", 
                    getPassedChecks(), 
                    getFailedChecks()
                );
            }
        } else {
            // No checks were performed
            return LoanEligibilityResult.requiresReview(
                "No eligibility checks were performed", 
                getPassedChecks(), 
                getFailedChecks()
            );
        }
    }
    
    /**
     * Determine the primary reason for the eligibility decision
     */
    private String determinePrimaryReason() {
        if (failedChecks.isEmpty()) {
            return "All eligibility criteria satisfied";
        }
        
        // Return the first failed check as primary reason
        // In a more sophisticated implementation, this could prioritize
        // certain types of failures over others
        return failedChecks.get(0);
    }
    
    /**
     * Check if there are high priority failures that require immediate rejection
     */
    private boolean hasHighPriorityFailures() {
        return failedChecks.stream().anyMatch(check -> 
            check.toLowerCase().contains("credit score") ||
            check.toLowerCase().contains("age") ||
            check.toLowerCase().contains("active") ||
            check.toLowerCase().contains("income") ||
            check.toLowerCase().contains("debt-to-income")
        );
    }
    
    /**
     * Check if there are moderate priority failures that require review
     */
    private boolean hasModeratePriorityFailures() {
        return failedChecks.stream().anyMatch(check -> 
            check.toLowerCase().contains("employment") ||
            check.toLowerCase().contains("loan") ||
            check.toLowerCase().contains("amount")
        );
    }
    
    /**
     * Clear all accumulated checks (useful for reusing the same instance)
     */
    public void clear() {
        passedChecks.clear();
        failedChecks.clear();
    }
    
    /**
     * Get a summary of current check status
     */
    public String getCheckSummary() {
        return String.format("Eligibility Checks: %d passed, %d failed, %d total", 
                           passedChecks.size(), failedChecks.size(), getTotalCheckCount());
    }
    
    @Override
    public String toString() {
        return String.format("LoanEligibilityChecks{passed=%d, failed=%d, total=%d}", 
                           passedChecks.size(), failedChecks.size(), getTotalCheckCount());
    }
}