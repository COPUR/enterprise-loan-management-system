package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Document("corporate_consents")
public record CorporateConsentDocument(
        @Id String id,
        String tppId,
        String corporateId,
        String entitlement,
        Set<String> scopes,
        Set<String> accountIds,
        Instant expiresAt
) {
}
