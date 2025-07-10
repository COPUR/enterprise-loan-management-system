package com.loanmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.loanmanagement.payment",
    "com.loanmanagement.shared"
})
@EntityScan(basePackages = {
    "com.loanmanagement.payment.domain.model",
    "com.loanmanagement.shared.domain.model"
})
@EnableJpaRepositories(basePackages = "com.loanmanagement.payment.infrastructure.adapter.out.persistence")
@EnableFeignClients
public class PaymentServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}