package com.enterprise.openfinance.uc12.domain.command;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class CreateOnboardingAccountCommandTest {

    @Test
    void shouldNormalizeAndBuildRequestFingerprint() {
        CreateOnboardingAccountCommand command = new CreateOnboardingAccountCommand(
                " TPP-001 ",
                " ix-uc12-1 ",
                " IDEMP-001 ",
                "jwe:alice|7841987001|AE",
                "usd"
        );

        assertThat(command.tppId()).isEqualTo("TPP-001");
        assertThat(command.interactionId()).isEqualTo("ix-uc12-1");
        assertThat(command.idempotencyKey()).isEqualTo("IDEMP-001");
        assertThat(command.preferredCurrency()).isEqualTo("USD");
        assertThat(command.requestFingerprint())
                .contains("jwe:alice|7841987001|AE")
                .contains("USD");
    }

    @Test
    void shouldRejectInvalidInputs() {
        assertThatThrownBy(() -> new CreateOnboardingAccountCommand(
                " ", "ix", "id", "jwe:payload", "USD"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new CreateOnboardingAccountCommand(
                "TPP", " ", "id", "jwe:payload", "USD"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");

        assertThatThrownBy(() -> new CreateOnboardingAccountCommand(
                "TPP", "ix", " ", "jwe:payload", "USD"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey");

        assertThatThrownBy(() -> new CreateOnboardingAccountCommand(
                "TPP", "ix", "id", " ", "USD"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("encryptedKycPayload");

        assertThatThrownBy(() -> new CreateOnboardingAccountCommand(
                "TPP", "ix", "id", "jwe:payload", "US"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preferredCurrency");
    }
}
