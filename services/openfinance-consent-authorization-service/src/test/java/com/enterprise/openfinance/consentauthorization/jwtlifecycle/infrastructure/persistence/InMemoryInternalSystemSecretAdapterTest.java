package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.persistence;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryInternalSystemSecretAdapterTest {

    @Test
    void shouldSaveAndFindBySecretKey() {
        InMemoryInternalSystemSecretAdapter adapter = new InMemoryInternalSystemSecretAdapter();
        InternalSystemSecretRecord record = new InternalSystemSecretRecord(
                "INTERNAL.JWT_HMAC_SECRET",
                "ab****yz",
                "hash",
                "salt",
                "INTERNAL",
                1,
                Instant.parse("2026-02-25T10:00:00Z"),
                Instant.parse("2026-02-25T10:00:00Z")
        );

        InternalSystemSecretRecord saved = adapter.save(record);

        assertThat(saved).isEqualTo(record);
        assertThat(adapter.findBySecretKey("INTERNAL.JWT_HMAC_SECRET")).contains(record);
    }
}
