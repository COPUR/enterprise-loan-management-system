package com.enterprise.openfinance.insurancequotes.domain.command;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class CreateMotorQuoteCommandAdditionalTest {

    @Test
    void shouldRejectAdditionalInvalidValues() {
        assertThatThrownBy(() -> new CreateMotorQuoteCommand(
                "TPP-001", "ix-1", "Toyota", "Camry", 2023, 35, -1
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new CreateMotorQuoteCommand(
                "TPP-001", "ix-1", " ", "Camry", 2023, 35, 10
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
