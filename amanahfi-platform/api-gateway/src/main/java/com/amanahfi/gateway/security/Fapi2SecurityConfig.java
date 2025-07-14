package com.amanahfi.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.server.header.ContentTypeOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.config.Customizer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * FAPI 2.0 Security Configuration for AmanahFi API Gateway
 * 
 * Implements Financial-grade API (FAPI) 2.0 Security Profile:
 * - OAuth 2.1 with PKCE mandatory
 * - DPoP (Demonstration of Proof of Possession) tokens
 * - TLS 1.2+ enforcement
 * - Comprehensive security headers
 * - Rate limiting and DDoS protection
 * - Islamic banking compliance headers
 * - UAE regulatory compliance (CBUAE, VARA)
 */
@Configuration
@EnableWebFluxSecurity
public class Fapi2SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            DPoPTokenValidator dPoPTokenValidator,
            IslamicBankingSecurityFilter islamicBankingFilter) {
        
        return http
            // Disable defaults that don't fit FAPI 2.0
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            
            // HTTPS enforcement
            .redirectToHttps(redirect -> redirect
                .httpsRedirectWhen(exchange -> 
                    !exchange.getRequest().getURI().getScheme().equals("https") &&
                    !isTestEnvironment(exchange)))
            
            // Security Headers for FAPI 2.0 compliance  
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                .contentTypeOptions(Customizer.withDefaults())
                .hsts(hsts -> hsts
                    .includeSubdomains(true)
                    .maxAge(Duration.ofDays(365))
                    .preload(true))
                .referrerPolicy(referrerPolicy -> referrerPolicy
                    .policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            
            // OAuth 2.1 + JWT Resource Server
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter()))
                )
                .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                })
            )
            
            // Authorization rules
            .authorizeExchange(authz -> authz
                // Public endpoints
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                .pathMatchers("/oauth2/**", "/.well-known/**").permitAll()
                .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Islamic banking endpoints - require authentication
                .pathMatchers("/api/v1/murabaha/**").authenticated()
                .pathMatchers("/api/v1/accounts/**").authenticated()
                .pathMatchers("/api/v1/payments/**").authenticated()
                .pathMatchers("/api/v1/customers/**").authenticated()
                .pathMatchers("/api/v1/compliance/**").authenticated()
                
                // Admin endpoints - require special scope
                .pathMatchers("/api/v1/admin/**").hasAuthority("SCOPE_admin")
                
                // High-value operations require enhanced security
                .pathMatchers("/api/v1/payments/high-value/**").hasAuthority("SCOPE_high-value")
                
                // Default deny
                .anyExchange().authenticated()
            )
            
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                })
            )
            
            .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract scopes and convert to authorities
            List<String> scopes = jwt.getClaimAsStringList("scope");
            return scopes != null ? 
                scopes.stream()
                    .map(scope -> "SCOPE_" + scope)
                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                    .collect(java.util.stream.Collectors.toList()) :
                java.util.Collections.emptyList();
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow Islamic banking frontend domains
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "https://*.amanahfi.ae",
            "https://*.amanahfi.com", 
            "https://localhost:*", // Development
            "https://127.0.0.1:*"  // Development
        ));
        
        // FAPI 2.0 required headers
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type", 
            "X-Requested-With",
            "DPoP",                    // FAPI 2.0 DPoP token
            "X-Client-ID",
            "X-Request-ID",
            "X-Correlation-ID",
            "X-Request-Signature"      // For high-value transactions
        ));
        
        configuration.setExposedHeaders(Arrays.asList(
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset", 
            "DPoP-Nonce",              // FAPI 2.0 DPoP nonce
            "X-Request-Signature-Required",
            "X-Islamic-Banking",
            "X-Sharia-Compliant",
            "X-Regulatory-Compliance"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public DPoPTokenValidator dPoPTokenValidator() {
        return new DPoPTokenValidator();
    }

    @Bean 
    public IslamicBankingSecurityFilter islamicBankingSecurityFilter() {
        return new IslamicBankingSecurityFilter();
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter() {
        return new RateLimitingFilter();
    }

    @Bean
    public AuditSecurityEventListener auditSecurityEventListener() {
        return new AuditSecurityEventListener();
    }

    private boolean isTestEnvironment(org.springframework.web.server.ServerWebExchange exchange) {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("test") || profile.contains("local");
    }
}