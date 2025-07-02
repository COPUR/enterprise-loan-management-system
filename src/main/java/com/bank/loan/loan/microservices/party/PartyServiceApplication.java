package com.bank.loanmanagement.loan.microservices.party;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
    "com.bank.customer.infrastructure.repository",
    "com.bank.loanmanagement.loan.infrastructure.repository"
})
@EntityScan(basePackages = {
    "com.bank.customer.domain",
    "com.bank.loanmanagement.loan.sharedkernel.domain"
})
@ComponentScan(basePackages = {
    "com.bank.customer.application.service",
    "com.bank.customer.infrastructure.adapter",
    "com.bank.loanmanagement.loan.sharedkernel",
    "com.bank.loanmanagement.loan.security",
    "com.bank.loanmanagement.loan.messaging",
    "com.bank.loanmanagement.loan.monitoring"
})
@EnableTransactionManagement
@EnableCaching
@EnableKafka
@EnableFeignClients
public class PartyServiceApplication {

    public static void main(String[] args) {
        System.setProperty("spring.application.name", "party-service");
        System.setProperty("server.port", "8084");
        SpringApplication.run(PartyServiceApplication.class, args);
    }
}