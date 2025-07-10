package com.loanmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.loanmanagement.customer",
    "com.loanmanagement.shared"
})
@EntityScan(basePackages = {
    "com.loanmanagement.customer.domain.model",
    "com.loanmanagement.shared.domain.model"
})
@EnableJpaRepositories(basePackages = "com.loanmanagement.customer.infrastructure.adapter.out.persistence")
@EnableFeignClients
public class CustomerServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}