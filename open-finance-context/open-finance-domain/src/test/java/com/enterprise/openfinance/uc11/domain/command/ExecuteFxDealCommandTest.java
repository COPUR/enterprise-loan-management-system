package com.enterprise.openfinance.uc11.domain.command;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class ExecuteFxDealCommandTest {

    @Test
    void shouldCreateAndNormalizeCommand() {
        ExecuteFxDealCommand command = new ExecuteFxDealCommand(" TPP-001 ", " Q-1 ", " IDEMP-1 ", " ix-1 ");

        assertThat(command.tppId()).isEqualTo("TPP-001");
        assertThat(command.quoteId()).isEqualTo("Q-1");
        assertThat(command.idempotencyKey()).isEqualTo("IDEMP-1");
        assertThat(command.interactionId()).isEqualTo("ix-1");
        assertThat(command.requestFingerprint()).isEqualTo("Q-1");
    }

    @Test
    void shouldRejectInvalidCommand() {
        assertThatThrownBy(() -> new ExecuteFxDealCommand(" ", "Q-1", "IDEMP-1", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExecuteFxDealCommand("TPP-1", " ", "IDEMP-1", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExecuteFxDealCommand("TPP-1", "Q-1", " ", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
