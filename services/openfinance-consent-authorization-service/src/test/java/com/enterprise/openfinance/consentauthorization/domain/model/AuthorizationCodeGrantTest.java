package com.enterprise.openfinance.consentauthorization.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class AuthorizationCodeGrantTest {

    private static final Instant ISSUED_AT = Instant.parse("2026-02-11T10:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-02-11T10:05:00Z");

    @Test
    void shouldConsumeCodeOnlyOnce() {
        AuthorizationCodeGrant grant = sampleGrant();

        grant.consume(ISSUED_AT.plusSeconds(10));

        assertThat(grant.isConsumed()).isTrue();
        assertThat(grant.getConsumedAt()).isEqualTo(ISSUED_AT.plusSeconds(10));

        assertThatThrownBy(() -> grant.consume(ISSUED_AT.plusSeconds(20)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already consumed");
    }

    @Test
    void shouldTreatCodeAsExpiredAtTtlBoundary() {
        AuthorizationCodeGrant grant = sampleGrant();

        assertThat(grant.isExpired(EXPIRES_AT.minusMillis(1))).isFalse();
        assertThat(grant.isExpired(EXPIRES_AT)).isTrue();
    }

    @Test
    void shouldMatchClientAndRedirect() {
        AuthorizationCodeGrant grant = sampleGrant();

        assertThat(grant.matchesClientAndRedirect("client-app", "https://tpp.example/callback")).isTrue();
        assertThat(grant.matchesClientAndRedirect("other-client", "https://tpp.example/callback")).isFalse();
    }

    private static AuthorizationCodeGrant sampleGrant() {
        return new AuthorizationCodeGrant(
                "AUTH_CODE_1",
                "client-app",
                "https://tpp.example/callback",
                "ReadAccounts ReadBalances",
                "CONSENT-1",
                "challenge",
                "S256",
                ISSUED_AT,
                EXPIRES_AT
        );
    }
}

