package com.enterprise.openfinance.accountinformation.domain.model;

public record AccountSnapshot(
        String accountId,
        String psuId,
        String iban,
        String currency,
        String accountType,
        String status,
        String schemeName,
        String name
) {

    public AccountSnapshot {
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (isBlank(psuId)) {
            throw new IllegalArgumentException("psuId is required");
        }
        if (isBlank(iban)) {
            throw new IllegalArgumentException("iban is required");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (isBlank(accountType)) {
            throw new IllegalArgumentException("accountType is required");
        }
        if (isBlank(status)) {
            throw new IllegalArgumentException("status is required");
        }
        if (isBlank(schemeName)) {
            throw new IllegalArgumentException("schemeName is required");
        }

        accountId = accountId.trim();
        psuId = psuId.trim();
        iban = iban.trim();
        currency = currency.trim();
        accountType = accountType.trim();
        status = status.trim();
        schemeName = schemeName.trim();
        name = name == null ? null : name.trim();
    }

    public String maskedIban() {
        if (iban.length() <= 8) {
            return "****";
        }
        String prefix = iban.substring(0, 4);
        String suffix = iban.substring(iban.length() - 4);
        int middleLength = Math.max(4, iban.length() - 8);
        return prefix + "*".repeat(middleLength) + suffix;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
