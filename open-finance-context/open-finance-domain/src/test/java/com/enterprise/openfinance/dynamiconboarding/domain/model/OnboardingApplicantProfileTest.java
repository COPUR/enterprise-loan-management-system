package com.enterprise.openfinance.dynamiconboarding.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class OnboardingApplicantProfileTest {

    @Test
    void shouldNormalizeProfile() {
        OnboardingApplicantProfile profile = new OnboardingApplicantProfile(" Alice Ahmed ", " 7841987001 ", " ae ");

        assertThat(profile.fullName()).isEqualTo("Alice Ahmed");
        assertThat(profile.nationalId()).isEqualTo("7841987001");
        assertThat(profile.countryCode()).isEqualTo("AE");
    }

    @Test
    void shouldRejectInvalidProfile() {
        assertThatThrownBy(() -> new OnboardingApplicantProfile(" ", "7841987001", "AE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fullName");

        assertThatThrownBy(() -> new OnboardingApplicantProfile("Alice Ahmed", " ", "AE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nationalId");

        assertThatThrownBy(() -> new OnboardingApplicantProfile("Alice Ahmed", "7841987001", "A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("countryCode");
    }
}
