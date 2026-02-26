package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto;

import java.time.Instant;

public record InternalBusinessResponse(
        String status,
        String subject,
        Instant issuedAt,
        Instant expiresAt
) {
}

