package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.persistence;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "internal_system_secrets")
public class InternalSystemSecretJpaEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "secret_key", nullable = false, unique = true, length = 120)
    private String secretKey;

    @Column(name = "masked_value", nullable = false, length = 256)
    private String maskedValue;

    @Column(name = "secret_hash", nullable = false, length = 256)
    private String secretHash;

    @Column(name = "hash_salt", nullable = false, length = 128)
    private String hashSalt;

    @Column(name = "classification", nullable = false, length = 80)
    private String classification;

    @Column(name = "version_number", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static InternalSystemSecretJpaEntity fromDomain(InternalSystemSecretRecord record) {
        InternalSystemSecretJpaEntity entity = new InternalSystemSecretJpaEntity();
        entity.secretKey = record.secretKey();
        entity.maskedValue = record.maskedValue();
        entity.secretHash = record.secretHash();
        entity.hashSalt = record.hashSalt();
        entity.classification = record.classification();
        entity.version = record.version();
        entity.createdAt = record.createdAt();
        entity.updatedAt = record.updatedAt();
        return entity;
    }

    public void mergeFrom(InternalSystemSecretRecord record) {
        this.secretKey = record.secretKey();
        this.maskedValue = record.maskedValue();
        this.secretHash = record.secretHash();
        this.hashSalt = record.hashSalt();
        this.classification = record.classification();
        this.version = record.version();
        this.createdAt = record.createdAt();
        this.updatedAt = record.updatedAt();
    }

    public InternalSystemSecretRecord toDomain() {
        return new InternalSystemSecretRecord(
                secretKey,
                maskedValue,
                secretHash,
                hashSalt,
                classification,
                version,
                createdAt,
                updatedAt
        );
    }
}
