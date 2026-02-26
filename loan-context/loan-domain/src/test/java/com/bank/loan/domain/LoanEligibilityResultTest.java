package com.bank.loan.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoanEligibilityResultTest {

    @Test
    void factoryMethodsShouldCreateConsistentDecisionStates() {
        LoanEligibilityResult approved = LoanEligibilityResult.approved("All criteria met", List.of("Income"));
        LoanEligibilityResult rejected = LoanEligibilityResult.rejected("Credit score too low");
        LoanEligibilityResult conditional = LoanEligibilityResult.conditionalApproval(
            "Need co-signer", List.of("Income"), List.of("Co-signer missing")
        );
        LoanEligibilityResult review = LoanEligibilityResult.requiresReview(
            "Manual underwriting", List.of("Credit"), List.of("Employment check pending")
        );

        assertThat(approved.isApproved()).isTrue();
        assertThat(approved.hasPassedAllChecks()).isTrue();
        assertThat(rejected.isRejected()).isTrue();
        assertThat(conditional.isConditionalApproval()).isTrue();
        assertThat(review.requiresReview()).isTrue();
    }

    @Test
    void summaryAndCountMethodsShouldExposeAssessmentDetails() {
        LoanEligibilityResult result = LoanEligibilityResult.rejected(
            "Debt-to-income ratio too high",
            List.of("Credit score meets minimum"),
            List.of("Debt-to-income ratio above threshold")
        );

        assertThat(result.getPassedCheckCount()).isEqualTo(1);
        assertThat(result.getFailedCheckCount()).isEqualTo(1);
        assertThat(result.getTotalCheckCount()).isEqualTo(2);
        assertThat(result.getSummary()).contains("Rejected");
        assertThat(result.getSummary()).contains("Debt-to-income ratio too high");
        assertThat(result.toString()).contains("failedChecks=1");
    }

    @Test
    void equalityShouldBeBasedOnAllFields() {
        LoanEligibilityResult left = LoanEligibilityResult.approved(
            "OK",
            List.of("A")
        );
        LoanEligibilityResult right = LoanEligibilityResult.approved(
            "OK",
            List.of("A")
        );

        assertThat(left).isEqualTo(right);
        assertThat(left.hashCode()).isEqualTo(right.hashCode());
    }
}
