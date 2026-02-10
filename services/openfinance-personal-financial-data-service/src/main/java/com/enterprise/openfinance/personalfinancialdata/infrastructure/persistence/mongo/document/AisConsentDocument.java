package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Document("ais_consents")
public record AisConsentDocument(
        @Id String id,
        String tppId,
        String psuId,
        Set<String> scopes,
        Set<String> accountIds,
        Instant expiresAt
) {
}

