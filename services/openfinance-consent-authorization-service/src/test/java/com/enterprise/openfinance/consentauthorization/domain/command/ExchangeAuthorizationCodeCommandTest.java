package com.enterprise.openfinance.consentauthorization.domain.command;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class ExchangeAuthorizationCodeCommandTest {

    @Test
    void shouldCreateCommandWhenRequiredFieldsExist() {
        assertThatCode(() -> new ExchangeAuthorizationCodeCommand(
                "authorization_code",
                "AUTH_CODE_1",
                "verifier",
                "client-app",
                "https://tpp.example/callback"
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingGrantType() {
        assertThatThrownBy(() -> new ExchangeAuthorizationCodeCommand(
                " ",
                "AUTH_CODE_1",
                "verifier",
                "client-app",
                "https://tpp.example/callback"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}

