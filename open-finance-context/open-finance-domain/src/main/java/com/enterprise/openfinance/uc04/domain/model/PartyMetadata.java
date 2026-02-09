package com.enterprise.openfinance.uc04.domain.model;

import java.time.Instant;

public record PartyMetadata(
        String accountId,
        String fullLegalName,
        String kycStatus,
        Instant relationshipStartDate
) {

    public PartyMetadata {
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (isBlank(fullLegalName)) {
            throw new IllegalArgumentException("fullLegalName is required");
        }
        if (isBlank(kycStatus)) {
            throw new IllegalArgumentException("kycStatus is required");
        }
        if (relationshipStartDate == null) {
            throw new IllegalArgumentException("relationshipStartDate is required");
        }

        accountId = accountId.trim();
        fullLegalName = fullLegalName.trim();
        kycStatus = kycStatus.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
