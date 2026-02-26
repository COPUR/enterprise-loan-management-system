package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "openfinance.internal.security")
public class InternalSecurityProperties {

    @NotBlank
    private String issuer = "openfinance-internal-auth";

    @NotBlank
    private String audience = "openfinance-internal-clients";

    @NotBlank
    private String jwtHmacSecret = "RUNTIME_MANAGED__USE_SYSTEM_SECRETS_API";

    @NotNull
    private Duration accessTokenTtl = Duration.ofMinutes(15);

    @NotNull
    private Duration allowedClockSkew = Duration.ofSeconds(30);

    @NotBlank
    private String internalUsername = "RUNTIME_MANAGED_INTERNAL_USERNAME";

    @NotBlank
    private String internalPassword = "RUNTIME_MANAGED_INTERNAL_PASSWORD";

    @Min(1)
    private int maxFailedAttempts = 5;

    @NotNull
    private Duration failedAttemptWindow = Duration.ofMinutes(15);

    @NotNull
    private Duration lockDuration = Duration.ofMinutes(15);

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getJwtHmacSecret() {
        return jwtHmacSecret;
    }

    public void setJwtHmacSecret(String jwtHmacSecret) {
        this.jwtHmacSecret = jwtHmacSecret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getAllowedClockSkew() {
        return allowedClockSkew;
    }

    public void setAllowedClockSkew(Duration allowedClockSkew) {
        this.allowedClockSkew = allowedClockSkew;
    }

    public String getInternalUsername() {
        return internalUsername;
    }

    public void setInternalUsername(String internalUsername) {
        this.internalUsername = internalUsername;
    }

    public String getInternalPassword() {
        return internalPassword;
    }

    public void setInternalPassword(String internalPassword) {
        this.internalPassword = internalPassword;
    }

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public Duration getFailedAttemptWindow() {
        return failedAttemptWindow;
    }

    public void setFailedAttemptWindow(Duration failedAttemptWindow) {
        this.failedAttemptWindow = failedAttemptWindow;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(Duration lockDuration) {
        this.lockDuration = lockDuration;
    }
}
