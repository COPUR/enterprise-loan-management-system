package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("metadata_parties")
public record PartyMetadataDocument(
        @Id String id,
        String accountId,
        String fullLegalName,
        String kycStatus,
        Instant relationshipStartDate
) {
}

