package com.enterprise.openfinance.uc10.domain.command;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class AcceptMotorQuoteCommandTest {

    @Test
    void shouldCreateAndNormalizeAcceptCommand() {
        AcceptMotorQuoteCommand command = new AcceptMotorQuoteCommand(
                "TPP-001",
                "Q-1",
                "IDEMP-1",
                "ix-1",
                " accept ",
                " PAY-1 ",
                "Toyota",
                "Camry",
                2023,
                35,
                10
        );

        assertThat(command.action()).isEqualTo("ACCEPT");
        assertThat(command.paymentReference()).isEqualTo("PAY-1");
        assertThat(command.hasRiskSnapshot()).isTrue();
        assertThat(command.riskFingerprint()).contains("TOYOTA|CAMRY|2023|35|10");
    }

    @Test
    void shouldRejectInvalidAcceptCommand() {
        assertThatThrownBy(() -> new AcceptMotorQuoteCommand(
                "TPP-001", "Q-1", "IDEMP-1", "ix-1", "REJECT", "PAY-1",
                null, null, null, null, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACCEPT");

        assertThatThrownBy(() -> new AcceptMotorQuoteCommand(
                "TPP-001", "Q-1", "IDEMP-1", "ix-1", "ACCEPT", "PAY-1",
                "Toyota", null, 2023, 35, 10
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("all risk snapshot fields");
    }
}
