package com.bank.monitoring.health;

import org.springframework.boot.actuator.health.CompositeHealthContributor;
import org.springframework.boot.actuator.health.HealthContributor;
import org.springframework.boot.actuator.health.NamedContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Banking Health Configuration
 * Configures custom banking health indicators and composite health contributors
 */
@Configuration
public class BankingHealthConfiguration {

    @Bean
    public CompositeHealthContributor bankingHealthContributor(
            LoanProcessingHealthIndicator loanProcessingHealthIndicator,
            PaymentSystemHealthIndicator paymentSystemHealthIndicator,
            ComplianceServiceHealthIndicator complianceServiceHealthIndicator,
            FraudDetectionHealthIndicator fraudDetectionHealthIndicator,
            CustomerServiceHealthIndicator customerServiceHealthIndicator) {
        
        Map<String, HealthContributor> contributors = new LinkedHashMap<>();
        
        contributors.put("loanProcessing", loanProcessingHealthIndicator);
        contributors.put("paymentSystem", paymentSystemHealthIndicator);
        contributors.put("complianceService", complianceServiceHealthIndicator);
        contributors.put("fraudDetection", fraudDetectionHealthIndicator);
        contributors.put("customerService", customerServiceHealthIndicator);
        
        return CompositeHealthContributor.fromMap(contributors);
    }
    
    @Bean
    public CompositeHealthContributor coreServicesHealthContributor(
            LoanProcessingHealthIndicator loanProcessingHealthIndicator,
            PaymentSystemHealthIndicator paymentSystemHealthIndicator) {
        
        Map<String, HealthContributor> coreContributors = new LinkedHashMap<>();
        coreContributors.put("loans", loanProcessingHealthIndicator);
        coreContributors.put("payments", paymentSystemHealthIndicator);
        
        return CompositeHealthContributor.fromMap(coreContributors);
    }
    
    @Bean
    public CompositeHealthContributor securityServicesHealthContributor(
            ComplianceServiceHealthIndicator complianceServiceHealthIndicator,
            FraudDetectionHealthIndicator fraudDetectionHealthIndicator) {
        
        Map<String, HealthContributor> securityContributors = new LinkedHashMap<>();
        securityContributors.put("compliance", complianceServiceHealthIndicator);
        securityContributors.put("fraud", fraudDetectionHealthIndicator);
        
        return CompositeHealthContributor.fromMap(securityContributors);
    }
}