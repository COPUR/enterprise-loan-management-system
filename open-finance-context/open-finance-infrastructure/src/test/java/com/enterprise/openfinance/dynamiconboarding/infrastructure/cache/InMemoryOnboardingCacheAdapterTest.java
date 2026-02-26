package com.enterprise.openfinance.dynamiconboarding.infrastructure.cache;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccount;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountItemResult;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountStatus;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryOnboardingCacheAdapterTest {

    @Test
    void shouldCacheAndExpireEntries() {
        InMemoryOnboardingCacheAdapter adapter = new InMemoryOnboardingCacheAdapter(10);
        OnboardingAccountItemResult result = new OnboardingAccountItemResult(account("ACC-001"), false);

        adapter.putAccount("ACC-001:TPP-001", result, Instant.parse("2026-02-09T10:10:00Z"));

        assertThat(adapter.getAccount("ACC-001:TPP-001", Instant.parse("2026-02-09T10:05:00Z")))
                .get()
                .extracting(OnboardingAccountItemResult::cacheHit)
                .isEqualTo(true);
        assertThat(adapter.getAccount("ACC-001:TPP-001", Instant.parse("2026-02-09T10:10:00Z"))).isEmpty();
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
