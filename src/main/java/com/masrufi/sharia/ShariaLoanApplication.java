package com.masrufi.sharia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MasruFi Sharia Platform - Main Application
 * Halal Crypto Finance with Blockchain Innovation
 * 
 * Features:
 * - Variable loan terms: Hourly to 10 years
 * - Sharia-compliant financing models
 * - Multi-chain cryptocurrency support
 * - Automated payment management
 * - Real-time compliance monitoring
 * 
 * @author Ali&Co - MasruFi Framework Team
 * @version 1.0.0
 * @since 2024
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = {
    "com.masrufi.sharia.repository",
    "com.masrufi.sharia.compliance.repository",
    "com.masrufi.sharia.payment.repository"
})
@ComponentScan(basePackages = {
    "com.masrufi.sharia",
    "com.masrufi.framework.shared",
    "com.masrufi.framework.security"
})
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
public class ShariaLoanApplication {

    public static void main(String[] args) {
        System.out.println("🌙 Starting MasruFi Sharia Platform...");
        System.out.println("📋 Halal Crypto Finance with Blockchain Innovation");
        System.out.println("🏦 Serving Global Islamic Finance Community");
        
        SpringApplication.run(ShariaLoanApplication.class, args);
        
        System.out.println("✅ MasruFi Sharia Platform Started Successfully!");
        System.out.println("🔗 API Documentation: http://localhost:8080/swagger-ui.html");
        System.out.println("📊 Sharia Dashboard: http://localhost:8080/sharia-dashboard");
    }
}