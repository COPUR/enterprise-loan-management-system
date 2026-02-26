package com.enterprise.openfinance.payeeverification.domain.model;

public record ConfirmationResult(
        AccountStatus accountStatus,
        NameMatchDecision nameMatched,
        String matchedName,
        int matchScore,
        boolean fromCache
) {
    public ConfirmationResult {
        if (accountStatus == null) {
            throw new IllegalArgumentException("accountStatus is required");
        }
        if (nameMatched == null) {
            throw new IllegalArgumentException("nameMatched is required");
        }
        if (matchScore < 0 || matchScore > 100) {
            throw new IllegalArgumentException("matchScore must be between 0 and 100");
        }
        if (nameMatched != NameMatchDecision.CLOSE_MATCH) {
            matchedName = null;
        }
    }
}
