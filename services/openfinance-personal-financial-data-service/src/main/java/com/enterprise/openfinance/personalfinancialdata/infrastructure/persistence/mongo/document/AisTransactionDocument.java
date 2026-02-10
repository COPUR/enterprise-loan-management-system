package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("ais_transactions")
public record AisTransactionDocument(
        @Id String id,
        String accountId,
        BigDecimal amount,
        String currency,
        Instant bookingDateTime,
        Instant valueDateTime,
        String creditDebitIndicator,
        String status,
        String merchantName
) {
}

