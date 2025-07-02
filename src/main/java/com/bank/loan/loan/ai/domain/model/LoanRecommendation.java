package com.bank.loan.loan.ai.domain.model;

/**
 * Loan Recommendation enumeration for AI analysis results
 */
public enum LoanRecommendation {
    APPROVE("Approve - Low risk, meets all criteria"),
    APPROVE_WITH_CONDITIONS("Approve with Conditions - Moderate risk, additional requirements needed"),
    COUNTER_OFFER("Counter Offer - Different terms recommended"),
    REQUIRE_REVIEW("Require Manual Review - Complex case needs human evaluation"),
    DENY("Deny - High risk, does not meet criteria");

    private final String description;

    LoanRecommendation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if recommendation is positive (any form of approval)
     */
    public boolean isPositive() {
        return this == APPROVE || this == APPROVE_WITH_CONDITIONS || this == COUNTER_OFFER;
    }

    /**
     * Check if recommendation requires human intervention
     */
    public boolean requiresHumanReview() {
        return this == REQUIRE_REVIEW || this == APPROVE_WITH_CONDITIONS;
    }

    /**
     * Check if recommendation is automated approval
     */
    public boolean isAutomatedApproval() {
        return this == APPROVE;
    }

    /**
     * Get priority level for processing (1-5, higher needs immediate attention)
     */
    public int getPriorityLevel() {
        return switch (this) {
            case DENY -> 1;
            case APPROVE -> 2;
            case COUNTER_OFFER -> 3;
            case APPROVE_WITH_CONDITIONS -> 4;
            case REQUIRE_REVIEW -> 5;
        };
    }

    /**
     * Get next action required for this recommendation
     */
    public String getNextAction() {
        return switch (this) {
            case APPROVE -> "Generate loan agreement";
            case APPROVE_WITH_CONDITIONS -> "Review conditions and verify requirements";
            case COUNTER_OFFER -> "Present alternative terms to customer";
            case REQUIRE_REVIEW -> "Schedule manual underwriting review";
            case DENY -> "Send denial notification with reasons";
        };
    }
}