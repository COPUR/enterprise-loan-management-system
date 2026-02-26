package com.enterprise.openfinance.insurancedata.domain.query;

public record GetMotorPolicyQuery(
        String consentId,
        String tppId,
        String policyId,
        String interactionId
) {

    public GetMotorPolicyQuery {
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(policyId)) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }

        consentId = consentId.trim();
        tppId = tppId.trim();
        policyId = policyId.trim();
        interactionId = interactionId.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
