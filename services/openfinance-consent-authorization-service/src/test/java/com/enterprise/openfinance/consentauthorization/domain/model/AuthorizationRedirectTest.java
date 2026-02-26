package com.enterprise.openfinance.consentauthorization.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class AuthorizationRedirectTest {

    @Test
    void shouldCreateAuthorizationRedirectForValidValues() {
        assertThatCode(() -> new AuthorizationRedirect(
                "AUTH_CODE_1",
                "state-1",
                "https://tpp.example/callback?code=AUTH_CODE_1",
                Instant.parse("2026-02-11T10:05:00Z")
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectBlankRedirectUri() {
        assertThatThrownBy(() -> new AuthorizationRedirect(
                "AUTH_CODE_1",
                "state-1",
                " ",
                Instant.parse("2026-02-11T10:05:00Z")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}

