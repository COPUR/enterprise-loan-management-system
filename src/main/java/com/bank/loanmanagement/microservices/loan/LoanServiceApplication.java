package com.bank.loanmanagement.microservices.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
    "com.bank.loanmanagement.domain.loan.repository",
    "com.bank.loanmanagement.infrastructure.repository"
})
@EntityScan(basePackages = {
    "com.bank.loanmanagement.domain.loan",
    "com.bank.loanmanagement.domain.installment",
    "com.bank.loanmanagement.sharedkernel.domain"
})
@ComponentScan(basePackages = {
    "com.bank.loanmanagement.application",
    "com.bank.loanmanagement.domain.loan",
    "com.bank.loanmanagement.domain.installment",
    "com.bank.loanmanagement.infrastructure.web",
    "com.bank.loanmanagement.sharedkernel",
    "com.bank.loanmanagement.security",
    "com.bank.loanmanagement.messaging",
    "com.bank.loanmanagement.saga",
    "com.bank.loanmanagement.monitoring"
})
@EnableTransactionManagement
@EnableCaching
@EnableKafka
@EnableAsync
@EnableFeignClients
public class LoanServiceApplication {

    public static void main(String[] args) {
        System.setProperty("spring.application.name", "loan-service");
        System.setProperty("server.port", "8082");
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}