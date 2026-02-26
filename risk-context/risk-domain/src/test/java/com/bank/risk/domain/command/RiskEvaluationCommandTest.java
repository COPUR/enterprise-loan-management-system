package com.bank.risk.domain.command;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiskEvaluationCommandTest {

    @Test
    void shouldRejectInvalidInputs() {
        assertThatThrownBy(() -> new RiskEvaluationCommand("", new BigDecimal("10"), "AED", false, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");

        assertThatThrownBy(() -> new RiskEvaluationCommand("TX-1", BigDecimal.ZERO, "AED", false, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> new RiskEvaluationCommand("TX-1", new BigDecimal("10"), "", false, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");

        assertThatThrownBy(() -> new RiskEvaluationCommand("TX-1", new BigDecimal("10"), "AED", false, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("velocityScore");
    }

    @Test
    void shouldCreateCommandForValidInput() {
        assertThatCode(() -> new RiskEvaluationCommand("TX-1", new BigDecimal("100"), "AED", true, 90))
                .doesNotThrowAnyException();
    }
}
