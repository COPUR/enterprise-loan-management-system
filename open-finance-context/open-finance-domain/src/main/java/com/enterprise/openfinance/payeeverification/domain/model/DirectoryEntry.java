package com.enterprise.openfinance.payeeverification.domain.model;

import java.util.Locale;

public record DirectoryEntry(
        String schemeName,
        String identification,
        String legalName,
        AccountStatus accountStatus
) {
    public DirectoryEntry {
        if (isBlank(schemeName)) {
            throw new IllegalArgumentException("schemeName is required");
        }
        if (isBlank(identification)) {
            throw new IllegalArgumentException("identification is required");
        }
        if (isBlank(legalName)) {
            throw new IllegalArgumentException("legalName is required");
        }
        if (accountStatus == null) {
            throw new IllegalArgumentException("accountStatus is required");
        }

        schemeName = schemeName.trim().toUpperCase(Locale.ROOT);
        identification = identification.trim().replace(" ", "");
        legalName = legalName.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
