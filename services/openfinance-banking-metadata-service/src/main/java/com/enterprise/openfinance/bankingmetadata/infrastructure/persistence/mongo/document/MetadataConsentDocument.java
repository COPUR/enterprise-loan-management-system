package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Document("metadata_consents")
public record MetadataConsentDocument(
        @Id String id,
        String tppId,
        Set<String> scopes,
        Set<String> accountIds,
        Instant expiresAt
) {
}

