package com.enterprise.openfinance.businessfinancialdata.domain.query;

public record GetCorporateBalancesQuery(
        String consentId,
        String tppId,
        String masterAccountId,
        String interactionId
) {

    public GetCorporateBalancesQuery {
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(masterAccountId)) {
            throw new IllegalArgumentException("masterAccountId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }

        consentId = consentId.trim();
        tppId = tppId.trim();
        masterAccountId = masterAccountId.trim();
        interactionId = interactionId.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
