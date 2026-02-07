package com.amanahfi.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.server.header.ContentTypeOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.config.Customizer;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.net.URI;
import java.nio.charset.StandardCharsets;

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
            Environment environment) {

        RateLimitingFilter rateLimitingFilter = new RateLimitingFilter();
        IslamicBankingSecurityFilter islamicBankingFilter = new IslamicBankingSecurityFilter();
        DPoPValidationFilter dPoPValidationFilter = new DPoPValidationFilter(dPoPTokenValidator);
        PkceValidationFilter pkceValidationFilter = new PkceValidationFilter();
        
        ServerHttpSecurity httpBuilder = http
            // Disable defaults that don't fit FAPI 2.0
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        // HTTPS enforcement (skip in test/local to avoid scheme-less requests)
        if (!environment.acceptsProfiles(Profiles.of("test", "local"))) {
            httpBuilder = httpBuilder.redirectToHttps(redirect -> redirect
                .httpsRedirectWhen(exchange -> {
                    String scheme = exchange.getRequest().getURI().getScheme();
                    boolean isHttps = "https".equalsIgnoreCase(scheme);
                    return !isHttps;
                }));
        }

        return httpBuilder
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
                    .jwtDecoder(jwtDecoder(environment))
                )
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                })
            )
            
            // Authorization rules
            .authorizeExchange(authz -> authz
                // Preflight requests
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public endpoints
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                .pathMatchers("/actuator/auditevents").permitAll()
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
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler((exchange, denied) -> {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                })
            )
            
            // Custom filters for FAPI compliance
            .addFilterAt(pkceValidationFilter, SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(rateLimitingFilter, SecurityWebFiltersOrder.FIRST)
            .addFilterAfter(islamicBankingFilter, SecurityWebFiltersOrder.FIRST)
            .addFilterAt(dPoPValidationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterBefore(corsSchemeNormalizationFilter(environment), SecurityWebFiltersOrder.CORS)
            
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
            "https://amanahfi.ae",
            "https://amanahfi.com",
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
    public AuditEventRepository auditEventRepository() {
        return new InMemoryAuditEventRepository();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(Environment environment) {
        if (environment.acceptsProfiles(Profiles.of("test", "local"))) {
            return token -> Mono.error(new OAuth2AuthenticationException(
                new OAuth2Error("invalid_token", "Invalid access token", null)));
        }

        return ReactiveJwtDecoders.fromIssuerLocation("http://localhost:8086/oauth2");
    }

    private WebFilter corsSchemeNormalizationFilter(Environment environment) {
        return (exchange, chain) -> {
            if (!environment.acceptsProfiles(Profiles.of("test", "local"))) {
                return chain.filter(exchange);
            }

            String origin = exchange.getRequest().getHeaders().getOrigin();
            if (origin == null) {
                return chain.filter(exchange);
            }

            if (exchange.getRequest().getURI().getScheme() != null) {
                return chain.filter(exchange);
            }

            URI requestUri = exchange.getRequest().getURI();
            String rawPath = requestUri.getRawPath() != null ? requestUri.getRawPath() : "/";
            String rawQuery = requestUri.getRawQuery();

            StringBuilder uriBuilder = new StringBuilder()
                .append("https://localhost");
            uriBuilder.append(rawPath);
            if (rawQuery != null && !rawQuery.isBlank()) {
                uriBuilder.append("?").append(rawQuery);
            }

            URI normalized = URI.create(uriBuilder.toString());

            return chain.filter(new org.springframework.web.server.ServerWebExchangeDecorator(exchange) {
                @Override
                public org.springframework.http.server.reactive.ServerHttpRequest getRequest() {
                    return new org.springframework.http.server.reactive.ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public java.net.URI getURI() {
                            return normalized;
                        }
                    };
                }
            });
        };
    }

    private ServerAuthenticationEntryPoint authenticationEntryPoint() {
        return (exchange, ex) -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String errorMessage = ex != null && ex.getMessage() != null ? ex.getMessage() : "Unauthorized";
            String errorCode = "unauthorized";

            if (ex instanceof OAuth2AuthenticationException oauth2Exception) {
                if (oauth2Exception.getError() != null) {
                    errorCode = oauth2Exception.getError().getErrorCode();
                    if (oauth2Exception.getError().getDescription() != null) {
                        errorMessage = oauth2Exception.getError().getDescription();
                    }
                }
            }
            String body = """
                {
                    "error": "%s",
                    "error_description": "%s",
                    "timestamp": "%s"
                }
                """.formatted(errorCode, errorMessage.replace("\"", "'"), Instant.now().toString());

            DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
            return exchange.getResponse().writeWith(Mono.just(buffer));
        };
    }

}
