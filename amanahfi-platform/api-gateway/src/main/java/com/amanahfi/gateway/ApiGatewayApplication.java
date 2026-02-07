package com.amanahfi.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;

/**
 * AmanahFi API Gateway Application
 * 
 * FAPI 2.0 compliant API Gateway for Islamic Finance Platform
 * 
 * Features:
 * - OAuth 2.1 + PKCE authentication
 * - DPoP token binding (RFC 9449)
 * - Rate limiting and security headers
 * - Islamic banking compliance
 * - Circuit breaker patterns
 * - Request/response transformation
 * - Audit logging for compliance
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    /**
     * Route configuration for microservices
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            
            // Customer Onboarding Service
            .route("onboarding-service", r -> r
                .path("/api/v1/customers/**")
                .filters(f -> f
                    .circuitBreaker(config -> config
                        .setName("onboarding-cb")
                        .setFallbackUri("forward:/fallback/customers"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setMethods(org.springframework.http.HttpMethod.GET)
                        .setBackoff(Duration.ofSeconds(1), Duration.ofSeconds(5), 2, false))
                    .addRequestHeader("X-Gateway", "AmanahFi-Gateway")
                    .addRequestHeader("X-Islamic-Banking", "true")
                    .removeRequestHeader("X-Internal-Secret"))
                .uri("http://onboarding-service:8081"))

            // Account Management Service  
            .route("accounts-service", r -> r
                .path("/api/v1/accounts/**")
                .filters(f -> f
                    .circuitBreaker(config -> config
                        .setName("accounts-cb")
                        .setFallbackUri("forward:/fallback/accounts"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setMethods(org.springframework.http.HttpMethod.GET))
                    .addRequestHeader("X-Gateway", "AmanahFi-Gateway")
                    .addRequestHeader("X-Multi-Currency", "true")
                    .addRequestHeader("X-CBDC-Enabled", "true"))
                .uri("http://accounts-service:8082"))

            // Payment Processing Service
            .route("payments-service", r -> r
                .path("/api/v1/payments/**")
                .filters(f -> f
                    .circuitBreaker(config -> config
                        .setName("payments-cb")
                        .setFallbackUri("forward:/fallback/payments"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(2) // Lower retries for payments
                        .setMethods(org.springframework.http.HttpMethod.GET))
                    .addRequestHeader("X-Gateway", "AmanahFi-Gateway")
                    .addRequestHeader("X-CBDC-Settlement", "true")
                    .addRequestHeader("X-Islamic-Payment", "true"))
                .uri("http://payments-service:8083"))

            // Murabaha Contract Service
            .route("murabaha-service", r -> r
                .path("/api/v1/murabaha/**")
                .filters(f -> f
                    .circuitBreaker(config -> config
                        .setName("murabaha-cb")
                        .setFallbackUri("forward:/fallback/murabaha"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setMethods(org.springframework.http.HttpMethod.GET))
                    .addRequestHeader("X-Gateway", "AmanahFi-Gateway")
                    .addRequestHeader("X-Islamic-Finance", "true")
                    .addRequestHeader("X-Sharia-Compliant", "true"))
                .uri("http://murabaha-service:8084"))

            // Compliance Service
            .route("compliance-service", r -> r
                .path("/api/v1/compliance/**")
                .filters(f -> f
                    .circuitBreaker(config -> config
                        .setName("compliance-cb")
                        .setFallbackUri("forward:/fallback/compliance"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setMethods(org.springframework.http.HttpMethod.GET))
                    .addRequestHeader("X-Gateway", "AmanahFi-Gateway")
                    .addRequestHeader("X-AML-Enabled", "true")
                    .addRequestHeader("X-Regulatory-Compliance", "CBUAE,VARA"))
                .uri("http://compliance-service:8085"))

            // OAuth 2.1 Authorization Server
            .route("oauth-server", r -> r
                .path("/oauth2/**", "/.well-known/**")
                .filters(f -> f
                    .addRequestHeader("X-Gateway", "AmanahFi-Gateway")
                    .addRequestHeader("X-FAPI-Compliant", "true"))
                .uri("http://oauth-server:8086"))

            // Admin endpoints with enhanced security
            .route("admin-service", r -> r
                .path("/api/v1/admin/**")
                .filters(f -> f
                    .circuitBreaker(config -> config
                        .setName("admin-cb")
                        .setFallbackUri("forward:/fallback/admin"))
                    .addRequestHeader("X-Gateway", "AmanahFi-Gateway")
                    .addRequestHeader("X-Admin-Access", "true")
                    .addRequestHeader("X-Enhanced-Security", "required"))
                .uri("http://admin-service:8087"))

            // High-value transaction routing with special handling
            .route("high-value-payments", r -> r
                .path("/api/v1/payments/high-value/**")
                .filters(f -> f
                    .circuitBreaker(config -> config
                        .setName("high-value-cb")
                        .setFallbackUri("forward:/fallback/high-value"))
                    .addRequestHeader("X-Gateway", "AmanahFi-Gateway")
                    .addRequestHeader("X-High-Value", "true")
                    .addRequestHeader("X-Enhanced-Monitoring", "true")
                    .addRequestHeader("X-Signature-Required", "true"))
                .uri("http://payments-service:8083"))

            .build();
    }

    /**
     * Global CORS configuration for Islamic banking frontend
     */
    @Bean
    @Profile("!test & !local")
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Islamic banking platform domains
        corsConfig.setAllowedOriginPatterns(Arrays.asList(
            "https://*.amanahfi.ae",
            "https://*.amanahfi.com",
            "https://islamic-banking.ae",
            "https://localhost:*",
            "https://127.0.0.1:*"
        ));
        
        corsConfig.setMaxAge(Duration.ofHours(1));
        corsConfig.setAllowCredentials(true);
        
        corsConfig.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        corsConfig.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With", 
            "X-Client-ID",
            "X-Request-ID",
            "X-Correlation-ID",
            "DPoP",                    // FAPI 2.0 DPoP token
            "X-Request-Signature"      // High-value transaction signing
        ));
        
        corsConfig.setExposedHeaders(Arrays.asList(
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset",
            "DPoP-Nonce",
            "X-Request-Signature-Required",
            "X-Islamic-Banking",
            "X-Sharia-Compliant"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}
