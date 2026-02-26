package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalTokenSessionTest {

    @Test
    void shouldCreateDeactivateAndExposeAccessors() {
        Instant issuedAt = Instant.parse("2026-02-25T00:00:00Z");
        Instant expiresAt = issuedAt.plusSeconds(60);
        InternalTokenSession session = InternalTokenSession.active("jti-1", "svc-user", issuedAt, expiresAt);

        assertThat(session.getJti()).isEqualTo("jti-1");
        assertThat(session.getSubject()).isEqualTo("svc-user");
        assertThat(session.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(session.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(session.isActive(issuedAt.plusSeconds(1))).isTrue();

        session.deactivate(issuedAt.plusSeconds(2));
        assertThat(session.isActiveFlag()).isFalse();
        assertThat(session.getRevokedAt()).isEqualTo(issuedAt.plusSeconds(2));
        assertThat(session.isActive(issuedAt.plusSeconds(3))).isFalse();
    }

    @Test
    void shouldRejectInvalidInputs() {
        Instant issuedAt = Instant.parse("2026-02-25T00:00:00Z");

        assertThatThrownBy(() -> InternalTokenSession.active(" ", "svc-user", issuedAt, issuedAt.plusSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jti");

        assertThatThrownBy(() -> InternalTokenSession.active("jti", " ", issuedAt, issuedAt.plusSeconds(10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");

        assertThatThrownBy(() -> InternalTokenSession.active("jti", "svc-user", issuedAt, issuedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt");
    }
}
