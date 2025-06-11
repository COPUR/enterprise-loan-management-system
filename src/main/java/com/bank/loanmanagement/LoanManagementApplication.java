package com.bank.loanmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
@EnableAsync
public class LoanManagementApplication {
    
    public static void main(String[] args) {
        // Enable Virtual Threads for Java 21
        System.setProperty("spring.threads.virtual.enabled", "true");
        SpringApplication.run(LoanManagementApplication.class, args);
    }
}