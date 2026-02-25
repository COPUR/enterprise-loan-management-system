package com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto;

public record OAuthErrorResponse(
        String error,
        String errorDescription
) {

    public static OAuthErrorResponse of(String error, String description) {
        return new OAuthErrorResponse(error, description);
    }
}

