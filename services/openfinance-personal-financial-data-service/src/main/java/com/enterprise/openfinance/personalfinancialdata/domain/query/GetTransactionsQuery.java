package com.enterprise.openfinance.personalfinancialdata.domain.query;

import java.time.Instant;

public record GetTransactionsQuery(
        String consentId,
        String tppId,
        String accountId,
        String interactionId,
        Instant fromBookingDateTime,
        Instant toBookingDateTime,
        Integer page,
        Integer pageSize
) {

    public GetTransactionsQuery {
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
        if (fromBookingDateTime != null && toBookingDateTime != null && toBookingDateTime.isBefore(fromBookingDateTime)) {
            throw new IllegalArgumentException("toBookingDateTime must be >= fromBookingDateTime");
        }
        if (page != null && page < 1) {
            throw new IllegalArgumentException("page must be >= 1");
        }
        if (pageSize != null && pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }

        consentId = consentId.trim();
        tppId = tppId.trim();
        accountId = accountId.trim();
        interactionId = interactionId.trim();
    }

    public int resolvePage() {
        return page == null ? 1 : page;
    }

    public int resolvePageSize(int defaultPageSize, int maxPageSize) {
        int resolved = pageSize == null ? defaultPageSize : pageSize;
        return Math.min(Math.max(1, resolved), maxPageSize);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
