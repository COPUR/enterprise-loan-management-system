package com.bank.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OAuth 2.1 with DPoP Security Configuration
 * 
 * Comprehensive security configuration for enterprise banking platform:
 * - OAuth 2.1 with Demonstrating Proof-of-Possession (DPoP)
 * - JWT token validation with custom claims
 * - Multi-factor authentication support
 * - Role-based access control (RBAC)
 * - Financial-grade API (FAPI) 2.0 compliance
 * - Advanced threat detection and prevention
 * - Audit logging and compliance tracking
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class OAuth2DPoPSecurityConfiguration {
    
    @Value("${banking.security.oauth2.issuer-uri}")
    private String issuerUri;
    
    @Value("${banking.security.oauth2.jwk-set-uri}")
    private String jwkSetUri;
    
    @Value("${banking.security.dpop.enabled:true}")
    private boolean dpopEnabled;
    
    @Value("${banking.security.mtls.enabled:true}")
    private boolean mtlsEnabled;
    
    @Value("${banking.security.fapi.enabled:true}")
    private boolean fapiEnabled;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Authentication endpoints
                .requestMatchers("/oauth/**", "/login", "/logout").permitAll()
                
                // API endpoints with role-based access
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/customers/**").hasAnyRole("CUSTOMER", "EMPLOYEE", "ADMIN")
                .requestMatchers("/api/v1/loans/**").hasAnyRole("LOAN_OFFICER", "EMPLOYEE", "ADMIN")
                .requestMatchers("/api/v1/payments/**").hasAnyRole("PAYMENT_PROCESSOR", "EMPLOYEE", "ADMIN")
                .requestMatchers("/api/v1/islamic/**").hasAnyRole("ISLAMIC_BANKING_OFFICER", "EMPLOYEE", "ADMIN")
                .requestMatchers("/api/v1/reports/**").hasAnyRole("ANALYST", "MANAGER", "ADMIN")
                
                // Default: all other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Add custom security filters
            .addFilterBefore(new DPoPValidationFilter(dpopEnabled), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new FAPISecurityFilter(fapiEnabled), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new ThreatDetectionFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new SecurityAuditFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // Configure exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                .accessDeniedHandler(new CustomAccessDeniedHandler())
            )
            
            // Add security headers
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                    .preload(true)
                )
                .and()
            )
            
            .build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        // Create JWT decoder with custom validation
        var decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        
        // Add custom JWT validation
        decoder.setJwtValidator(new CustomJwtValidator());
        
        return decoder;
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new BankingJwtGrantedAuthoritiesConverter());
        return converter;
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins for banking platform
        configuration.setAllowedOriginPatterns(List.of(
            "https://*.banking.example.com",
            "https://localhost:*",
            "http://localhost:*"
        ));
        
        // Allow specific methods
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // Allow specific headers
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-Request-ID",
            "X-Correlation-ID",
            "X-API-Version",
            "X-Idempotency-Key",
            "X-FAPI-Interaction-ID",
            "X-FAPI-Auth-Date",
            "X-FAPI-Customer-IP-Address",
            "DPoP"
        ));
        
        // Expose specific headers
        configuration.setExposedHeaders(List.of(
            "X-Request-ID",
            "X-Correlation-ID",
            "X-API-Version",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset",
            "X-Response-Time",
            "X-FAPI-Interaction-ID"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        source.registerCorsConfiguration("/oauth/**", configuration);
        
        return source;
    }
    
    /**
     * Custom JWT Granted Authorities Converter for Banking Roles
     */
    private static class BankingJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<SimpleGrantedAuthority>> {
        
        @Override
        public Collection<SimpleGrantedAuthority> convert(Jwt jwt) {
            // Extract roles from JWT claims
            Collection<String> roles = jwt.getClaimAsStringList("roles");
            Collection<String> scopes = jwt.getClaimAsStringList("scope");
            Collection<String> permissions = jwt.getClaimAsStringList("permissions");
            
            List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
            
            // Add role-based authorities
            if (roles != null) {
                authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList()));
            }
            
            // Add scope-based authorities
            if (scopes != null) {
                authorities.addAll(scopes.stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList()));
            }
            
            // Add permission-based authorities
            if (permissions != null) {
                authorities.addAll(permissions.stream()
                    .map(permission -> new SimpleGrantedAuthority("PERMISSION_" + permission))
                    .collect(Collectors.toList()));
            }
            
            return authorities;
        }
    }
}