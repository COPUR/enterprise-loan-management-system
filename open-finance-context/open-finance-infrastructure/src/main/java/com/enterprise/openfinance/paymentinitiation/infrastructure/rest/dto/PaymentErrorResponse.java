package com.enterprise.openfinance.paymentinitiation.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record PaymentErrorResponse(
        String code,
        String message,
        String interactionId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp
) {
    public static PaymentErrorResponse of(String code, String message, String interactionId) {
        return new PaymentErrorResponse(code, message, interactionId, Instant.now());
    }
}
