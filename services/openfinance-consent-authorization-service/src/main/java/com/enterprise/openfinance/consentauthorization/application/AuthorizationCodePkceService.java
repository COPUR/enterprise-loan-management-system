package com.enterprise.openfinance.consentauthorization.application;

import com.enterprise.openfinance.consentauthorization.domain.command.AuthorizeWithPkceCommand;
import com.enterprise.openfinance.consentauthorization.domain.command.ExchangeAuthorizationCodeCommand;
import com.enterprise.openfinance.consentauthorization.domain.model.AuthorizationCodeGrant;
import com.enterprise.openfinance.consentauthorization.domain.model.AuthorizationRedirect;
import com.enterprise.openfinance.consentauthorization.domain.model.TokenResult;
import com.enterprise.openfinance.consentauthorization.domain.port.in.ConsentManagementUseCase;
import com.enterprise.openfinance.consentauthorization.domain.port.in.PkceAuthorizationUseCase;
import com.enterprise.openfinance.consentauthorization.domain.port.out.AuthorizationCodeStore;
import com.enterprise.openfinance.consentauthorization.infrastructure.config.OAuth2PkceProperties;
import com.enterprise.openfinance.consentauthorization.infrastructure.rest.OAuthException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class AuthorizationCodePkceService implements PkceAuthorizationUseCase {

    private final ConsentManagementUseCase consentManagementUseCase;
    private final AuthorizationCodeStore authorizationCodeStore;
    private final PkceService pkceService;
    private final OAuth2PkceProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;

    public AuthorizationCodePkceService(ConsentManagementUseCase consentManagementUseCase,
                                        AuthorizationCodeStore authorizationCodeStore,
                                        PkceService pkceService,
                                        OAuth2PkceProperties properties,
                                        Clock clock,
                                        SecureRandom secureRandom) {
        this.consentManagementUseCase = consentManagementUseCase;
        this.authorizationCodeStore = authorizationCodeStore;
        this.pkceService = pkceService;
        this.properties = properties;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    @Override
    public AuthorizationRedirect authorize(AuthorizeWithPkceCommand command) {
        if (!"code".equalsIgnoreCase(command.responseType())) {
            throw new OAuthException("unsupported_response_type", "response_type must be code", HttpStatus.BAD_REQUEST);
        }
        if (!"S256".equalsIgnoreCase(command.codeChallengeMethod())) {
            throw new OAuthException("invalid_request", "code_challenge_method must be S256", HttpStatus.BAD_REQUEST);
        }

        Set<String> requiredScopes = normalizeScopes(command.scope());
        boolean hasConsent = consentManagementUseCase.hasActiveConsentForScopes(command.consentId(), requiredScopes);
        if (!hasConsent) {
            throw new OAuthException("access_denied", "consent is not active for requested scopes", HttpStatus.FORBIDDEN);
        }

        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(properties.getAuthorizationCodeTtl());
        String code = generateOpaqueToken(32);

        AuthorizationCodeGrant grant = new AuthorizationCodeGrant(
                code,
                command.clientId(),
                command.redirectUri(),
                command.scope(),
                command.consentId(),
                command.codeChallenge(),
                command.codeChallengeMethod(),
                issuedAt,
                expiresAt
        );

        authorizationCodeStore.save(grant);

        String redirectUri = UriComponentsBuilder.fromUriString(command.redirectUri())
                .queryParam("code", code)
                .queryParamIfPresent("state", java.util.Optional.ofNullable(command.state()).filter(value -> !value.isBlank()))
                .build(true)
                .toUriString();

        return new AuthorizationRedirect(code, command.state(), redirectUri, expiresAt);
    }

    @Override
    public TokenResult exchange(ExchangeAuthorizationCodeCommand command) {
        if (!"authorization_code".equals(command.grantType())) {
            throw new OAuthException("unsupported_grant_type", "grant_type must be authorization_code", HttpStatus.BAD_REQUEST);
        }

        AuthorizationCodeGrant grant = authorizationCodeStore.findByCode(command.code())
                .orElseThrow(() -> new OAuthException("invalid_grant", "authorization code is invalid", HttpStatus.BAD_REQUEST));

        Instant now = Instant.now(clock);
        if (grant.isConsumed()) {
            throw new OAuthException("invalid_grant", "authorization code has already been consumed", HttpStatus.BAD_REQUEST);
        }
        if (grant.isExpired(now)) {
            throw new OAuthException("invalid_grant", "authorization code expired", HttpStatus.BAD_REQUEST);
        }
        if (!grant.matchesClientAndRedirect(command.clientId(), command.redirectUri())) {
            throw new OAuthException("invalid_grant", "client_id or redirect_uri mismatch", HttpStatus.BAD_REQUEST);
        }
        if (!pkceService.verifyCodeChallenge(command.codeVerifier(), grant.getCodeChallenge())) {
            throw new OAuthException("invalid_grant", "code_verifier does not match code_challenge", HttpStatus.BAD_REQUEST);
        }

        grant.consume(now);
        authorizationCodeStore.save(grant);

        return new TokenResult(
                generateOpaqueToken(48),
                "Bearer",
                properties.getAccessTokenTtl().toSeconds(),
                generateOpaqueToken(48),
                grant.getScope()
        );
    }

    private String generateOpaqueToken(int bytes) {
        byte[] raw = new byte[bytes];
        secureRandom.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static Set<String> normalizeScopes(String scope) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String candidate : scope.split("\\s+")) {
            String value = candidate.trim();
            if (!value.isBlank()) {
                normalized.add(value.toUpperCase(Locale.ROOT));
            }
        }
        return normalized;
    }
}

