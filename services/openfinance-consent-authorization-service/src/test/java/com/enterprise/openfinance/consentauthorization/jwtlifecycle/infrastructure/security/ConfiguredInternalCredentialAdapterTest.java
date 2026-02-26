package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config.InternalSecurityProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredInternalCredentialAdapterTest {

    @Test
    void shouldMatchOnlyConfiguredCredentials() {
        InternalSecurityProperties properties = new InternalSecurityProperties();
        properties.setInternalUsername("svc-user");
        properties.setInternalPassword("svc-pass");
        ConfiguredInternalCredentialAdapter adapter = new ConfiguredInternalCredentialAdapter(properties);

        assertThat(adapter.matches("svc-user", "svc-pass")).isTrue();
        assertThat(adapter.matches("svc-user", "bad-pass")).isFalse();
        assertThat(adapter.matches("other-user", "svc-pass")).isFalse();
        assertThat(adapter.matches(null, "svc-pass")).isFalse();
    }
}
