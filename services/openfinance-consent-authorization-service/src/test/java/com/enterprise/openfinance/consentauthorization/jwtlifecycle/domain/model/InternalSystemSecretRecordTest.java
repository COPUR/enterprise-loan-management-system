package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InternalSystemSecretRecordTest {

    @Test
    void shouldExposeMetadataView() {
        InternalSystemSecretRecord record = new InternalSystemSecretRecord(
                "KEY",
                "ab****yz",
                "hash",
                "salt",
                "INTERNAL",
                2,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z")
        );

        InternalSystemSecretView view = record.toView();

        assertThat(view.secretKey()).isEqualTo("KEY");
        assertThat(view.maskedValue()).isEqualTo("ab****yz");
        assertThat(view.version()).isEqualTo(2);
    }

    @Test
    void shouldRejectInvalidFields() {
        assertThatThrownBy(() -> new InternalSystemSecretRecord(
                " ",
                "ab****yz",
                "hash",
                "salt",
                "INTERNAL",
                1,
                Instant.now(),
                Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new InternalSystemSecretRecord(
                "KEY",
                "ab****yz",
                "hash",
                "salt",
                "INTERNAL",
                0,
                Instant.now(),
                Instant.now()
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
