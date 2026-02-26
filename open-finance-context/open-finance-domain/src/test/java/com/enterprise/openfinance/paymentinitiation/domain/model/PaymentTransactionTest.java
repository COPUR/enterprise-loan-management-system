package com.enterprise.openfinance.paymentinitiation.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTransactionTest {

    @Test
    void shouldCreateTransactionFromFactory() {
        PaymentInitiation initiation = new PaymentInitiation(
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

        PaymentTransaction transaction = PaymentTransaction.create(
                "CONS-001",
                "TPP-001",
                "IDEMP-001",
                PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS,
                initiation,
                Instant.parse("2026-02-09T10:00:00Z")
        );

        assertThat(transaction.paymentId()).startsWith("PAY-");
        assertThat(transaction.status()).isEqualTo(PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS);
        assertThat(transaction.amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldRejectInvalidTransactionState() {
        Instant now = Instant.parse("2026-02-09T10:00:00Z");
        assertThatThrownBy(() -> new PaymentTransaction(
                " ",
                "CONS-001",
                "TPP-001",
                "IDEMP-001",
                PaymentStatus.PENDING,
                "ACC-DEBTOR-001",
                "AE120001000000123456789",
                new BigDecimal("100.00"),
                "AED",
                null,
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("paymentId is required");

        assertThatThrownBy(() -> new PaymentTransaction(
                "PAY-001",
                "CONS-001",
                "TPP-001",
                "IDEMP-001",
                PaymentStatus.PENDING,
                "ACC-DEBTOR-001",
                "AE120001000000123456789",
                BigDecimal.ZERO,
                "AED",
                null,
                now,
                now
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("amount must be greater than zero");
    }
}
