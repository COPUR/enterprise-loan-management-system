package com.enterprise.openfinance.uc04.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record MetadataErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {

    public static MetadataErrorResponse of(String code, String message, String interactionId) {
        return new MetadataErrorResponse(code, message, interactionId, Instant.now());
    }
}
