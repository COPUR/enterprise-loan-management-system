package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model;

import java.time.Instant;

public record InternalSystemSecretRecord(
        String secretKey,
        String maskedValue,
        String secretHash,
        String hashSalt,
        String classification,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public InternalSystemSecretRecord {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("Secret key is required");
        }
        if (maskedValue == null || maskedValue.isBlank()) {
            throw new IllegalArgumentException("Masked value is required");
        }
        if (secretHash == null || secretHash.isBlank()) {
            throw new IllegalArgumentException("Secret hash is required");
        }
        if (hashSalt == null || hashSalt.isBlank()) {
            throw new IllegalArgumentException("Hash salt is required");
        }
        if (classification == null || classification.isBlank()) {
            throw new IllegalArgumentException("Classification is required");
        }
        if (version < 1) {
            throw new IllegalArgumentException("Version must be >= 1");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Timestamps are required");
        }
    }

    public InternalSystemSecretView toView() {
        return new InternalSystemSecretView(
                secretKey,
                maskedValue,
                classification,
                version,
                createdAt,
                updatedAt
        );
    }
}
