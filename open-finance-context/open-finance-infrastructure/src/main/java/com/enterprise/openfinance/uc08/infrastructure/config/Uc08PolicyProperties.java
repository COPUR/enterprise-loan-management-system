package com.enterprise.openfinance.uc08.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openfinance.uc08.policy")
public class Uc08PolicyProperties {

    private Duration idempotencyTtl = Duration.ofHours(24);

    public Duration getIdempotencyTtl() {
        return idempotencyTtl;
    }

    public void setIdempotencyTtl(Duration idempotencyTtl) {
        this.idempotencyTtl = idempotencyTtl;
    }
}
