package com.enterprise.openfinance.payeeverification.infrastructure.config;

import com.enterprise.openfinance.payeeverification.domain.model.ConfirmationSettings;
import com.enterprise.openfinance.payeeverification.domain.service.ConfirmationDecisionPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({CoPCacheProperties.class, CoPMatchingProperties.class})
public class ConfirmationOfPayeeConfiguration {

    @Bean
    public Clock confirmationOfPayeeClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ConfirmationSettings confirmationSettings(CoPMatchingProperties matchingProperties,
                                                     CoPCacheProperties cacheProperties) {
        return new ConfirmationSettings(
                matchingProperties.getCloseMatchThreshold(),
                cacheProperties.getTtl()
        );
    }

    @Bean
    public ConfirmationDecisionPolicy confirmationDecisionPolicy(CoPMatchingProperties matchingProperties) {
        return new ConfirmationDecisionPolicy(matchingProperties.getCloseMatchThreshold());
    }
}
