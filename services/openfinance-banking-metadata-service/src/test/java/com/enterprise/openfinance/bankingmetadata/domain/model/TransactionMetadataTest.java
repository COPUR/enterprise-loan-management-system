package com.enterprise.openfinance.bankingmetadata.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionMetadataTest {

    @Test
    void shouldCreateTransactionMetadataWithOptionalFxAndFees() {
        TransactionMetadata metadata = new TransactionMetadata(
                "TXN-001",
                "ACC-001",
                Instant.parse("2026-01-01T00:00:00Z"),
                new BigDecimal("100.00"),
                new BigDecimal("2.00"),
                "AED",
                "Merchant",
                "5411",
                new GeoLocation(25.2048, 55.2708),
                new FxDetails(new BigDecimal("0.27290"), "AED", "USD")
        );

        assertThat(metadata.transactionId()).isEqualTo("TXN-001");
        assertThat(metadata.fxDetails()).isNotNull();
        assertThat(metadata.chargeAmount()).isEqualByComparingTo("2.00");
    }

    @Test
    void shouldRejectInvalidTransactionMetadata() {
        assertThatThrownBy(() -> new TransactionMetadata(
                "",
                "ACC-001",
                Instant.parse("2026-01-01T00:00:00Z"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                "AED",
                "Merchant",
                "5411",
                new GeoLocation(25.2048, 55.2708),
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");
    }
}
