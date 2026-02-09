package com.enterprise.openfinance.uc06.infrastructure.event;

import com.enterprise.openfinance.uc06.domain.model.PaymentStatus;
import com.enterprise.openfinance.uc06.domain.model.PaymentTransaction;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

@Tag("unit")
class NoOpPaymentEventAdapterTest {

    @Test
    void shouldPublishWithoutThrowing() {
        NoOpPaymentEventAdapter adapter = new NoOpPaymentEventAdapter();
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

        assertThatCode(() -> adapter.publishSubmitted(transaction)).doesNotThrowAnyException();
    }
}
