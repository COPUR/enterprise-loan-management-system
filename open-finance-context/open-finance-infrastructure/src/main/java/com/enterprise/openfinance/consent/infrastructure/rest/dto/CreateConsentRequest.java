package com.enterprise.openfinance.consent.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

public record CreateConsentRequest(
        @NotBlank String customerId,
        @NotBlank String participantId,
        @NotEmpty Set<String> scopes,
        @NotBlank String purpose,
        @NotNull Instant expiresAt
) {
}
