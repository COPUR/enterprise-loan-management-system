package com.enterprise.openfinance.dynamiconboarding.infrastructure.security;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class PrefixKycDecryptionAdapterTest {

    @Test
    void shouldDecryptValidPayload() {
        PrefixKycDecryptionAdapter adapter = new PrefixKycDecryptionAdapter();

        OnboardingApplicantProfile profile = adapter.decrypt("jwe:Alice Ahmed|7841987001|AE", "ix-dynamic-onboarding-1");

        assertThat(profile.fullName()).isEqualTo("Alice Ahmed");
        assertThat(profile.nationalId()).isEqualTo("7841987001");
        assertThat(profile.countryCode()).isEqualTo("AE");
    }

    @Test
    void shouldRejectInvalidPayload() {
        PrefixKycDecryptionAdapter adapter = new PrefixKycDecryptionAdapter();

        assertThatThrownBy(() -> adapter.decrypt("bad-payload", "ix-dynamic-onboarding-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Decryption Failed");
    }
}
