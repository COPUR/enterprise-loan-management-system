package com.enterprise.openfinance.payeeverification.domain.model;

import java.util.Locale;

public record ConfirmationRequest(
        String identification,
        String schemeName,
        String name,
        String tppId,
        String interactionId
) {
    public ConfirmationRequest {
        if (isBlank(identification)) {
            throw new IllegalArgumentException("identification is required");
        }
        if (isBlank(schemeName)) {
            throw new IllegalArgumentException("schemeName is required");
        }
        if (isBlank(name)) {
            throw new IllegalArgumentException("name is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }

        identification = identification.trim().replace(" ", "");
        schemeName = schemeName.trim().toUpperCase(Locale.ROOT);
        name = name.trim();
        tppId = tppId.trim();
        interactionId = interactionId.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
