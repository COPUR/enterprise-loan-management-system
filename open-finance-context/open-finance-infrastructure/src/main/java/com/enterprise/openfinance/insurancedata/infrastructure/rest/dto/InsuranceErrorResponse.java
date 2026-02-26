package com.enterprise.openfinance.insurancedata.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record InsuranceErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {

    public static InsuranceErrorResponse of(String code, String message, String interactionId) {
        return new InsuranceErrorResponse(code, message, interactionId, Instant.now());
    }
}
