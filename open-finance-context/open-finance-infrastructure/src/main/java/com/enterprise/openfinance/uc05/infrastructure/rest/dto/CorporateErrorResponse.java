package com.enterprise.openfinance.uc05.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record CorporateErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {

    public static CorporateErrorResponse of(String code, String message, String interactionId) {
        return new CorporateErrorResponse(code, message, interactionId, Instant.now());
    }
}
