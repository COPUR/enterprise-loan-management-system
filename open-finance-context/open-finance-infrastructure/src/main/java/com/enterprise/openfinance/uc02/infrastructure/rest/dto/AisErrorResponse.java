package com.enterprise.openfinance.uc02.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record AisErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {

    public static AisErrorResponse of(String code, String message, String interactionId) {
        return new AisErrorResponse(code, message, interactionId, Instant.now());
    }
}
