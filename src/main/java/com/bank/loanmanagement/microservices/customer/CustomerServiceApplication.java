package com.bank.loanmanagement.microservices.customer;

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
    "com.bank.loanmanagement.customermanagement.infrastructure.repository",
    "com.bank.loanmanagement.infrastructure.repository"
})
@EntityScan(basePackages = {
    "com.bank.loanmanagement.customermanagement.domain",
    "com.bank.loanmanagement.sharedkernel.domain"
})
@ComponentScan(basePackages = {
    "com.bank.loanmanagement.customermanagement",
    "com.bank.loanmanagement.sharedkernel",
    "com.bank.loanmanagement.security",
    "com.bank.loanmanagement.messaging",
    "com.bank.loanmanagement.monitoring"
})
@EnableTransactionManagement
@EnableCaching
@EnableKafka
@EnableFeignClients
public class CustomerServiceApplication {

    public static void main(String[] args) {
        System.setProperty("spring.application.name", "customer-service");
        System.setProperty("server.port", "8081");
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}