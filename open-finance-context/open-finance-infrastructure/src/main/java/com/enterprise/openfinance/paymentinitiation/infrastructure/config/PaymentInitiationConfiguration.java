package com.enterprise.openfinance.paymentinitiation.infrastructure.config;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentSettings;
import com.enterprise.openfinance.paymentinitiation.domain.service.PaymentStatusPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({PaymentInitiationIdempotencyProperties.class, PaymentInitiationSecurityProperties.class})
public class PaymentInitiationConfiguration {

    @Bean
    public Clock paymentClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PaymentSettings paymentSettings(PaymentInitiationIdempotencyProperties properties) {
        return new PaymentSettings(properties.getTtl());
    }

    @Bean
    public PaymentStatusPolicy paymentStatusPolicy() {
        return new PaymentStatusPolicy();
    }
}
