package com.bank.payment.infrastructure.fraud.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;

/**
 * Configuration for external fraud detection service integrations
 */
@Configuration
public class FraudDetectionConfiguration {

    @Value("${fraud.detection.timeout.seconds:10}")
    private int timeoutSeconds;

    @Value("${fraud.detection.max.connections:50}")
    private int maxConnections;

    @Value("${fraud.detection.max.connections.per.route:10}")
    private int maxConnectionsPerRoute;

    /**
     * RestTemplate configured for fraud detection service calls
     */
    @Bean("fraudDetectionRestTemplate")
    public RestTemplate fraudDetectionRestTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
            .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
            .requestFactory(this::clientHttpRequestFactory)
            .build();
    }

    /**
     * HTTP client factory with connection pooling for fraud services
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutSeconds * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return factory;
    }

    /**
     * External fraud detection client bean
     */
    @Bean
    public ExternalFraudDetectionClient externalFraudDetectionClient(
            RestTemplate fraudDetectionRestTemplate) {
        return new ExternalFraudDetectionClient(fraudDetectionRestTemplate);
    }

    /**
     * Enhanced fraud detection service that integrates external services
     */
    @Bean
    public EnhancedFraudDetectionService enhancedFraudDetectionService(
            ExternalFraudDetectionClient externalClient) {
        return new EnhancedFraudDetectionService(externalClient);
    }
}