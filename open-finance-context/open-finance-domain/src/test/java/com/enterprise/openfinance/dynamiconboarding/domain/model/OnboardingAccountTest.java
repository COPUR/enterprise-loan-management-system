package com.enterprise.openfinance.dynamiconboarding.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class OnboardingAccountTest {

    @Test
    void shouldCreateAccountAndSupportOwnershipAndRejectionTransition() {
        OnboardingApplicantProfile profile = new OnboardingApplicantProfile("Alice Ahmed", "7841987001", "AE");
        OnboardingAccount account = new OnboardingAccount(
                "ACC-001",
                "TPP-001",
                "CIF-001",
                profile,
                "USD",
                OnboardingAccountStatus.OPENED,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );

        OnboardingAccount rejected = account.reject("MANUAL_REVIEW_FAILED", Instant.parse("2026-02-09T10:05:00Z"));

        assertThat(account.belongsTo("TPP-001")).isTrue();
        assertThat(account.belongsTo("TPP-OTHER")).isFalse();
        assertThat(rejected.status()).isEqualTo(OnboardingAccountStatus.REJECTED);
        assertThat(rejected.rejectionReason()).isEqualTo("MANUAL_REVIEW_FAILED");
    }

    @Test
    void shouldReturnSameInstanceWhenAlreadyRejected() {
        OnboardingApplicantProfile profile = new OnboardingApplicantProfile("Alice Ahmed", "7841987001", "AE");
        OnboardingAccount rejected = new OnboardingAccount(
                "ACC-001",
                "TPP-001",
                "CIF-001",
                profile,
                "USD",
                OnboardingAccountStatus.REJECTED,
                "SANCTIONS",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );

        OnboardingAccount result = rejected.reject("SHOULD_NOT_CHANGE", Instant.parse("2026-02-09T10:10:00Z"));
        assertThat(result).isSameAs(rejected);
    }

    @Test
    void shouldRejectInvalidAccount() {
        OnboardingApplicantProfile profile = new OnboardingApplicantProfile("Alice Ahmed", "7841987001", "AE");

        assertThatThrownBy(() -> new OnboardingAccount(
                " ",
                "TPP-001",
                "CIF-001",
                profile,
                "USD",
                OnboardingAccountStatus.OPENED,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new OnboardingAccount(
                "ACC-001",
                "TPP-001",
                "CIF-001",
                null,
                "USD",
                OnboardingAccountStatus.OPENED,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicantProfile");
    }
}
