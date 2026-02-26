package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command;

import jakarta.validation.constraints.NotBlank;

public record UpsertInternalSystemSecretCommand(
        @NotBlank String secretKey,
        @NotBlank String secretValue,
        String classification
) {
    public UpsertInternalSystemSecretCommand {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("Secret key is required");
        }
        if (secretValue == null || secretValue.isBlank()) {
            throw new IllegalArgumentException("Secret value is required");
        }
    }
}
