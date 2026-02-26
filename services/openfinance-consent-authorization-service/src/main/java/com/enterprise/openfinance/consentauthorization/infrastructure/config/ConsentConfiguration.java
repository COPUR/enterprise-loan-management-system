package com.enterprise.openfinance.consentauthorization.infrastructure.config;

import com.enterprise.openfinance.consentauthorization.infrastructure.security.SoftHsmProperties;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.config.InternalSecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties({OAuth2PkceProperties.class, SoftHsmProperties.class, InternalSecurityProperties.class})
public class ConsentConfiguration {

    @Bean
    public Clock consentClock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
