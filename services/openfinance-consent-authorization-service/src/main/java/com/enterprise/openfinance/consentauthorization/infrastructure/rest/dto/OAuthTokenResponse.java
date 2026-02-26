package com.enterprise.openfinance.consentauthorization.infrastructure.rest.dto;

import com.enterprise.openfinance.consentauthorization.domain.model.TokenResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuthTokenResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("expires_in")
        long expiresIn,
        @JsonProperty("refresh_token")
        String refreshToken,
        String scope
) {

    public static OAuthTokenResponse from(TokenResult result) {
        return new OAuthTokenResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresIn(),
                result.refreshToken(),
                result.scope()
        );
    }
}

