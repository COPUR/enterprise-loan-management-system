package com.enterprise.openfinance.corporatetreasury.domain.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public record CorporateConsentContext(
        String consentId,
        String tppId,
        String corporateId,
        String entitlement,
        Set<String> scopes,
        Set<String> accountIds,
        Instant expiresAt
) {

    private static final Pattern SCOPE_SEPARATOR_PATTERN = Pattern.compile("[^A-Z0-9]");

    public CorporateConsentContext {
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(corporateId)) {
            throw new IllegalArgumentException("corporateId is required");
        }
        if (isBlank(entitlement)) {
            throw new IllegalArgumentException("entitlement is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }

        Set<String> normalizedScopes = new LinkedHashSet<>();
        for (String scope : scopes == null ? Set.<String>of() : scopes) {
            normalizedScopes.add(normalizeScope(scope));
        }

        Set<String> normalizedAccounts = new LinkedHashSet<>();
        for (String accountId : accountIds == null ? Set.<String>of() : accountIds) {
            if (isBlank(accountId)) {
                throw new IllegalArgumentException("accountId cannot be blank");
            }
            normalizedAccounts.add(accountId.trim());
        }

        consentId = consentId.trim();
        tppId = tppId.trim();
        corporateId = corporateId.trim();
        entitlement = entitlement.trim().toUpperCase(Locale.ROOT);
        scopes = Set.copyOf(normalizedScopes);
        accountIds = Set.copyOf(normalizedAccounts);
    }

    public boolean hasScope(String scope) {
        return scopes.contains(normalizeScope(scope));
    }

    public boolean allowsAccount(String accountId) {
        return accountIds.contains(accountId);
    }

    public boolean belongsToTpp(String candidateTppId) {
        return tppId.equals(candidateTppId);
    }

    public boolean isActive(Instant now) {
        return expiresAt.isAfter(now);
    }

    public boolean isRestricted() {
        return "RESTRICTED".equals(entitlement);
    }

    private static String normalizeScope(String scope) {
        if (isBlank(scope)) {
            throw new IllegalArgumentException("scope cannot be blank");
        }
        String upper = scope.trim().toUpperCase(Locale.ROOT);
        String canonical = SCOPE_SEPARATOR_PATTERN.matcher(upper).replaceAll("");
        if (canonical.isBlank()) {
            throw new IllegalArgumentException("scope cannot be blank");
        }
        return canonical;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
