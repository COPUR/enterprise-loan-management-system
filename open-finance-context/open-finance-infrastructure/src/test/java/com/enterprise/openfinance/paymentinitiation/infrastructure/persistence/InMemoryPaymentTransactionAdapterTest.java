package com.enterprise.openfinance.paymentinitiation.infrastructure.persistence;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentStatus;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentTransaction;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryPaymentTransactionAdapterTest {

    private final InMemoryPaymentTransactionAdapter adapter = new InMemoryPaymentTransactionAdapter();

    @Test
    void shouldSaveAndFindPaymentTransaction() {
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

        adapter.save(transaction);

        assertThat(adapter.findByPaymentId("PAY-001")).contains(transaction);
    }
}
