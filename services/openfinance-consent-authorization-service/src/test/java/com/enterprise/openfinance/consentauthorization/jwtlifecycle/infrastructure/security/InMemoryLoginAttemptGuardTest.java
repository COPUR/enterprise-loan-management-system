package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalAuthenticationException;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config.InternalSecurityProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryLoginAttemptGuardTest {

    @Test
    void shouldAllowWhenNoPriorAttemptsOrAfterReset() {
        InternalSecurityProperties properties = defaultProperties();
        InMemoryLoginAttemptGuard guard = new InMemoryLoginAttemptGuard(properties);
        Instant now = Instant.parse("2026-02-25T00:00:00Z");

        assertThatCode(() -> guard.checkAllowed("svc", now)).doesNotThrowAnyException();
        guard.recordFailure("svc", now);
        guard.reset("svc");
        assertThatCode(() -> guard.checkAllowed("svc", now.plusSeconds(1))).doesNotThrowAnyException();
    }

    @Test
    void shouldLockAfterConfiguredFailures() {
        InternalSecurityProperties properties = defaultProperties();
        properties.setMaxFailedAttempts(2);
        InMemoryLoginAttemptGuard guard = new InMemoryLoginAttemptGuard(properties);
        Instant now = Instant.parse("2026-02-25T00:00:00Z");

        guard.recordFailure("svc", now);
        guard.recordFailure("svc", now.plusSeconds(1));

        assertThatThrownBy(() -> guard.checkAllowed("svc", now.plusSeconds(2)))
                .isInstanceOf(InternalAuthenticationException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void shouldResetFailureWindowWhenFailuresAreOld() {
        InternalSecurityProperties properties = defaultProperties();
        InMemoryLoginAttemptGuard guard = new InMemoryLoginAttemptGuard(properties);
        Instant now = Instant.parse("2026-02-25T00:00:00Z");

        guard.recordFailure("svc", now);
        guard.recordFailure("svc", now.plus(properties.getFailedAttemptWindow()).plusSeconds(1));

        assertThatCode(() -> guard.checkAllowed("svc", now.plus(properties.getFailedAttemptWindow()).plusSeconds(2)))
                .doesNotThrowAnyException();
    }

    private static InternalSecurityProperties defaultProperties() {
        InternalSecurityProperties properties = new InternalSecurityProperties();
        properties.setJwtHmacSecret("0123456789abcdef0123456789abcdef");
        properties.setInternalPassword("runtime-managed-password");
        properties.setInternalUsername("runtime-managed-user");
        return properties;
    }
}
