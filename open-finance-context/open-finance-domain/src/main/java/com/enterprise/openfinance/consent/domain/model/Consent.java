package com.enterprise.openfinance.consent.domain.model;

import com.enterprise.openfinance.consent.domain.command.CreateConsentCommand;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class Consent {

    private static final Pattern SCOPE_SEPARATOR_PATTERN = Pattern.compile("[^A-Z0-9]");

    private static final Set<String> SUPPORTED_SCOPES = Set.of(
            "READACCOUNTS",
            "READBALANCES",
            "READTRANSACTIONS",
            "READBENEFICIARIES",
            "READDIRECTDEBITS",
            "READSTANDINGORDERS",
            "READPARTIES",
            "READSCHEDULEDPAYMENTS",
            "INITIATEPAYMENTS",
            "INITIATEBULKPAYMENTS",
            "INITIATEVRP",
            "READPOLICIES",
            "READPRODUCTS",
            "READATMS"
    );

    private final String consentId;
    private final String customerId;
    private final String participantId;
    private final Set<String> scopes;
    private final String purpose;
    private final Instant createdAt;
    private final Instant expiresAt;
    private ConsentStatus status;
    private Instant authorizedAt;
    private Instant revokedAt;
    private String revocationReason;

    private Consent(
            String consentId,
            String customerId,
            String participantId,
            Set<String> scopes,
            String purpose,
            Instant createdAt,
            Instant expiresAt) {
        this.consentId = consentId;
        this.customerId = customerId;
        this.participantId = participantId;
        this.scopes = Set.copyOf(scopes);
        this.purpose = purpose;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = ConsentStatus.PENDING;
    }

    public static Consent create(CreateConsentCommand command, Clock clock) {
        Instant now = Instant.now(clock);
        if (!command.expiresAt().isAfter(now)) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }

        Set<String> normalizedScopes = new LinkedHashSet<>();
        for (String scope : command.scopes()) {
            String normalizedScope = normalizeScope(scope);
            if (!SUPPORTED_SCOPES.contains(normalizedScope)) {
                throw new IllegalArgumentException("invalid scope: " + scope);
            }
            normalizedScopes.add(normalizedScope);
        }

        return new Consent(
                "CONSENT-" + UUID.randomUUID().toString().toUpperCase(),
                command.customerId().trim(),
                command.participantId().trim(),
                normalizedScopes,
                command.purpose().trim(),
                now,
                command.expiresAt());
    }

    public void authorize(Instant at) {
        if (status == ConsentStatus.REVOKED) {
            throw new IllegalStateException("cannot authorize a revoked consent");
        }
        if (isExpired(at)) {
            status = ConsentStatus.EXPIRED;
            throw new IllegalStateException("cannot authorize an expired consent");
        }
        if (status == ConsentStatus.AUTHORIZED) {
            return;
        }
        status = ConsentStatus.AUTHORIZED;
        authorizedAt = at;
    }

    public void revoke(String reason, Instant at) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("revocation reason is required");
        }
        if (status == ConsentStatus.REVOKED) {
            return;
        }
        status = ConsentStatus.REVOKED;
        revokedAt = at;
        revocationReason = reason.trim();
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isActive(Instant now) {
        if (status != ConsentStatus.REVOKED && isExpired(now)) {
            status = ConsentStatus.EXPIRED;
        }
        if (status != ConsentStatus.AUTHORIZED) {
            return false;
        }
        return true;
    }

    public boolean coversScopes(Set<String> requiredScopes) {
        if (requiredScopes == null || requiredScopes.isEmpty()) {
            return true;
        }

        Set<String> normalizedRequired = new LinkedHashSet<>();
        for (String scope : requiredScopes) {
            normalizedRequired.add(normalizeScope(scope));
        }
        return scopes.containsAll(normalizedRequired);
    }

    private static String normalizeScope(String scope) {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope cannot be blank");
        }
        String upper = scope.trim().toUpperCase(Locale.ROOT);
        String canonical = SCOPE_SEPARATOR_PATTERN.matcher(upper).replaceAll("");
        if (canonical.isBlank()) {
            throw new IllegalArgumentException("scope cannot be blank");
        }
        return canonical;
    }

    public String getConsentId() {
        return consentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public String getPurpose() {
        return purpose;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getRevocationReason() {
        return revocationReason;
    }
}
