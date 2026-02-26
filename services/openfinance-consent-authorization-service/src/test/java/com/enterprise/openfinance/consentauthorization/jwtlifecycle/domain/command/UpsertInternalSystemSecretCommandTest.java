package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpsertInternalSystemSecretCommandTest {

    @Test
    void shouldRejectBlankInputs() {
        assertThatThrownBy(() -> new UpsertInternalSystemSecretCommand(" ", "secret", "INTERNAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Secret key");

        assertThatThrownBy(() -> new UpsertInternalSystemSecretCommand("KEY", " ", "INTERNAL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Secret value");
    }
}
