package com.bank.loan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main Application Configuration
 * 
 * Configures component scanning and module imports to avoid circular dependencies.
 * Uses clean separation between domain models and persistence entities.
 */
@Configuration
@ComponentScan(basePackages = {
    "com.bank.loan.domain.service",
    "com.bank.loan.infrastructure.persistence"
})
@EntityScan(basePackages = {
    "com.bank.loan.infrastructure.persistence"
})
@EnableJpaRepositories(basePackages = {
    "com.bank.loan.infrastructure.persistence"
})
@Import({
    DatabaseConfiguration.class,
    SecurityConfiguration.class
})
public class ApplicationConfiguration {
    
}