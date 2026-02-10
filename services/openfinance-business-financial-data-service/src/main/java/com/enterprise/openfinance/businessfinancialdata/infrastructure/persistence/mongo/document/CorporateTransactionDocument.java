package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("corporate_transactions")
public record CorporateTransactionDocument(
        @Id String id,
        String accountId,
        BigDecimal amount,
        String currency,
        Instant bookingDateTime,
        String transactionCode,
        String proprietaryCode,
        String description
) {
}
