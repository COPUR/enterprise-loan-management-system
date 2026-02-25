package com.enterprise.openfinance.consentauthorization.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class TokenResultTest {

    @Test
    void shouldCreateTokenResultForValidValues() {
        assertThatCode(() -> new TokenResult(
                "access",
                "Bearer",
                900,
                "refresh",
                "ReadAccounts"
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidExpiresIn() {
        assertThatThrownBy(() -> new TokenResult(
                "access",
                "Bearer",
                0,
                "refresh",
                "ReadAccounts"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}

