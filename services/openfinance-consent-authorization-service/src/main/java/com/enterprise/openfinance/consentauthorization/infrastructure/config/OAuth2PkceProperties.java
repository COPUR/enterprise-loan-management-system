package com.enterprise.openfinance.consentauthorization.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openfinance.security.oauth2")
public class OAuth2PkceProperties {

    private Duration authorizationCodeTtl = Duration.ofMinutes(5);
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofHours(12);

    public Duration getAuthorizationCodeTtl() {
        return authorizationCodeTtl;
    }

    public void setAuthorizationCodeTtl(Duration authorizationCodeTtl) {
        this.authorizationCodeTtl = authorizationCodeTtl;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }
}

