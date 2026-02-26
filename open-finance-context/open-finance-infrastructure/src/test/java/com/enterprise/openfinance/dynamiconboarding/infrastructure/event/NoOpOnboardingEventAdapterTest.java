package com.enterprise.openfinance.dynamiconboarding.infrastructure.event;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccount;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountStatus;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

@Tag("unit")
class NoOpOnboardingEventAdapterTest {

    @Test
    void shouldAllowNoOpPublishing() {
        NoOpOnboardingEventAdapter adapter = new NoOpOnboardingEventAdapter();
        OnboardingAccount account = new OnboardingAccount(
                "ACC-001",
                "TPP-001",
                "CIF-001",
                new OnboardingApplicantProfile("Alice Ahmed", "7841987001", "AE"),
                "USD",
                OnboardingAccountStatus.OPENED,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );

        adapter.publishAccountOpened(account);
        adapter.publishOnboardingRejected(new OnboardingApplicantProfile("TEST_BLOCKED", "7841987002", "AE"), "TPP-001", "SANCTIONS_HIT");
    }
}
