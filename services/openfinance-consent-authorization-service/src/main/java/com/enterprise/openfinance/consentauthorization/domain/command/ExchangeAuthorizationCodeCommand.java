package com.enterprise.openfinance.consentauthorization.domain.command;

public record ExchangeAuthorizationCodeCommand(
        String grantType,
        String code,
        String codeVerifier,
        String clientId,
        String redirectUri
) {

    public ExchangeAuthorizationCodeCommand {
        if (isBlank(grantType)) {
            throw new IllegalArgumentException("grant_type is required");
        }
        if (isBlank(code)) {
            throw new IllegalArgumentException("code is required");
        }
        if (isBlank(codeVerifier)) {
            throw new IllegalArgumentException("code_verifier is required");
        }
        if (isBlank(clientId)) {
            throw new IllegalArgumentException("client_id is required");
        }
        if (isBlank(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

