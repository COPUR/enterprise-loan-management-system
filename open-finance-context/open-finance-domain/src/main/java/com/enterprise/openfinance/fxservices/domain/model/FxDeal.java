package com.enterprise.openfinance.fxservices.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record FxDeal(
        String dealId,
        String quoteId,
        String tppId,
        String idempotencyKey,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal sourceAmount,
        BigDecimal targetAmount,
        BigDecimal exchangeRate,
        FxDealStatus status,
        Instant bookedAt
) {

    public FxDeal {
        if (isBlank(dealId)) {
            throw new IllegalArgumentException("dealId is required");
        }
        if (isBlank(quoteId)) {
            throw new IllegalArgumentException("quoteId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (!isCurrency(sourceCurrency)) {
            throw new IllegalArgumentException("sourceCurrency is invalid");
        }
        if (!isCurrency(targetCurrency)) {
            throw new IllegalArgumentException("targetCurrency is invalid");
        }
        if (sourceAmount == null || sourceAmount.signum() <= 0) {
            throw new IllegalArgumentException("sourceAmount must be positive");
        }
        if (targetAmount == null || targetAmount.signum() <= 0) {
            throw new IllegalArgumentException("targetAmount must be positive");
        }
        if (exchangeRate == null || exchangeRate.signum() <= 0) {
            throw new IllegalArgumentException("exchangeRate must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (bookedAt == null) {
            throw new IllegalArgumentException("bookedAt is required");
        }

        dealId = dealId.trim();
        quoteId = quoteId.trim();
        tppId = tppId.trim();
        idempotencyKey = idempotencyKey.trim();
        sourceCurrency = sourceCurrency.trim().toUpperCase();
        targetCurrency = targetCurrency.trim().toUpperCase();
    }

    public boolean belongsTo(String candidateTppId) {
        return tppId.equals(candidateTppId);
    }

    private static boolean isCurrency(String value) {
        return value != null && value.trim().matches("[A-Za-z]{3}");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
