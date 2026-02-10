package com.enterprise.openfinance.uc14.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ProductErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {

    public static ProductErrorResponse of(String code, String message, String interactionId) {
        return new ProductErrorResponse(code, message, interactionId, Instant.now());
    }
}
