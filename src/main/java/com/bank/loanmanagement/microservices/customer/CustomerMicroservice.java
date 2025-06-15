package com.bank.loanmanagement.microservices.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.bank.loanmanagement.customermanagement",
    "com.bank.loanmanagement.shared"
})
@EntityScan(basePackages = "com.bank.loanmanagement.domain.model")
@EnableJpaRepositories(basePackages = "com.bank.loanmanagement.infrastructure.adapter.out.persistence")
public class CustomerMicroservice {

    public static void main(String[] args) {
        System.setProperty("server.port", "8081");
        System.setProperty("spring.application.name", "customer-service");
        SpringApplication.run(CustomerMicroservice.class, args);
    }
}