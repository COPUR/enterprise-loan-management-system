package com.enterprise.openfinance.uc12.infrastructure.persistence;

import com.enterprise.openfinance.uc12.domain.model.OnboardingAccount;
import com.enterprise.openfinance.uc12.domain.model.OnboardingAccountStatus;
import com.enterprise.openfinance.uc12.domain.model.OnboardingApplicantProfile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryOnboardingAccountAdapterTest {

    @Test
    void shouldSaveAndFindAccount() {
        InMemoryOnboardingAccountAdapter adapter = new InMemoryOnboardingAccountAdapter();
        OnboardingAccount account = account("ACC-001");

        adapter.save(account);

        assertThat(adapter.findById("ACC-001")).contains(account);
        assertThat(adapter.findById("ACC-404")).isEmpty();
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
