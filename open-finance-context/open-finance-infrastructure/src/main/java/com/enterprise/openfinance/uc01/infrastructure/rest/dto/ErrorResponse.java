package com.enterprise.openfinance.uc01.infrastructure.rest.dto;

import java.time.Instant;

public record ErrorResponse(
        String code,
        String message,
        String interactionId,
        Instant timestamp
) {
    public static ErrorResponse of(String code, String message, String interactionId) {
        return new ErrorResponse(code, message, interactionId, Instant.now());
    }
}
