package com.bank.loanmanagement.security.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FAPITokenRequest {
    private String clientId;
    private String scope;
    private String redirectUri;
    private String codeVerifier;
    private String code;
    private String dpopJkt;
    private String grantType;
    private String dpopProof;
    private String userId;
}

// FAPITokenRefreshRequest and FAPITokenValidationRequest are defined as records in separate files

@Data
@Builder
class FAPITokenValidationResult {
    private boolean valid;
    private String subject;
    private String scope;
    private long expiresAt;
}

@Data
@Builder
class FAPITokenRevocationRequest {
    private String token;
    private String tokenTypeHint;
    private String clientId;
}

@Data
@Builder
class TemporaryTokenRequest {
    private String purpose;
    private String scope;
    private Long ttlSeconds;
}

@Data
@Builder
class TemporaryTokenResponse {
    private String temporaryToken;
    private Long expiresIn;
    private String purpose;
}

// TokenAnalytics, TokenBinding and StoredTokenData are now in separate files

@Data
@Builder
class RateLimitingService {
    public boolean isAllowed(String key) { return true; }
}