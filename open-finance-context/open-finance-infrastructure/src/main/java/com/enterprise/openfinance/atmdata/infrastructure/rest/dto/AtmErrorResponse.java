package com.enterprise.openfinance.atmdata.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record AtmErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {

    public static AtmErrorResponse of(String code, String message, String interactionId) {
        return new AtmErrorResponse(code, message, interactionId, Instant.now());
    }
}
