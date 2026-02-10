package com.enterprise.openfinance.consentauthorization.domain.command;

import java.time.Instant;
import java.util.Set;

public record CreateConsentCommand(
        String customerId,
        String participantId,
        Set<String> scopes,
        String purpose,
        Instant expiresAt
) {
    public CreateConsentCommand {
        if (isBlank(customerId)) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (isBlank(participantId)) {
            throw new IllegalArgumentException("participantId is required");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("at least one scope is required");
        }
        if (isBlank(purpose)) {
            throw new IllegalArgumentException("purpose is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
