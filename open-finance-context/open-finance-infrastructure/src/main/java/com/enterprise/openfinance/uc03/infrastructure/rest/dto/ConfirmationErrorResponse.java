package com.enterprise.openfinance.uc03.infrastructure.rest.dto;

import java.time.Instant;

public record ConfirmationErrorResponse(
        String code,
        String message,
        String interactionId,
        Instant timestamp
) {
    public static ConfirmationErrorResponse of(String code, String message, String interactionId) {
        return new ConfirmationErrorResponse(code, message, interactionId, Instant.now());
    }
}
