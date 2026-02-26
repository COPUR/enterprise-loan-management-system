package com.enterprise.openfinance.payeeverification.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class ConfirmationRequestTest {

    @Test
    void shouldCreateRequestWhenAllFieldsAreValid() {
        assertThatCode(() -> new ConfirmationRequest(
                "GB82WEST12345698765432",
                "IBAN",
                "Al Tareq Trading LLC",
                "TPP-ABC",
                "interaction-001"
        )).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectBlankIdentification() {
        assertThatThrownBy(() -> new ConfirmationRequest(
                " ",
                "IBAN",
                "Al Tareq Trading LLC",
                "TPP-ABC",
                "interaction-001"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("identification is required");
    }

    @Test
    void shouldRejectBlankSchemeName() {
        assertThatThrownBy(() -> new ConfirmationRequest(
                "GB82WEST12345698765432",
                "",
                "Al Tareq Trading LLC",
                "TPP-ABC",
                "interaction-001"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("schemeName is required");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> new ConfirmationRequest(
                "GB82WEST12345698765432",
                "IBAN",
                "",
                "TPP-ABC",
                "interaction-001"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name is required");
    }

    @Test
    void shouldRejectBlankTppId() {
        assertThatThrownBy(() -> new ConfirmationRequest(
                "GB82WEST12345698765432",
                "IBAN",
                "Al Tareq Trading LLC",
                " ",
                "interaction-001"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tppId is required");
    }

    @Test
    void shouldRejectBlankInteractionId() {
        assertThatThrownBy(() -> new ConfirmationRequest(
                "GB82WEST12345698765432",
                "IBAN",
                "Al Tareq Trading LLC",
                "TPP-ABC",
                " "
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("interactionId is required");
    }
}
