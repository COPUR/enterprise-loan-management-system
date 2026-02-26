package com.enterprise.openfinance.paymentinitiation.domain.command;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentInitiation;

public record SubmitPaymentCommand(
        String tppId,
        String idempotencyKey,
        String consentId,
        PaymentInitiation initiation,
        String interactionId,
        String rawPayload,
        String jwsSignature
) {
    public SubmitPaymentCommand {
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (initiation == null) {
            throw new IllegalArgumentException("initiation is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }
        if (isBlank(rawPayload)) {
            throw new IllegalArgumentException("rawPayload is required");
        }
        if (isBlank(jwsSignature)) {
            throw new IllegalArgumentException("jwsSignature is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
