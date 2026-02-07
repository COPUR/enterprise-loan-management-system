package com.bank.payment.infrastructure.fraud.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        factory.setConnectTimeout(timeout);
        factory.setConnectionRequestTimeout(timeout);
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
            ExternalFraudDetectionClient externalClient,
            Executor fraudExecutor) {
        return new EnhancedFraudDetectionService(externalClient, fraudExecutor);
    }

    @Bean(name = "fraudExecutor")
    public Executor fraudExecutor() {
        int size = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        return Executors.newFixedThreadPool(size);
    }
}
