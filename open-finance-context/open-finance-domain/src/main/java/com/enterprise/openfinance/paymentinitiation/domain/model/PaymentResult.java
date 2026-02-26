package com.enterprise.openfinance.paymentinitiation.domain.model;

import java.time.Instant;

public record PaymentResult(
        String paymentId,
        String consentId,
        PaymentStatus status,
        String interactionId,
        Instant createdAt,
        boolean idempotencyReplay
) {
    public PaymentResult {
        if (isBlank(paymentId)) {
            throw new IllegalArgumentException("paymentId is required");
        }
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt is required");
        }
    }

    public PaymentResult asReplay() {
        return new PaymentResult(paymentId, consentId, status, interactionId, createdAt, true);
    }

    public static PaymentResult from(PaymentTransaction transaction, String interactionId) {
        return new PaymentResult(
                transaction.paymentId(),
                transaction.consentId(),
                transaction.status(),
                interactionId,
                transaction.createdAt(),
                false
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
