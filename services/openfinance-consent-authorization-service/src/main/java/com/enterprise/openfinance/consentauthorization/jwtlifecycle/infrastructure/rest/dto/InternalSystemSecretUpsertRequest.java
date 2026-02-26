package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record InternalSystemSecretUpsertRequest(
        @NotBlank String secretKey,
        @NotBlank String secretValue,
        String classification
) {
}
