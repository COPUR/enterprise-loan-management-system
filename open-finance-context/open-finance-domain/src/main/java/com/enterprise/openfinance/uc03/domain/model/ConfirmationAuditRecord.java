package com.enterprise.openfinance.uc03.domain.model;

import java.time.Instant;

public record ConfirmationAuditRecord(
        String tppId,
        String interactionId,
        String schemeName,
        String identification,
        String requestedName,
        AccountStatus accountStatus,
        NameMatchDecision nameMatched,
        int matchScore,
        boolean fromCache,
        Instant timestamp
) {
    public ConfirmationAuditRecord {
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }
        if (isBlank(schemeName)) {
            throw new IllegalArgumentException("schemeName is required");
        }
        if (isBlank(identification)) {
            throw new IllegalArgumentException("identification is required");
        }
        if (isBlank(requestedName)) {
            throw new IllegalArgumentException("requestedName is required");
        }
        if (accountStatus == null) {
            throw new IllegalArgumentException("accountStatus is required");
        }
        if (nameMatched == null) {
            throw new IllegalArgumentException("nameMatched is required");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp is required");
        }
        if (matchScore < 0 || matchScore > 100) {
            throw new IllegalArgumentException("matchScore must be between 0 and 100");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
