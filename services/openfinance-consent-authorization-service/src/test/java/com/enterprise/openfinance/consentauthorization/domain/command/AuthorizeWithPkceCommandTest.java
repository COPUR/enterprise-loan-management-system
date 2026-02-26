package com.enterprise.openfinance.consentauthorization.domain.command;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class AuthorizeWithPkceCommandTest {

    @Test
    void shouldCreateCommandWhenRequiredFieldsExist() {
        assertThatCode(() -> new AuthorizeWithPkceCommand(
                "code",
                "client-app",
                "https://tpp.example/callback",
                "ReadAccounts",
                "state-1",
                "CONSENT-1",
                "challenge",
                "S256"
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingResponseType() {
        assertThatThrownBy(() -> new AuthorizeWithPkceCommand(
                " ",
                "client-app",
                "https://tpp.example/callback",
                "ReadAccounts",
                "state-1",
                "CONSENT-1",
                "challenge",
                "S256"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}

