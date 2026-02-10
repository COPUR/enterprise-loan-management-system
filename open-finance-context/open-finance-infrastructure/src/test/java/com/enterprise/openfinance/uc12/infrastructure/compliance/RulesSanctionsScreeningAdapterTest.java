package com.enterprise.openfinance.uc12.infrastructure.compliance;

import com.enterprise.openfinance.uc12.domain.model.OnboardingApplicantProfile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class RulesSanctionsScreeningAdapterTest {

    @Test
    void shouldDetectBlockedProfiles() {
        RulesSanctionsScreeningAdapter adapter = new RulesSanctionsScreeningAdapter(Set.of("TEST_BLOCKED", "SANCTIONED_ENTITY"));

        boolean blocked = adapter.isBlocked(new OnboardingApplicantProfile("TEST_BLOCKED", "7841987001", "AE"), "ix-uc12-1");
        boolean allowed = adapter.isBlocked(new OnboardingApplicantProfile("Alice Ahmed", "7841987001", "AE"), "ix-uc12-1");

        assertThat(blocked).isTrue();
        assertThat(allowed).isFalse();
    }
}
