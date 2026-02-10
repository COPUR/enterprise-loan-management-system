package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("metadata_transactions")
public record TransactionMetadataDocument(
        @Id String id,
        String accountId,
        Instant bookingDateTime,
        BigDecimal amount,
        BigDecimal chargeAmount,
        String currency,
        String merchantName,
        String categoryCode,
        GeoLocationDocument geoLocation,
        FxDetailsDocument fxDetails
) {

    public record GeoLocationDocument(
            double latitude,
            double longitude
    ) {
    }

    public record FxDetailsDocument(
            BigDecimal exchangeRate,
            String originalCurrency,
            String targetCurrency
    ) {
    }
}

