package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.security;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception.InternalTokenUnauthorizedException;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config.InternalSecurityProperties;
import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NimbusHmacJwtAdapterTest {

    @Test
    void shouldIssueAndVerifyToken() throws JOSEException {
        InternalSecurityProperties properties = defaultProperties("0123456789abcdef0123456789abcdef");
        NimbusHmacJwtAdapter adapter = new NimbusHmacJwtAdapter(properties);
        Instant issuedAt = Instant.parse("2026-02-25T00:00:00Z");

        var token = adapter.issueToken("svc-user", issuedAt);
        var principal = adapter.verify(token.accessToken(), issuedAt.plusSeconds(10));

        assertThat(principal.subject()).isEqualTo("svc-user");
        assertThat(principal.jti()).isEqualTo(token.jti());
    }

    @Test
    void shouldRejectShortHmacSecret() {
        InternalSecurityProperties properties = defaultProperties("too-short");

        assertThatThrownBy(() -> new NimbusHmacJwtAdapter(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void shouldRejectMalformedOrInvalidToken() throws JOSEException {
        NimbusHmacJwtAdapter adapter = new NimbusHmacJwtAdapter(defaultProperties("0123456789abcdef0123456789abcdef"));
        Instant now = Instant.parse("2026-02-25T00:00:00Z");

        assertThatThrownBy(() -> adapter.verify("not-a-jwt", now))
                .isInstanceOf(InternalTokenUnauthorizedException.class)
                .hasMessageContaining("Malformed token");

        NimbusHmacJwtAdapter differentSecretAdapter = new NimbusHmacJwtAdapter(defaultProperties("abcdef0123456789abcdef0123456789"));
        String issuedToken = differentSecretAdapter.issueToken("svc-user", now).accessToken();
        assertThatThrownBy(() -> adapter.verify(issuedToken, now.plusSeconds(1)))
                .isInstanceOf(InternalTokenUnauthorizedException.class)
                .hasMessageContaining("Invalid token signature");
    }

    @Test
    void shouldRejectExpiredToken() throws JOSEException {
        InternalSecurityProperties properties = defaultProperties("0123456789abcdef0123456789abcdef");
        properties.setAccessTokenTtl(Duration.ofSeconds(1));
        properties.setAllowedClockSkew(Duration.ZERO);
        NimbusHmacJwtAdapter adapter = new NimbusHmacJwtAdapter(properties);
        Instant issuedAt = Instant.parse("2026-02-25T00:00:00Z");
        String token = adapter.issueToken("svc-user", issuedAt).accessToken();

        assertThatThrownBy(() -> adapter.verify(token, issuedAt.plusSeconds(3)))
                .isInstanceOf(InternalTokenUnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    private static InternalSecurityProperties defaultProperties(String secret) {
        InternalSecurityProperties properties = new InternalSecurityProperties();
        properties.setIssuer("issuer");
        properties.setAudience("audience");
        properties.setJwtHmacSecret(secret);
        properties.setAccessTokenTtl(Duration.ofMinutes(10));
        properties.setAllowedClockSkew(Duration.ofSeconds(30));
        properties.setInternalUsername("runtime-user");
        properties.setInternalPassword("runtime-password");
        return properties;
    }
}
