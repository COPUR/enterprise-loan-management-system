package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.rest.dto;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalTokenIssueResult;

import java.time.Instant;

public record InternalTokenResponse(
        String token,
        String tokenType,
        long expiresIn,
        Instant issuedAt,
        Instant expiresAt
) {
    public static InternalTokenResponse from(InternalTokenIssueResult token) {
        return new InternalTokenResponse(
                token.accessToken(),
                token.tokenType(),
                token.expiresInSeconds(),
                token.issuedAt(),
                token.expiresAt()
        );
    }
}

