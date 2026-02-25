package com.enterprise.openfinance.consentauthorization.domain.command;

public record AuthorizeWithPkceCommand(
        String responseType,
        String clientId,
        String redirectUri,
        String scope,
        String state,
        String consentId,
        String codeChallenge,
        String codeChallengeMethod
) {

    public AuthorizeWithPkceCommand {
        if (isBlank(responseType)) {
            throw new IllegalArgumentException("response_type is required");
        }
        if (isBlank(clientId)) {
            throw new IllegalArgumentException("client_id is required");
        }
        if (isBlank(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri is required");
        }
        if (isBlank(scope)) {
            throw new IllegalArgumentException("scope is required");
        }
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consent_id is required");
        }
        if (isBlank(codeChallenge)) {
            throw new IllegalArgumentException("code_challenge is required");
        }
        if (isBlank(codeChallengeMethod)) {
            throw new IllegalArgumentException("code_challenge_method is required");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

