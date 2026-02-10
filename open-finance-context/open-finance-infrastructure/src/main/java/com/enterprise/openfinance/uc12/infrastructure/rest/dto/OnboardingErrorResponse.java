package com.enterprise.openfinance.uc12.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record OnboardingErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {

    public static OnboardingErrorResponse of(String code, String message, String interactionId) {
        return new OnboardingErrorResponse(code, message, interactionId, Instant.now());
    }
}
