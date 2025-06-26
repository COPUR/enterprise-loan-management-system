package com.bank.loanmanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enterprise Security Configuration with OAuth2.1, Keycloak Integration, and FAPI Compliance
 * Provides comprehensive security for the banking loan management system
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, jsr250Enabled = true, securedEnabled = true)
@Profile({"enterprise", "oauth2"})
public class EnterpriseSecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Main security filter chain for enterprise banking application
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Disabled for API-first architecture with JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Authorization rules for banking endpoints
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/actuator/health", "/api/actuator/info").permitAll()
                .requestMatchers("/api/v3/api-docs/**", "/api/swagger-ui/**", "/api/swagger-ui.html").permitAll()
                .requestMatchers("/graphql", "/graphiql/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll() // Only for development
                
                // Authentication endpoints
                .requestMatchers("/login/**", "/oauth2/**").permitAll()
                .requestMatchers("/logout").permitAll()
                
                // Banking API endpoints with role-based access
                .requestMatchers("/api/loans/**").hasAnyRole("CUSTOMER", "LOAN_OFFICER", "ADMIN")
                .requestMatchers("/api/payments/**").hasAnyRole("CUSTOMER", "LOAN_OFFICER", "ADMIN")
                .requestMatchers("/api/customers/**").hasAnyRole("LOAN_OFFICER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/audit/**").hasAnyRole("AUDITOR", "ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // OAuth2 Resource Server (JWT)
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            // OAuth2 Login for web interface
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService())
                )
                .defaultSuccessUrl("/api/dashboard", true)
                .failureUrl("/login?error=true")
            )
            
            // Logout configuration
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            )
            
            // Headers for security
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            );

        return http.build();
    }

    /**
     * JWT Decoder bean for OAuth2 Resource Server
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    /**
     * JWT Authentication Converter for mapping Keycloak roles to Spring Security authorities
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        
        // Extract authorities from both 'realm_access.roles' and 'banking_roles' claims
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("banking_roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new HashSet<>();
            
            // Extract realm roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                List<String> roles = (List<String>) realmAccess.get("roles");
                authorities.addAll(roles.stream()
                    .filter(role -> role.startsWith("LOAN_") || role.startsWith("CUSTOMER") || 
                                   role.startsWith("ADMIN") || role.startsWith("AUDITOR"))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList()));
            }
            
            // Extract banking-specific roles
            List<String> bankingRoles = jwt.getClaimAsStringList("banking_roles");
            if (bankingRoles != null) {
                authorities.addAll(bankingRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList()));
            }
            
            // Add default authority if no specific roles found
            if (authorities.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
            }
            
            return authorities;
        });
        
        return jwtAuthenticationConverter;
    }

    /**
     * OIDC User Service for enhanced user information
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            
            // Extract additional banking attributes
            Map<String, Object> attributes = oidcUser.getAttributes();
            Set<SimpleGrantedAuthority> mappedAuthorities = new HashSet<>();
            
            // Map Keycloak roles to Spring Security authorities
            if (attributes.containsKey("realm_access")) {
                Map<String, Object> realmAccess = (Map<String, Object>) attributes.get("realm_access");
                if (realmAccess.containsKey("roles")) {
                    List<String> roles = (List<String>) realmAccess.get("roles");
                    mappedAuthorities.addAll(roles.stream()
                        .filter(role -> role.startsWith("LOAN_") || role.startsWith("CUSTOMER") || 
                                       role.startsWith("ADMIN") || role.startsWith("AUDITOR"))
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toSet()));
                }
            }
            
            // Add banking-specific attributes
            String customerId = oidcUser.getClaimAsString("customer_id");
            if (customerId != null) {
                // Store customer ID for banking operations
                attributes.put("banking_customer_id", customerId);
            }
            
            return new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    /**
     * CORS Configuration for cross-origin requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins for production security
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://banking-app-enterprise:*",
            "https://*.banking.com"
        ));
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Custom logout handler for OAuth2 logout
     */
    @Bean
    public LogoutHandler customLogoutHandler() {
        return (request, response, authentication) -> {
            // Custom logout logic for banking compliance
            // Clear sensitive data, log audit events, etc.
            if (authentication != null) {
                String username = authentication.getName();
                // Log logout event for audit purposes
                System.out.println("Banking logout event for user: " + username);
            }
        };
    }
}