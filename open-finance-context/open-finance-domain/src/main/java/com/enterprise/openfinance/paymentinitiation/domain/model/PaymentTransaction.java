package com.enterprise.openfinance.paymentinitiation.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public record PaymentTransaction(
        String paymentId,
        String consentId,
        String tppId,
        String idempotencyKey,
        PaymentStatus status,
        String debtorAccountId,
        String creditorAccountIdentification,
        BigDecimal amount,
        String currency,
        LocalDate requestedExecutionDate,
        Instant createdAt,
        Instant updatedAt
) {
    public PaymentTransaction {
        if (isBlank(paymentId)) {
            throw new IllegalArgumentException("paymentId is required");
        }
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (isBlank(debtorAccountId)) {
            throw new IllegalArgumentException("debtorAccountId is required");
        }
        if (isBlank(creditorAccountIdentification)) {
            throw new IllegalArgumentException("creditorAccountIdentification is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("timestamps are required");
        }

        paymentId = paymentId.trim();
        consentId = consentId.trim();
        tppId = tppId.trim();
        idempotencyKey = idempotencyKey.trim();
        debtorAccountId = debtorAccountId.trim();
        creditorAccountIdentification = creditorAccountIdentification.trim().replace(" ", "");
        amount = amount.stripTrailingZeros();
        currency = currency.trim().toUpperCase(Locale.ROOT);
    }

    public static PaymentTransaction create(
            String consentId,
            String tppId,
            String idempotencyKey,
            PaymentStatus status,
            PaymentInitiation initiation,
            Instant now
    ) {
        return new PaymentTransaction(
                "PAY-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT),
                consentId,
                tppId,
                idempotencyKey,
                status,
                initiation.debtorAccountId(),
                initiation.creditorAccountIdentification(),
                initiation.instructedAmount(),
                initiation.currency(),
                initiation.requestedExecutionDate(),
                now,
                now
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
