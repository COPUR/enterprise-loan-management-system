package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("ais_accounts")
public record AisAccountDocument(
        @Id String id,
        String psuId,
        String iban,
        String currency,
        String accountType,
        String status,
        String schemeName,
        String name
) {
}

