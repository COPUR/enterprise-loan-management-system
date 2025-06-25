package com.bank.loanmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {
    "com.bank.loanmanagement"
})
@EntityScan(basePackages = {
    "com.bank.loanmanagement.domain"
})
@EnableJpaRepositories(basePackages = {
    "com.bank.loanmanagement.infrastructure"
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