package com.enterprise.openfinance.uc06.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentInitiationTest {

    @Test
    void shouldCreatePaymentInitiationWhenValid() {
        PaymentInitiation initiation = new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                LocalDate.parse("2026-02-15")
        );

        assertThat(initiation.instructionIdentification()).isEqualTo("INSTR-001");
        assertThat(initiation.endToEndIdentification()).isEqualTo("E2E-001");
        assertThat(initiation.instructedAmount()).isEqualByComparingTo("100.00");
        assertThat(initiation.currency()).isEqualTo("AED");
        assertThat(initiation.creditorAccountIdentification()).isEqualTo("AE120001000000123456789");
        assertThat(initiation.creditorName()).isEqualTo("Vendor LLC");
    }

    @Test
    void shouldRejectInvalidInitiationData() {
        assertThatThrownBy(() -> new PaymentInitiation(
                " ",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("instructionIdentification is required");

        assertThatThrownBy(() -> new PaymentInitiation(
                "INSTR-001",
                " ",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("endToEndIdentification is required");

        assertThatThrownBy(() -> new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                " ",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("debtorAccountId is required");

        assertThatThrownBy(() -> new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("0.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("instructedAmount must be greater than zero");

        assertThatThrownBy(() -> new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                " ",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("currency is required");

        assertThatThrownBy(() -> new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                " ",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("creditorAccountScheme is required");

        assertThatThrownBy(() -> new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                " ",
                "Vendor LLC",
                null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("creditorAccountIdentification is required");

        assertThatThrownBy(() -> new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                " ",
                null
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("creditorName is required");
    }
}
