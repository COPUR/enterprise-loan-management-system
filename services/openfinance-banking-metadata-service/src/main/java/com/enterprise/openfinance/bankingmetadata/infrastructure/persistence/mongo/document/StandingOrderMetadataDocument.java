package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("metadata_standing_orders")
public record StandingOrderMetadataDocument(
        @Id String id,
        String accountId,
        String frequency,
        Instant firstPaymentDate,
        Instant finalPaymentDate,
        BigDecimal amount,
        String currency
) {
}

