package com.enterprise.openfinance.insurancedata.domain.model;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public record InsuranceConsentContext(
        String consentId,
        String tppId,
        Set<String> scopes,
        Set<String> policyIds,
        Instant expiresAt
) {

    private static final Pattern SCOPE_SEPARATOR_PATTERN = Pattern.compile("[^A-Z0-9]");

    public InsuranceConsentContext {
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }

        Set<String> normalizedScopes = new LinkedHashSet<>();
        for (String scope : scopes == null ? Set.<String>of() : scopes) {
            normalizedScopes.add(normalizeScope(scope));
        }

        Set<String> normalizedPolicyIds = new LinkedHashSet<>();
        for (String policyId : policyIds == null ? Set.<String>of() : policyIds) {
            if (isBlank(policyId)) {
                throw new IllegalArgumentException("policyId cannot be blank");
            }
            normalizedPolicyIds.add(policyId.trim());
        }

        consentId = consentId.trim();
        tppId = tppId.trim();
        scopes = Set.copyOf(normalizedScopes);
        policyIds = Set.copyOf(normalizedPolicyIds);
    }

    public boolean hasScope(String scope) {
        return scopes.contains(normalizeScope(scope));
    }

    public boolean allowsPolicy(String policyId) {
        return policyIds.contains(policyId);
    }

    public boolean belongsToTpp(String candidateTppId) {
        return tppId.equals(candidateTppId);
    }

    public boolean isActive(Instant now) {
        return expiresAt.isAfter(now);
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
