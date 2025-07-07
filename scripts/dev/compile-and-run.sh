#!/bin/bash

# Set Java environment
export JAVA_HOME="/nix/store/$(ls /nix/store | grep -E 'openjdk.*21' | head -1)"
export PATH="$JAVA_HOME/bin:$PATH"

# Create a minimal Spring Boot application
cat > src/main/java/com/bank/loanmanagement/LoanManagementApplication.java << 'EOF'
package com.bank.loanmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class LoanManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanManagementApplication.class, args);
    }
}

@RestController
class HomeController {
    @GetMapping("/")
    public String home() {
        return "Loan Management System is running!";
    }
    
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
EOF

# Download Spring Boot CLI for quick setup
curl -L -o spring-boot-cli.tar.gz https://repo.spring.io/release/org/springframework/boot/spring-boot-cli/3.2.0/spring-boot-cli-3.2.0-bin.tar.gz
tar -xzf spring-boot-cli.tar.gz
export PATH="$(pwd)/spring-3.2.0/bin:$PATH"

# Run the application
spring run src/main/java/com/bank/loanmanagement/LoanManagementApplication.java --server.port=5000