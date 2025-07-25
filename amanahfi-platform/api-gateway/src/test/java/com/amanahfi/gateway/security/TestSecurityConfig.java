package com.amanahfi.gateway.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.config.Customizer;

/**
 * Test Security Configuration
 * 
 * Provides a simplified security configuration for testing that still includes
 * all required FAPI 2.0 headers but without requiring actual OAuth2 tokens.
 */
@TestConfiguration
@EnableWebFluxSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http
            // Disable CSRF and authentication for testing
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            
            // Include all required FAPI 2.0 security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                .contentTypeOptions(Customizer.withDefaults())
                .referrerPolicy(referrerPolicy -> referrerPolicy
                    .policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            
            // Allow all requests for testing
            .authorizeExchange(authz -> authz
                .anyExchange().permitAll()
            )
            
            .build();
    }
    
    @Bean
    @Primary
    public DPoPTokenValidator testDPoPTokenValidator() {
        return new DPoPTokenValidator() {
            public reactor.core.publisher.Mono<DPoPValidationResult> validateDPoPToken(
                    String dPoPToken, 
                    String accessToken,
                    org.springframework.web.server.ServerWebExchange exchange) {
                // Return validation failure for invalid tokens in tests
                if ("invalid-dpop-token".equals(dPoPToken)) {
                    return reactor.core.publisher.Mono.just(DPoPValidationResult.invalid("Invalid DPoP token"));
                }
                return reactor.core.publisher.Mono.just(DPoPValidationResult.valid());
            }
        };
    }
    
    @Bean
    @Primary
    public IslamicBankingSecurityFilter testIslamicBankingSecurityFilter() {
        return new IslamicBankingSecurityFilter() {
            // Simplified implementation for testing
        };
    }
}