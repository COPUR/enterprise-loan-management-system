package com.enterprise.openfinance.uc12.infrastructure.config;

import com.enterprise.openfinance.uc12.domain.model.OnboardingSettings;
import com.enterprise.openfinance.uc12.domain.port.out.SanctionsScreeningPort;
import com.enterprise.openfinance.uc12.infrastructure.compliance.RulesSanctionsScreeningAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.HashSet;

@Configuration
@EnableConfigurationProperties({Uc12CacheProperties.class, Uc12ComplianceProperties.class})
public class Uc12Configuration {

    @Bean
    public Clock onboardingClock() {
        return Clock.systemUTC();
    }

    @Bean
    public OnboardingSettings onboardingSettings(Uc12CacheProperties cacheProperties) {
        return new OnboardingSettings(
                cacheProperties.getIdempotencyTtl(),
                cacheProperties.getCacheTtl(),
                cacheProperties.getAccountPrefix()
        );
    }

    @Bean
    public SanctionsScreeningPort sanctionsScreeningPort(Uc12ComplianceProperties complianceProperties) {
        return new RulesSanctionsScreeningAdapter(new HashSet<>(complianceProperties.getBlockedNames()));
    }
}
