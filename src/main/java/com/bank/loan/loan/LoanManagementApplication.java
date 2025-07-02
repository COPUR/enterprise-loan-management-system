package com.bank.loan.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {
    "com.bank.loan.loan"
})
@EntityScan(basePackages = {
    "com.bank.loan.loan.domain"
})
@EnableJpaRepositories(basePackages = {
    "com.bank.loan.loan.infrastructure"
})
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@EnableTransactionManagement
public class LoanManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanManagementApplication.class, args);
    }
}