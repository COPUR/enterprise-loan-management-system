package com.bank.loanmanagement.api;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Test Configuration for SimpleLoanController tests
 * Only loads necessary components without excluded dependencies
 */
@SpringBootApplication
@Configuration
@ComponentScan(basePackages = {
    "com.bank.loanmanagement.api.controller",
    "com.bank.loanmanagement.api.config"
})
@EntityScan(basePackages = {
    // No JPA entities needed for controller tests
})
public class TestConfiguration {
    // Test configuration for isolated controller testing
}