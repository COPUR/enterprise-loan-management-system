package com.enterprise.openfinance.uc12.domain.command;

public record CreateOnboardingAccountCommand(
        String tppId,
        String interactionId,
        String idempotencyKey,
        String encryptedKycPayload,
        String preferredCurrency
) {

    public CreateOnboardingAccountCommand {
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }
        if (isBlank(idempotencyKey)) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        if (isBlank(encryptedKycPayload)) {
            throw new IllegalArgumentException("encryptedKycPayload is required");
        }
        if (!isCurrency(preferredCurrency)) {
            throw new IllegalArgumentException("preferredCurrency must be ISO-4217 alpha-3");
        }

        tppId = tppId.trim();
        interactionId = interactionId.trim();
        idempotencyKey = idempotencyKey.trim();
        encryptedKycPayload = encryptedKycPayload.trim();
        preferredCurrency = preferredCurrency.trim().toUpperCase();
    }

    public String requestFingerprint() {
        return encryptedKycPayload + '|' + preferredCurrency;
    }

    private static boolean isCurrency(String value) {
        return value != null && value.trim().matches("[A-Za-z]{3}");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
