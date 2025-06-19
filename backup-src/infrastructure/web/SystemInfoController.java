package com.bank.loanmanagement.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

@RestController
public class SystemInfoController {
    
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Enterprise Loan Management System");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("timestamp", LocalDateTime.now());
        response.put("description", "Production-ready loan management with DDD and hexagonal architecture");
        
        response.put("technology_stack", Map.of(
            "java", "Java 21 with Virtual Threads",
            "framework", "Spring Boot 3.2",
            "database", "PostgreSQL (production) + H2 (development)",
            "messaging", "Apache Kafka with Spring Cloud Stream",
            "caching", "Redis (ElastiCache)",
            "security", "Spring Security with JWT",
            "monitoring", "Micrometer + Prometheus",
            "documentation", "OpenAPI 3.0 with Swagger UI",
            "testing", "JUnit 5 + TestContainers + ArchUnit",
            "resilience", "Resilience4j Circuit Breakers"
        ));
        
        response.put("features", new String[]{
            "Customer Management (DDD Bounded Context)",
            "Loan Origination (Business Rules & Calculations)", 
            "Payment Processing (Interest & Penalty Calculations)",
            "JWT Authentication & Authorization",
            "PostgreSQL Database with Flyway Migrations",
            "Event-Driven Architecture with Kafka",
            "Redis Caching Layer",
            "Circuit Breaker Patterns",
            "Comprehensive API Documentation"
        });
        
        response.put("architecture", Map.of(
            "pattern", "Hexagonal Architecture",
            "design", "Domain-Driven Design",
            "contexts", new String[]{"Customer Management", "Loan Origination", "Payment Processing"},
            "principles", new String[]{"Clean Architecture", "SOLID", "DRY", "KISS"}
        ));
        
        response.put("business_rules", Map.of(
            "installments", new int[]{6, 9, 12, 24},
            "interest_rates", "0.1% - 0.5% monthly",
            "max_loan_amount", 500000,
            "min_loan_amount", 1000,
            "currency", "USD"
        ));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("java_version", System.getProperty("java.version"));
        response.put("spring_boot_version", "3.2.0");
        response.put("virtual_threads_enabled", Runtime.version().feature() >= 21);
        return ResponseEntity.ok(response);
    }
}