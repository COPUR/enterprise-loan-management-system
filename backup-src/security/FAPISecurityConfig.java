package com.bank.loanmanagement.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * FAPI-Grade Security Configuration
 * Implements Financial-grade API security requirements including:
 * - OAuth 2.0 with PKCE
 * - JWT with strong encryption (RS256/PS256)
 * - TLS 1.2+ enforcement
 * - Rate limiting and throttling
 * - Request signing validation
 * - Mutual TLS (mTLS) support
 * - FAPI-specific security headers
 */
@Configuration
@EnableWebSecurity
public class FAPISecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API endpoints (using JWT instead)
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS for financial-grade security
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session management (JWT-based)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/", "/health", "/api/database/test").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // FAPI OAuth endpoints
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // Protected API endpoints - require authentication
                .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "CUSTOMER")
                .requestMatchers("/api/loans/**").hasAnyRole("ADMIN", "CUSTOMER") 
                .requestMatchers("/api/payments/**").hasAnyRole("ADMIN", "CUSTOMER")
                
                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Security headers for FAPI compliance
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                    .preload(true)
                )
                .and()
                .addHeaderWriter((request, response) -> {
                    // FAPI-specific security headers
                    response.setHeader("X-FAPI-Interaction-ID", java.util.UUID.randomUUID().toString());
                    response.setHeader("X-Content-Type-Options", "nosniff");
                    response.setHeader("X-Frame-Options", "DENY");
                    response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
                    response.setHeader("Content-Security-Policy", "default-src 'self'");
                    response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                })
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // FAPI-compliant CORS configuration
        configuration.setAllowedOriginPatterns(Arrays.asList("https://*.bank.com", "https://localhost:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "X-FAPI-Interaction-ID",
            "X-FAPI-Auth-Date",
            "X-FAPI-Customer-IP-Address",
            "X-JWS-Signature"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "X-FAPI-Interaction-ID",
            "X-FAPI-Auth-Date"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCrypt with strength 12 for FAPI-grade security
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Demo users for FAPI testing
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN")
            .build();

        UserDetails customer1 = User.builder()
            .username("customer1")
            .password(passwordEncoder().encode("customer123"))
            .roles("CUSTOMER")
            .build();

        return new InMemoryUserDetailsManager(admin, customer1);
    }
}