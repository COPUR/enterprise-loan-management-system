package com.bank.loan.loan.api.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.time.LocalDateTime;

/**
 * Health Check Controller for Business Requirements Validation
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().toString(),
            "service", "enterprise-loan-management",
            "version", "1.0.0"
        ));
    }
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        return ResponseEntity.ok(Map.of(
            "application", "Enterprise Loan Management System",
            "version", "1.0.0",
            "description", "Banking loan management system with FAPI compliance",
            "businessRequirements", "Orange Solution Case Study",
            "author", "Copur - AliCo",
            "contact", "copur@github.com"
        ));
    }
}