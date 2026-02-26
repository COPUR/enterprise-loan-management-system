package com.enterprise.openfinance.insurancedata.infrastructure.config;

import com.enterprise.openfinance.insurancedata.domain.model.InsuranceDataSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({InsuranceDataCacheProperties.class, InsuranceDataPolicyProperties.class})
public class InsuranceDataConfiguration {

    @Bean
    public Clock insuranceDataClock() {
        return Clock.systemUTC();
    }

    @Bean
    public InsuranceDataSettings insuranceDataSettings(InsuranceDataCacheProperties cacheProperties,
                                                       InsuranceDataPolicyProperties policyProperties) {
        return new InsuranceDataSettings(
                cacheProperties.getTtl(),
                policyProperties.getDefaultPageSize(),
                policyProperties.getMaxPageSize()
        );
    }
}
