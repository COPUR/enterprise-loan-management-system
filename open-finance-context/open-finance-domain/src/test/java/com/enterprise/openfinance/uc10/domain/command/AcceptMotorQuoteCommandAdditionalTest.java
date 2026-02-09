package com.enterprise.openfinance.uc10.domain.command;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class AcceptMotorQuoteCommandAdditionalTest {

    @Test
    void shouldSupportNoRiskSnapshotAndIdempotencyFingerprint() {
        AcceptMotorQuoteCommand command = new AcceptMotorQuoteCommand(
                "TPP-001",
                "Q-1",
                "IDEMP-1",
                "ix-1",
                "ACCEPT",
                "PAY-1",
                null,
                null,
                null,
                null,
                null
        );

        assertThat(command.hasRiskSnapshot()).isFalse();
        assertThat(command.idempotencyFingerprint()).contains("NONE");
    }

    @Test
    void shouldRejectInvalidRiskSnapshotRange() {
        assertThatThrownBy(() -> new AcceptMotorQuoteCommand(
                "TPP-001", "Q-1", "IDEMP-1", "ix-1", "ACCEPT", "PAY-1",
                "Toyota", "Camry", 2023, 10, 1
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new AcceptMotorQuoteCommand(
                "TPP-001", "Q-1", "IDEMP-1", "ix-1", "ACCEPT", "PAY-1",
                "Toyota", "Camry", 1800, 35, 10
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
