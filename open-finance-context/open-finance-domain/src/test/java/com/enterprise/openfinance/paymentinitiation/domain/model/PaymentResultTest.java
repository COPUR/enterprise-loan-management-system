package com.enterprise.openfinance.paymentinitiation.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentResultTest {

    @Test
    void shouldBuildResultFromTransactionAndMarkReplay() {
        PaymentTransaction transaction = new PaymentTransaction(
                "PAY-001",
                "CONS-001",
                "TPP-001",
                "IDEMP-001",
                PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS,
                "ACC-DEBTOR-001",
                "AE120001000000123456789",
                new BigDecimal("100.00"),
                "AED",
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );

        PaymentResult result = PaymentResult.from(transaction, "ix-001");
        PaymentResult replay = result.asReplay();

        assertThat(result.idempotencyReplay()).isFalse();
        assertThat(replay.idempotencyReplay()).isTrue();
        assertThat(replay.paymentId()).isEqualTo("PAY-001");
    }

    @Test
    void shouldRejectInvalidResult() {
        assertThatThrownBy(() -> new PaymentResult(
                " ",
                "CONS-001",
                PaymentStatus.PENDING,
                "ix-001",
                Instant.parse("2026-02-09T10:00:00Z"),
                false
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("paymentId is required");
    }
}
