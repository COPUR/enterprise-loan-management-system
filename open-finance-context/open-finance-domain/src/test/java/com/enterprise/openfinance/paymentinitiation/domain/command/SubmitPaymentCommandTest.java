package com.enterprise.openfinance.paymentinitiation.domain.command;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentInitiation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubmitPaymentCommandTest {

    @Test
    void shouldCreateCommandWhenValid() {
        SubmitPaymentCommand command = new SubmitPaymentCommand(
                "TPP-001",
                "IDEMP-001",
                "CONS-001",
                initiation(),
                "ix-001",
                "{\"Data\":{}}",
                "detached-jws"
        );

        assertThat(command.tppId()).isEqualTo("TPP-001");
        assertThat(command.idempotencyKey()).isEqualTo("IDEMP-001");
        assertThat(command.consentId()).isEqualTo("CONS-001");
        assertThat(command.interactionId()).isEqualTo("ix-001");
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        assertThatThrownBy(() -> new SubmitPaymentCommand(
                " ",
                "IDEMP-001",
                "CONS-001",
                initiation(),
                "ix-001",
                "{\"Data\":{}}",
                "detached-jws"
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("tppId is required");

        assertThatThrownBy(() -> new SubmitPaymentCommand(
                "TPP-001",
                " ",
                "CONS-001",
                initiation(),
                "ix-001",
                "{\"Data\":{}}",
                "detached-jws"
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("idempotencyKey is required");

        assertThatThrownBy(() -> new SubmitPaymentCommand(
                "TPP-001",
                "IDEMP-001",
                " ",
                initiation(),
                "ix-001",
                "{\"Data\":{}}",
                "detached-jws"
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("consentId is required");

        assertThatThrownBy(() -> new SubmitPaymentCommand(
                "TPP-001",
                "IDEMP-001",
                "CONS-001",
                null,
                "ix-001",
                "{\"Data\":{}}",
                "detached-jws"
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("initiation is required");

        assertThatThrownBy(() -> new SubmitPaymentCommand(
                "TPP-001",
                "IDEMP-001",
                "CONS-001",
                initiation(),
                " ",
                "{\"Data\":{}}",
                "detached-jws"
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("interactionId is required");

        assertThatThrownBy(() -> new SubmitPaymentCommand(
                "TPP-001",
                "IDEMP-001",
                "CONS-001",
                initiation(),
                "ix-001",
                " ",
                "detached-jws"
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("rawPayload is required");

        assertThatThrownBy(() -> new SubmitPaymentCommand(
                "TPP-001",
                "IDEMP-001",
                "CONS-001",
                initiation(),
                "ix-001",
                "{\"Data\":{}}",
                " "
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("jwsSignature is required");
    }

    private static PaymentInitiation initiation() {
        return new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        );
    }
}
