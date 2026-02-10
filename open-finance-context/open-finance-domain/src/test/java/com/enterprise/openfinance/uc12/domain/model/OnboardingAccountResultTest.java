package com.enterprise.openfinance.uc12.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class OnboardingAccountResultTest {

    @Test
    void shouldCreateResult() {
        OnboardingAccountResult result = new OnboardingAccountResult(account("ACC-001"), false);

        assertThat(result.account().accountId()).isEqualTo("ACC-001");
        assertThat(result.idempotencyReplay()).isFalse();
    }

    @Test
    void shouldRejectNullAccount() {
        assertThatThrownBy(() -> new OnboardingAccountResult(null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("account");
    }

    private static OnboardingAccount account(String accountId) {
        return new OnboardingAccount(
                accountId,
                "TPP-001",
                "CIF-001",
                new OnboardingApplicantProfile("Alice Ahmed", "7841987001", "AE"),
                "USD",
                OnboardingAccountStatus.OPENED,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
