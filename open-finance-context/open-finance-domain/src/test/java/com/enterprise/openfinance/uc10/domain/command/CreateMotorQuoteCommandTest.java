package com.enterprise.openfinance.uc10.domain.command;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class CreateMotorQuoteCommandTest {

    @Test
    void shouldCreateAndNormalizeCommand() {
        CreateMotorQuoteCommand command = new CreateMotorQuoteCommand(
                "  TPP-001 ",
                " ix-1 ",
                " toyota ",
                " camry ",
                2023,
                35,
                10
        );

        assertThat(command.tppId()).isEqualTo("TPP-001");
        assertThat(command.interactionId()).isEqualTo("ix-1");
        assertThat(command.vehicleMake()).isEqualTo("TOYOTA");
        assertThat(command.vehicleModel()).isEqualTo("CAMRY");
        assertThat(command.riskFingerprint()).contains("TOYOTA|CAMRY|2023|35|10");
    }

    @Test
    void shouldRejectInvalidCommand() {
        assertThatThrownBy(() -> new CreateMotorQuoteCommand(
                " ", "ix-1", "Toyota", "Camry", 2023, 35, 10
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CreateMotorQuoteCommand(
                "TPP-001", "ix-1", "Toyota", "Camry", 1800, 35, 10
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CreateMotorQuoteCommand(
                "TPP-001", "ix-1", "Toyota", "Camry", 2023, 15, 10
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
