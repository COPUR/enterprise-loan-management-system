package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("metadata_accounts")
public record AccountMetadataDocument(
        @Id String id,
        String schemeName,
        String secondaryIdentification
) {
}

