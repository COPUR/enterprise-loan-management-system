package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretView;

import java.time.Instant;

public record InternalSystemSecretResponse(
        String secretKey,
        String maskedValue,
        String classification,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static InternalSystemSecretResponse from(InternalSystemSecretView view) {
        return new InternalSystemSecretResponse(
                view.secretKey(),
                view.maskedValue(),
                view.classification(),
                view.version(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
