package com.enterprise.openfinance.uc06.infrastructure.config;

import com.enterprise.openfinance.uc06.domain.model.PaymentSettings;
import com.enterprise.openfinance.uc06.domain.service.PaymentStatusPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({Uc06IdempotencyProperties.class, Uc06SecurityProperties.class})
public class Uc06Configuration {

    @Bean
    public Clock paymentClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PaymentSettings paymentSettings(Uc06IdempotencyProperties properties) {
        return new PaymentSettings(properties.getTtl());
    }

    @Bean
    public PaymentStatusPolicy paymentStatusPolicy() {
        return new PaymentStatusPolicy();
    }
}
