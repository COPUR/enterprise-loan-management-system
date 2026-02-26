package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InternalSecurityPropertiesTest {

    @Test
    void shouldExposeConfiguredValuesThroughAccessors() {
        InternalSecurityProperties properties = new InternalSecurityProperties();
        properties.setIssuer("issuer");
        properties.setAudience("audience");
        properties.setJwtHmacSecret("0123456789abcdef0123456789abcdef");
        properties.setAccessTokenTtl(Duration.ofMinutes(5));
        properties.setAllowedClockSkew(Duration.ofSeconds(20));
        properties.setInternalUsername("runtime-user");
        properties.setInternalPassword("runtime-password");
        properties.setMaxFailedAttempts(7);
        properties.setFailedAttemptWindow(Duration.ofMinutes(10));
        properties.setLockDuration(Duration.ofMinutes(3));

        assertThat(properties.getIssuer()).isEqualTo("issuer");
        assertThat(properties.getAudience()).isEqualTo("audience");
        assertThat(properties.getJwtHmacSecret()).isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(properties.getAccessTokenTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.getAllowedClockSkew()).isEqualTo(Duration.ofSeconds(20));
        assertThat(properties.getInternalUsername()).isEqualTo("runtime-user");
        assertThat(properties.getInternalPassword()).isEqualTo("runtime-password");
        assertThat(properties.getMaxFailedAttempts()).isEqualTo(7);
        assertThat(properties.getFailedAttemptWindow()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.getLockDuration()).isEqualTo(Duration.ofMinutes(3));
    }
}
