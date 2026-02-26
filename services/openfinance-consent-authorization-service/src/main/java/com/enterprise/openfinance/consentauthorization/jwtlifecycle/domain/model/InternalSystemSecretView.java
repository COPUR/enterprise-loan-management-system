package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model;

import java.time.Instant;

public record InternalSystemSecretView(
        String secretKey,
        String maskedValue,
        String classification,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
