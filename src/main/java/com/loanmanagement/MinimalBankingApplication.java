package com.loanmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Minimal Enterprise Banking Application
 * Simplified version for Docker deployment with core functionality
 */
@SpringBootApplication(scanBasePackages = {
    "com.loanmanagement.zerotrust", // Only include working Zero Trust Security module
    "com.loanmanagement.shared"     // Shared components
})
@EnableJpaRepositories(basePackages = "com.loanmanagement")
@EntityScan(basePackages = "com.loanmanagement")
@EnableTransactionManagement
@EnableAsync
@EnableCaching
public class MinimalBankingApplication {

    public static void main(String[] args) {
        // Enable Virtual Threads for Java 21
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        // Set JVM flags for banking optimizations
        System.setProperty("jdk.virtualThreadScheduler.parallelism", "10");
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "256");
        
        SpringApplication.run(MinimalBankingApplication.class, args);
    }
}