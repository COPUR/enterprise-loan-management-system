package com.enterprise.openfinance.insurancedata.domain.query;

public record GetMotorPoliciesQuery(
        String consentId,
        String tppId,
        String interactionId,
        Integer page,
        Integer pageSize
) {

    public GetMotorPoliciesQuery {
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(interactionId)) {
            throw new IllegalArgumentException("interactionId is required");
        }
        if (page != null && page <= 0) {
            throw new IllegalArgumentException("page must be positive");
        }
        if (pageSize != null && pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }

        consentId = consentId.trim();
        tppId = tppId.trim();
        interactionId = interactionId.trim();
    }

    public int resolvePage() {
        return page == null ? 1 : page;
    }

    public int resolvePageSize(int defaultPageSize, int maxPageSize) {
        int resolved = pageSize == null ? defaultPageSize : pageSize;
        return Math.min(resolved, maxPageSize);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
