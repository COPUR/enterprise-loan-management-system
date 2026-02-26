package com.enterprise.openfinance.fxservices.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record FxErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {

    public static FxErrorResponse of(String code, String message, String interactionId) {
        return new FxErrorResponse(code, message, interactionId, Instant.now());
    }
}
