package com.enterprise.openfinance.bankingmetadata.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionMetadata(
        String transactionId,
        String accountId,
        Instant bookingDateTime,
        BigDecimal amount,
        BigDecimal chargeAmount,
        String currency,
        String merchantName,
        String categoryCode,
        GeoLocation geoLocation,
        FxDetails fxDetails
) {

    public TransactionMetadata {
        if (isBlank(transactionId)) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (bookingDateTime == null) {
            throw new IllegalArgumentException("bookingDateTime is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (chargeAmount == null || chargeAmount.signum() < 0) {
            throw new IllegalArgumentException("chargeAmount must be >= 0");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (isBlank(merchantName)) {
            throw new IllegalArgumentException("merchantName is required");
        }
        if (isBlank(categoryCode)) {
            throw new IllegalArgumentException("categoryCode is required");
        }
        if (geoLocation == null) {
            throw new IllegalArgumentException("geoLocation is required");
        }

        transactionId = transactionId.trim();
        accountId = accountId.trim();
        currency = currency.trim();
        merchantName = merchantName.trim();
        categoryCode = categoryCode.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
