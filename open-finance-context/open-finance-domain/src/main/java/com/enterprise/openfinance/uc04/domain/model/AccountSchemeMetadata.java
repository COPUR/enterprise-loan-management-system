package com.enterprise.openfinance.uc04.domain.model;

public record AccountSchemeMetadata(
        String accountId,
        String schemeName,
        String secondaryIdentification
) {

    public AccountSchemeMetadata {
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (isBlank(schemeName)) {
            throw new IllegalArgumentException("schemeName is required");
        }

        accountId = accountId.trim();
        schemeName = schemeName.trim();
        secondaryIdentification = secondaryIdentification == null ? null : secondaryIdentification.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
