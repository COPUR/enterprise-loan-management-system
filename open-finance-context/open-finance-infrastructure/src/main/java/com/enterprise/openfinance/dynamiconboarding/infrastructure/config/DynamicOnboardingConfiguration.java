package com.enterprise.openfinance.dynamiconboarding.infrastructure.config;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingSettings;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.SanctionsScreeningPort;
import com.enterprise.openfinance.dynamiconboarding.infrastructure.compliance.RulesSanctionsScreeningAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.HashSet;

@Configuration
@EnableConfigurationProperties({DynamicOnboardingCacheProperties.class, DynamicOnboardingComplianceProperties.class})
public class DynamicOnboardingConfiguration {

    @Bean
    public Clock onboardingClock() {
        return Clock.systemUTC();
    }

    @Bean
    public OnboardingSettings onboardingSettings(DynamicOnboardingCacheProperties cacheProperties) {
        return new OnboardingSettings(
                cacheProperties.getIdempotencyTtl(),
                cacheProperties.getCacheTtl(),
                cacheProperties.getAccountPrefix()
        );
    }

    @Bean
    public SanctionsScreeningPort sanctionsScreeningPort(DynamicOnboardingComplianceProperties complianceProperties) {
        return new RulesSanctionsScreeningAdapter(new HashSet<>(complianceProperties.getBlockedNames()));
    }
}
