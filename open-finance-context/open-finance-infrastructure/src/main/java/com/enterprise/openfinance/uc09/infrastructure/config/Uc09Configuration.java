package com.enterprise.openfinance.uc09.infrastructure.config;

import com.enterprise.openfinance.uc09.domain.model.InsuranceDataSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({Uc09CacheProperties.class, Uc09PolicyProperties.class})
public class Uc09Configuration {

    @Bean
    public Clock insuranceDataClock() {
        return Clock.systemUTC();
    }

    @Bean
    public InsuranceDataSettings insuranceDataSettings(Uc09CacheProperties cacheProperties,
                                                       Uc09PolicyProperties policyProperties) {
        return new InsuranceDataSettings(
                cacheProperties.getTtl(),
                policyProperties.getDefaultPageSize(),
                policyProperties.getMaxPageSize()
        );
    }
}
