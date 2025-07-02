package com.bank.loanmanagement.loan;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
@EnableAutoConfiguration(exclude = {HibernateJpaAutoConfiguration.class})
@ComponentScan(basePackages = "com.bank.loanmanagement.loan", 
               excludeFilters = {
                   @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*infrastructure.*")
               })
public class TestConfiguration {
    // Minimal test configuration for regression tests
}