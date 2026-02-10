package com.enterprise.openfinance.businessfinancialdata.domain.model;

public record CorporateAccountSnapshot(
        String accountId,
        String corporateId,
        String masterAccountId,
        String iban,
        String currency,
        String status,
        String accountType,
        boolean virtual
) {

    public CorporateAccountSnapshot {
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (isBlank(corporateId)) {
            throw new IllegalArgumentException("corporateId is required");
        }
        if (isBlank(iban)) {
            throw new IllegalArgumentException("iban is required");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (isBlank(status)) {
            throw new IllegalArgumentException("status is required");
        }
        if (isBlank(accountType)) {
            throw new IllegalArgumentException("accountType is required");
        }
        if (virtual && isBlank(masterAccountId)) {
            throw new IllegalArgumentException("masterAccountId is required for virtual accounts");
        }

        accountId = accountId.trim();
        corporateId = corporateId.trim();
        masterAccountId = masterAccountId == null ? null : masterAccountId.trim();
        iban = iban.trim();
        currency = currency.trim();
        status = status.trim();
        accountType = accountType.trim();
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
