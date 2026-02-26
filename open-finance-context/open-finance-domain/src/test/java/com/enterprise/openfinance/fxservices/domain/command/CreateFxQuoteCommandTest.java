package com.enterprise.openfinance.fxservices.domain.command;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class CreateFxQuoteCommandTest {

    @Test
    void shouldCreateAndNormalizeCommand() {
        CreateFxQuoteCommand command = new CreateFxQuoteCommand(
                " TPP-001 ",
                " ix-1 ",
                "aed",
                "usd",
                new BigDecimal("1000.00")
        );

        assertThat(command.tppId()).isEqualTo("TPP-001");
        assertThat(command.interactionId()).isEqualTo("ix-1");
        assertThat(command.sourceCurrency()).isEqualTo("AED");
        assertThat(command.targetCurrency()).isEqualTo("USD");
        assertThat(command.pair()).isEqualTo("AED-USD");
    }

    @Test
    void shouldRejectInvalidCommand() {
        assertThatThrownBy(() -> new CreateFxQuoteCommand(" ", "ix-1", "AED", "USD", new BigDecimal("1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateFxQuoteCommand("TPP-1", "ix-1", "AE", "USD", new BigDecimal("1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateFxQuoteCommand("TPP-1", "ix-1", "AED", "AED", new BigDecimal("1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CreateFxQuoteCommand("TPP-1", "ix-1", "AED", "USD", new BigDecimal("0")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
