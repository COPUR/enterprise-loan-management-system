package com.enterprise.openfinance.uc04.domain.query;

public record GetPartiesMetadataQuery(
        String consentId,
        String tppId,
        String accountId,
        String interactionId
) {

    public GetPartiesMetadataQuery {
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }

        consentId = consentId.trim();
        tppId = tppId.trim();
        accountId = accountId.trim();
        interactionId = interactionId.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
