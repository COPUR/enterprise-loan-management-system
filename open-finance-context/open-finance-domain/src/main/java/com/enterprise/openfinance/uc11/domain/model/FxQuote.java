package com.enterprise.openfinance.uc11.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record FxQuote(
        String quoteId,
        String tppId,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal sourceAmount,
        BigDecimal targetAmount,
        BigDecimal exchangeRate,
        FxQuoteStatus status,
        Instant validUntil,
        Instant createdAt,
        Instant updatedAt
) {

    public FxQuote {
        if (isBlank(quoteId)) {
            throw new IllegalArgumentException("quoteId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
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
        if (validUntil == null) {
            throw new IllegalArgumentException("validUntil is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("timestamps are required");
        }

        quoteId = quoteId.trim();
        tppId = tppId.trim();
        sourceCurrency = sourceCurrency.trim().toUpperCase();
        targetCurrency = targetCurrency.trim().toUpperCase();
    }

    public FxQuote book(Instant now) {
        return new FxQuote(
                quoteId,
                tppId,
                sourceCurrency,
                targetCurrency,
                sourceAmount,
                targetAmount,
                exchangeRate,
                FxQuoteStatus.BOOKED,
                validUntil,
                createdAt,
                now
        );
    }

    public FxQuote expire(Instant now) {
        if (status == FxQuoteStatus.EXPIRED) {
            return this;
        }
        return new FxQuote(
                quoteId,
                tppId,
                sourceCurrency,
                targetCurrency,
                sourceAmount,
                targetAmount,
                exchangeRate,
                FxQuoteStatus.EXPIRED,
                validUntil,
                createdAt,
                now
        );
    }

    public boolean isExpired(Instant now) {
        return !validUntil.isAfter(now);
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
