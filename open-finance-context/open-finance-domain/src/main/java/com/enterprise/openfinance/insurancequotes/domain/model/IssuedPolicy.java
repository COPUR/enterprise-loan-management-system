package com.enterprise.openfinance.insurancequotes.domain.model;

public record IssuedPolicy(
        String policyId,
        String policyNumber,
        String certificateId
) {

    public IssuedPolicy {
        if (isBlank(policyId)) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (isBlank(policyNumber)) {
            throw new IllegalArgumentException("policyNumber is required");
        }
        if (isBlank(certificateId)) {
            throw new IllegalArgumentException("certificateId is required");
        }

        policyId = policyId.trim();
        policyNumber = policyNumber.trim();
        certificateId = certificateId.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
