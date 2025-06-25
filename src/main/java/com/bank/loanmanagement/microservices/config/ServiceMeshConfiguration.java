package com.bank.loanmanagement.microservices.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import java.time.Duration;

@Configuration
@Profile("istio")
public class ServiceMeshConfiguration {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(60))
            .requestFactory(() -> {
                HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
                factory.setConnectTimeout(30000);
                return factory;
            })
            .build();
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .meterFilter(MeterFilter.deny(id -> {
                String uri = id.getTag("uri");
                return uri != null && (uri.startsWith("/actuator") || uri.startsWith("/health"));
            }))
            .commonTags(
                "service", "${spring.application.name:unknown}",
                "environment", "${spring.profiles.active:default}",
                "version", "${app.version:1.0.0}",
                "istio", "enabled"
            );
    }

    @Bean
    public ServiceMeshHealthIndicator serviceMeshHealthIndicator() {
        return new ServiceMeshHealthIndicator();
    }
}