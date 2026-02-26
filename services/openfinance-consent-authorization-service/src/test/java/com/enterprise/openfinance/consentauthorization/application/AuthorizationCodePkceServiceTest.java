package com.enterprise.openfinance.consentauthorization.application;

import com.enterprise.openfinance.consentauthorization.domain.command.AuthorizeWithPkceCommand;
import com.enterprise.openfinance.consentauthorization.domain.command.ExchangeAuthorizationCodeCommand;
import com.enterprise.openfinance.consentauthorization.domain.model.AuthorizationCodeGrant;
import com.enterprise.openfinance.consentauthorization.domain.port.in.ConsentManagementUseCase;
import com.enterprise.openfinance.consentauthorization.domain.port.out.AuthorizationCodeStore;
import com.enterprise.openfinance.consentauthorization.infrastructure.config.OAuth2PkceProperties;
import com.enterprise.openfinance.consentauthorization.infrastructure.rest.OAuthException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class AuthorizationCodePkceServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-11T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void shouldIssueAndExchangeAuthorizationCode() {
        ConsentManagementUseCase consentUseCase = Mockito.mock(ConsentManagementUseCase.class);
        Mockito.when(consentUseCase.hasActiveConsentForScopes(
                Mockito.eq("CONSENT-100"),
                Mockito.any(Set.class)
        )).thenReturn(true);

        InMemoryAuthorizationCodeStoreStub store = new InMemoryAuthorizationCodeStoreStub();
        PkceService pkceService = new PkceService(payload -> {
            try {
                return java.security.MessageDigest.getInstance("SHA-256").digest(payload);
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }, new SecureRandom());
        OAuth2PkceProperties properties = new OAuth2PkceProperties();

        AuthorizationCodePkceService service = new AuthorizationCodePkceService(
                consentUseCase,
                store,
                pkceService,
                properties,
                CLOCK,
                new SecureRandom()
        );

        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        String challenge = pkceService.deriveS256CodeChallenge(verifier);

        var redirect = service.authorize(new AuthorizeWithPkceCommand(
                "code",
                "client-app",
                "https://tpp.example/callback",
                "ReadAccounts ReadBalances",
                "state-1",
                "CONSENT-100",
                challenge,
                "S256"
        ));

        assertThat(redirect.redirectUri()).contains("code=");
        assertThat(redirect.redirectUri()).contains("state=state-1");
        assertThat(store.findByCode(redirect.code())).isPresent();

        var token = service.exchange(new ExchangeAuthorizationCodeCommand(
                "authorization_code",
                redirect.code(),
                verifier,
                "client-app",
                "https://tpp.example/callback"
        ));

        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.accessToken()).isNotBlank();
        assertThat(token.refreshToken()).isNotBlank();
    }

    @Test
    void shouldRejectAuthorizationWhenConsentIsNotActive() {
        ConsentManagementUseCase consentUseCase = Mockito.mock(ConsentManagementUseCase.class);
        Mockito.when(consentUseCase.hasActiveConsentForScopes(Mockito.anyString(), Mockito.any(Set.class)))
                .thenReturn(false);

        AuthorizationCodePkceService service = new AuthorizationCodePkceService(
                consentUseCase,
                new InMemoryAuthorizationCodeStoreStub(),
                new PkceService(payload -> payload, new SecureRandom()),
                new OAuth2PkceProperties(),
                CLOCK,
                new SecureRandom()
        );

        assertThatThrownBy(() -> service.authorize(new AuthorizeWithPkceCommand(
                "code",
                "client-app",
                "https://tpp.example/callback",
                "ReadAccounts",
                null,
                "CONSENT-200",
                "challenge",
                "S256"
        )))
                .isInstanceOf(OAuthException.class)
                .hasMessageContaining("consent is not active");
    }

    @Test
    void shouldRejectCodeReplay() {
        ConsentManagementUseCase consentUseCase = Mockito.mock(ConsentManagementUseCase.class);
        Mockito.when(consentUseCase.hasActiveConsentForScopes(Mockito.anyString(), Mockito.any(Set.class)))
                .thenReturn(true);

        PkceService pkceService = new PkceService(payload -> {
            try {
                return java.security.MessageDigest.getInstance("SHA-256").digest(payload);
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }, new SecureRandom());

        AuthorizationCodePkceService service = new AuthorizationCodePkceService(
                consentUseCase,
                new InMemoryAuthorizationCodeStoreStub(),
                pkceService,
                new OAuth2PkceProperties(),
                CLOCK,
                new SecureRandom()
        );

        String verifier = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";
        String challenge = pkceService.deriveS256CodeChallenge(verifier);
        String code = service.authorize(new AuthorizeWithPkceCommand(
                "code",
                "client-app",
                "https://tpp.example/callback",
                "ReadAccounts",
                null,
                "CONSENT-300",
                challenge,
                "S256"
        )).code();

        service.exchange(new ExchangeAuthorizationCodeCommand(
                "authorization_code",
                code,
                verifier,
                "client-app",
                "https://tpp.example/callback"
        ));

        assertThatThrownBy(() -> service.exchange(new ExchangeAuthorizationCodeCommand(
                "authorization_code",
                code,
                verifier,
                "client-app",
                "https://tpp.example/callback"
        )))
                .isInstanceOf(OAuthException.class)
                .hasMessageContaining("already been consumed");
    }

    private static final class InMemoryAuthorizationCodeStoreStub implements AuthorizationCodeStore {
        private final Map<String, AuthorizationCodeGrant> store = new ConcurrentHashMap<>();

        @Override
        public AuthorizationCodeGrant save(AuthorizationCodeGrant grant) {
            store.put(grant.getCode(), grant);
            return grant;
        }

        @Override
        public Optional<AuthorizationCodeGrant> findByCode(String code) {
            return Optional.ofNullable(store.get(code));
        }
    }
}

