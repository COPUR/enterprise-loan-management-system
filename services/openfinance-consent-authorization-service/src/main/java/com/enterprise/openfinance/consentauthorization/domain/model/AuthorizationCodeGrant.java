package com.enterprise.openfinance.consentauthorization.domain.model;

import java.time.Instant;

public final class AuthorizationCodeGrant {

    private final String code;
    private final String clientId;
    private final String redirectUri;
    private final String scope;
    private final String consentId;
    private final String codeChallenge;
    private final String codeChallengeMethod;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private boolean consumed;
    private Instant consumedAt;

    public AuthorizationCodeGrant(String code,
                                  String clientId,
                                  String redirectUri,
                                  String scope,
                                  String consentId,
                                  String codeChallenge,
                                  String codeChallengeMethod,
                                  Instant issuedAt,
                                  Instant expiresAt) {
        if (isBlank(code)) {
            throw new IllegalArgumentException("code is required");
        }
        if (isBlank(clientId)) {
            throw new IllegalArgumentException("clientId is required");
        }
        if (isBlank(redirectUri)) {
            throw new IllegalArgumentException("redirectUri is required");
        }
        if (isBlank(scope)) {
            throw new IllegalArgumentException("scope is required");
        }
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (isBlank(codeChallenge)) {
            throw new IllegalArgumentException("codeChallenge is required");
        }
        if (isBlank(codeChallengeMethod)) {
            throw new IllegalArgumentException("codeChallengeMethod is required");
        }
        if (issuedAt == null) {
            throw new IllegalArgumentException("issuedAt is required");
        }
        if (expiresAt == null || !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }

        this.code = code.trim();
        this.clientId = clientId.trim();
        this.redirectUri = redirectUri.trim();
        this.scope = scope.trim();
        this.consentId = consentId.trim();
        this.codeChallenge = codeChallenge.trim();
        this.codeChallengeMethod = codeChallengeMethod.trim();
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean matchesClientAndRedirect(String candidateClientId, String candidateRedirectUri) {
        return clientId.equals(candidateClientId) && redirectUri.equals(candidateRedirectUri);
    }

    public void consume(Instant at) {
        if (consumed) {
            throw new IllegalStateException("authorization code already consumed");
        }
        if (isExpired(at)) {
            throw new IllegalStateException("authorization code expired");
        }
        consumed = true;
        consumedAt = at;
    }

    public String getCode() {
        return code;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public String getConsentId() {
        return consentId;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

