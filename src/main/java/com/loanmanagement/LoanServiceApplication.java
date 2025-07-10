package com.loanmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.loanmanagement.loan",
    "com.loanmanagement.shared"
})
@EntityScan(basePackages = {
    "com.loanmanagement.loan.domain.model",
    "com.loanmanagement.shared.domain.model"
})
@EnableJpaRepositories(basePackages = "com.loanmanagement.loan.infrastructure.adapter.out.persistence")
@EnableFeignClients
public class LoanServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}