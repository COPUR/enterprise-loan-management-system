package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.persistence;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretRecord;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JpaInternalSystemSecretAdapterTest {

    @Test
    void findBySecretKeyShouldMapEntityToDomain() {
        InternalSystemSecretJpaRepository repository = Mockito.mock(InternalSystemSecretJpaRepository.class);
        JpaInternalSystemSecretAdapter adapter = new JpaInternalSystemSecretAdapter(repository);
        InternalSystemSecretRecord record = sampleRecord();
        when(repository.findBySecretKey("INTERNAL.JWT_HMAC_SECRET"))
                .thenReturn(Optional.of(InternalSystemSecretJpaEntity.fromDomain(record)));

        Optional<InternalSystemSecretRecord> result = adapter.findBySecretKey("INTERNAL.JWT_HMAC_SECRET");

        assertThat(result).contains(record);
    }

    @Test
    void saveShouldCreateWhenMissingAndUpdateWhenExisting() {
        InternalSystemSecretJpaRepository repository = Mockito.mock(InternalSystemSecretJpaRepository.class);
        JpaInternalSystemSecretAdapter adapter = new JpaInternalSystemSecretAdapter(repository);
        InternalSystemSecretRecord record = sampleRecord();

        when(repository.findBySecretKey(record.secretKey())).thenReturn(Optional.empty());
        when(repository.save(any(InternalSystemSecretJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, InternalSystemSecretJpaEntity.class));

        InternalSystemSecretRecord created = adapter.save(record);
        assertThat(created).isEqualTo(record);

        InternalSystemSecretRecord updatedRecord = new InternalSystemSecretRecord(
                record.secretKey(),
                "ab****zz",
                "hash-updated",
                "salt-updated",
                "INTERNAL",
                2,
                record.createdAt(),
                Instant.parse("2026-02-26T00:00:00Z")
        );
        when(repository.findBySecretKey(record.secretKey()))
                .thenReturn(Optional.of(InternalSystemSecretJpaEntity.fromDomain(record)));

        InternalSystemSecretRecord updated = adapter.save(updatedRecord);
        assertThat(updated).isEqualTo(updatedRecord);
    }

    private static InternalSystemSecretRecord sampleRecord() {
        return new InternalSystemSecretRecord(
                "INTERNAL.JWT_HMAC_SECRET",
                "ab****yz",
                "hash",
                "salt",
                "INTERNAL",
                1,
                Instant.parse("2026-02-25T00:00:00Z"),
                Instant.parse("2026-02-25T00:00:00Z")
        );
    }
}
