package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("ais_balances")
public record AisBalanceDocument(
        @Id String id,
        String accountId,
        String balanceType,
        BigDecimal amount,
        String currency,
        Instant asOf
) {
}

