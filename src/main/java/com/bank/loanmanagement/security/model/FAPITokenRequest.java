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
}

@Data
@Builder
class FAPITokenRefreshRequest {
    private String refreshToken;
    private String clientId;
    private String scope;
}

@Data
@Builder
class FAPITokenValidationRequest {
    private String accessToken;
    private String dpopProof;
    private String httpMethod;
    private String httpUri;
}

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

@Data
@Builder
class TokenAnalytics {
    private Long totalTokensIssued;
    private Long activeTokens;
    private Double averageTokenLifetime;
}

@Data
@Builder
class TokenBinding {
    private String bindingType;
    private String bindingValue;
    private String algorithm;
}

@Data
@Builder
class StoredTokenData {
    private String tokenId;
    private String clientId;
    private Long issuedAt;
    private Long expiresAt;
    private String scope;
    private TokenBinding binding;
}

@Data
@Builder
class RateLimitingService {
    public boolean isAllowed(String key) { return true; }
}