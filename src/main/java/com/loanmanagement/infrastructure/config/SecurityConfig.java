// infrastructure/config/SecurityConfig.java
package com.loanmanagement.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Security configuration for the loan management system.
 * Follows 12-Factor App principles by externalizing configuration and
 * maintaining separation of concerns in line with Hexagonal Architecture.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final Environment environment;

    @Value("${security.admin.username:admin}")
    private String adminUsername;

    @Value("${security.admin.password:}")
    private String adminPassword;

    @Value("${security.customer.username:customer1}")
    private String customerUsername;

    @Value("${security.customer.password:}")
    private String customerPassword;

    @Value("${security.customer.id:1}")
    private String customerId;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Configures the security filter chain with stateless session management
     * and proper authorization rules following security best practices.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    /**
     * Provides user details service with externalized configuration.
     * In production, this should be replaced with a database-backed implementation.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        validateSecurityConfiguration();

        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();

        UserDetails customer = User.builder()
                .username(customerUsername)
                .password(passwordEncoder().encode(customerPassword))
                .roles("CUSTOMER")
                .authorities("CUSTOMER_ID_" + customerId)
                .build();

        return new InMemoryUserDetailsManager(admin, customer);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Validates that required security configuration is present.
     * Fails fast if critical security parameters are missing.
     */
    private void validateSecurityConfiguration() {
        if (adminPassword == null || adminPassword.isEmpty()) {
            throw new IllegalStateException(
                "Admin password must be configured via security.admin.password property"
            );
        }

        if (customerPassword == null || customerPassword.isEmpty()) {
            throw new IllegalStateException(
                "Customer password must be configured via security.customer.password property"
            );
        }

        if (adminPassword.length() < 8 || customerPassword.length() < 8) {
            throw new IllegalStateException(
                "Passwords must be at least 8 characters long"
            );
        }
    }
}