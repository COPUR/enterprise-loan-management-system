package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document("corporate_balances")
public record CorporateBalanceDocument(
        @Id String id,
        String masterAccountId,
        String accountId,
        String balanceType,
        BigDecimal amount,
        String currency,
        Instant asOf
) {
}
