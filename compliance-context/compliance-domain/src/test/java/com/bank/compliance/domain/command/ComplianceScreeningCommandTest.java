package com.bank.compliance.domain.command;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComplianceScreeningCommandTest {

    @Test
    void shouldRejectInvalidCommandInput() {
        assertThatThrownBy(() -> new ComplianceScreeningCommand("", "C1", new BigDecimal("10"), false, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");

        assertThatThrownBy(() -> new ComplianceScreeningCommand("TX", "", new BigDecimal("10"), false, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId");

        assertThatThrownBy(() -> new ComplianceScreeningCommand("TX", "C1", BigDecimal.ZERO, false, true, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void shouldCreateValidCommand() {
        assertThatCode(() -> new ComplianceScreeningCommand("TX", "C1", new BigDecimal("10"), false, true, false))
                .doesNotThrowAnyException();
    }
}
