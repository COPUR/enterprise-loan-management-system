package com.bank.loan.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoanEligibilityChecksTest {

    @Test
    void shouldBuildApprovedResultWhenAllChecksPass() {
        LoanEligibilityChecks checks = new LoanEligibilityChecks();
        checks.addPassedCheck("Credit score meets minimum requirement");
        checks.addPassedCheck("Income requirement met");

        LoanEligibilityResult result = checks.buildResult();

        assertThat(result.isApproved()).isTrue();
        assertThat(result.getDecision()).isEqualTo(LoanEligibilityResult.EligibilityDecision.APPROVED);
        assertThat(result.getPassedCheckCount()).isEqualTo(2);
        assertThat(result.getFailedCheckCount()).isZero();
    }

    @Test
    void shouldBuildRejectedResultForHighPriorityFailure() {
        LoanEligibilityChecks checks = new LoanEligibilityChecks();
        checks.addPassedCheck("Age requirements met");
        checks.addFailedCheck("Credit score (580) is below minimum requirement (600)");

        LoanEligibilityResult result = checks.buildResult();

        assertThat(result.isRejected()).isTrue();
        assertThat(result.getPrimaryReason()).contains("Credit score");
    }

    @Test
    void shouldBuildRequiresReviewForModeratePriorityFailure() {
        LoanEligibilityChecks checks = new LoanEligibilityChecks();
        checks.addPassedCheck("Credit score meets minimum requirement");
        checks.addFailedCheck("Employment stability requirement not met");

        LoanEligibilityResult result = checks.buildResult();

        assertThat(result.requiresReview()).isTrue();
        assertThat(result.getPrimaryReason()).contains("Manual review required");
    }

    @Test
    void shouldBuildConditionalApprovalForLowPriorityFailure() {
        LoanEligibilityChecks checks = new LoanEligibilityChecks();
        checks.addPassedCheck("Credit score meets minimum requirement");
        checks.addFailedCheck("Additional documentation required");

        LoanEligibilityResult result = checks.buildResult();

        assertThat(result.isConditionalApproval()).isTrue();
        assertThat(result.getPrimaryReason()).contains("Conditional approval");
    }

    @Test
    void shouldRequireReviewWhenNoChecksPerformed() {
        LoanEligibilityChecks checks = new LoanEligibilityChecks();

        LoanEligibilityResult result = checks.buildResult();

        assertThat(result.requiresReview()).isTrue();
        assertThat(result.getPrimaryReason()).contains("No eligibility checks");
    }

    @Test
    void helperMethodsShouldExposeCountsAndImmutableViews() {
        LoanEligibilityChecks checks = new LoanEligibilityChecks();
        checks.addPassedCheck("  Income requirement met  ");
        checks.addPassedCheck(null);
        checks.addFailedCheck("Age requirement failed");
        checks.addFailedCheck(" ");

        assertThat(checks.hasPassedAllChecks()).isFalse();
        assertThat(checks.hasFailedChecks()).isTrue();
        assertThat(checks.getPassedCheckCount()).isEqualTo(1);
        assertThat(checks.getFailedCheckCount()).isEqualTo(1);
        assertThat(checks.getTotalCheckCount()).isEqualTo(2);
        assertThat(checks.getPassedChecks()).isEqualTo(List.of("Income requirement met"));
        assertThat(checks.getCheckSummary()).contains("1 passed, 1 failed, 2 total");
        assertThat(checks.toString()).contains("passed=1");

        checks.clear();
        assertThat(checks.getTotalCheckCount()).isZero();
    }
}
