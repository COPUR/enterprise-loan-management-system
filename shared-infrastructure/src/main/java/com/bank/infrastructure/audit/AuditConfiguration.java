package com.bank.infrastructure.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Audit Configuration for Enterprise Banking System
 * 
 * Non-Functional Requirements:
 * - NFR-009: Audit Trail & Logging
 * - NFR-010: Compliance & Regulatory Reporting
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfiguration {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SecurityAuditorAware();
    }
    
    /**
     * Security-aware auditor provider that extracts the current user
     * from the security context for audit trail purposes
     */
    public static class SecurityAuditorAware implements AuditorAware<String> {
        
        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("SYSTEM");
            }
            
            String principal = authentication.getName();
            return Optional.ofNullable(principal);
        }
    }
}