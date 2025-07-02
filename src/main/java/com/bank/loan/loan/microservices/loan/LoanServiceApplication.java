package com.bank.loanmanagement.loan.microservices.loan;

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
    "com.bank.loanmanagement.loan.domain.loan.repository",
    "com.bank.loanmanagement.loan.infrastructure.repository"
})
@EntityScan(basePackages = {
    "com.bank.loanmanagement.loan.domain.loan",
    "com.bank.loanmanagement.loan.domain.installment",
    "com.bank.loanmanagement.loan.sharedkernel.domain"
})
@ComponentScan(basePackages = {
    "com.bank.loanmanagement.loan.application",
    "com.bank.loanmanagement.loan.domain.loan",
    "com.bank.loanmanagement.loan.domain.installment",
    "com.bank.loanmanagement.loan.infrastructure.web",
    "com.bank.loanmanagement.loan.sharedkernel",
    "com.bank.loanmanagement.loan.security",
    "com.bank.loanmanagement.loan.messaging",
    "com.bank.loanmanagement.loan.saga",
    "com.bank.loanmanagement.loan.monitoring"
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