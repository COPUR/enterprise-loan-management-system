package com.enterprise.openfinance.dynamiconboarding.domain.query;

public record GetOnboardingAccountQuery(
        String accountId,
        String tppId,
        String interactionId
) {

    public GetOnboardingAccountQuery {
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }

        accountId = accountId.trim();
        tppId = tppId.trim();
        interactionId = interactionId.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
