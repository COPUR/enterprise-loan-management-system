package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.persistence;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretRecord;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InternalSystemSecretJpaEntityTest {

    @Test
    void shouldConvertFromAndToDomain() {
        InternalSystemSecretRecord source = new InternalSystemSecretRecord(
                "PAYMENT.API.KEY",
                "pa****ey",
                "hash",
                "salt",
                "PAYMENT",
                3,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z")
        );

        InternalSystemSecretJpaEntity entity = InternalSystemSecretJpaEntity.fromDomain(source);
        InternalSystemSecretRecord converted = entity.toDomain();

        assertThat(converted).isEqualTo(source);
    }

    @Test
    void mergeFromShouldRefreshEntityValues() {
        InternalSystemSecretJpaEntity entity = InternalSystemSecretJpaEntity.fromDomain(new InternalSystemSecretRecord(
                "OLD.KEY",
                "ol****ey",
                "oldHash",
                "oldSalt",
                "INTERNAL",
                1,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        ));
        InternalSystemSecretRecord updated = new InternalSystemSecretRecord(
                "NEW.KEY",
                "ne****ey",
                "newHash",
                "newSalt",
                "PAYMENT",
                2,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z")
        );

        entity.mergeFrom(updated);

        assertThat(entity.toDomain()).isEqualTo(updated);
    }
}
