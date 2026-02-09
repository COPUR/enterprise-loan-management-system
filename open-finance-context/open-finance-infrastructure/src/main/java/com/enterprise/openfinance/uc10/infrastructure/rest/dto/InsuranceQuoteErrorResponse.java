package com.enterprise.openfinance.uc10.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record InsuranceQuoteErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {

    public static InsuranceQuoteErrorResponse of(String code, String message, String interactionId) {
        return new InsuranceQuoteErrorResponse(code, message, interactionId, Instant.now());
    }
}
