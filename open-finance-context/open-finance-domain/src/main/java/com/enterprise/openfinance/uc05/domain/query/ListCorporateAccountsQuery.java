package com.enterprise.openfinance.uc05.domain.query;

public record ListCorporateAccountsQuery(
        String consentId,
        String tppId,
        String interactionId,
        Boolean includeVirtual,
        String masterAccountId
) {

    public ListCorporateAccountsQuery {
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }

        consentId = consentId.trim();
        tppId = tppId.trim();
        interactionId = interactionId.trim();
        masterAccountId = isBlank(masterAccountId) ? null : masterAccountId.trim();
    }

    public boolean resolveIncludeVirtual() {
        return includeVirtual != null && includeVirtual;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
