package com.amanahfi.gateway.fallback;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker Patterns
 * 
 * Provides graceful degradation when microservices are unavailable
 * Maintains Islamic banking compliance even during service failures
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/customers")
    public Mono<ResponseEntity<Map<String, Object>>> customersFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("X-Islamic-Banking", "true")
            .header("X-Fallback-Response", "true")
            .body(Map.of(
                "error", "service_unavailable",
                "error_description", "Customer service is temporarily unavailable. Please try again later.",
                "service", "customers",
                "fallback", true,
                "timestamp", Instant.now(),
                "islamic_banking_compliant", true
            )));
    }

    @GetMapping("/accounts")
    public Mono<ResponseEntity<Map<String, Object>>> accountsFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("X-Islamic-Banking", "true")
            .header("X-Multi-Currency", "true")
            .header("X-Fallback-Response", "true")
            .body(Map.of(
                "error", "service_unavailable", 
                "error_description", "Account service is temporarily unavailable. Your funds remain secure.",
                "service", "accounts",
                "fallback", true,
                "timestamp", Instant.now(),
                "fund_security_message", "All account balances and transactions are protected during maintenance."
            )));
    }

    @GetMapping("/payments")
    @PostMapping("/payments") 
    public Mono<ResponseEntity<Map<String, Object>>> paymentsFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("X-Islamic-Banking", "true")
            .header("X-CBDC-Settlement", "true")
            .header("X-Fallback-Response", "true")
            .body(Map.of(
                "error", "service_unavailable",
                "error_description", "Payment service is temporarily unavailable. No charges have been processed.",
                "service", "payments",
                "fallback", true,
                "timestamp", Instant.now(),
                "payment_safety_message", "No Islamic banking transactions were processed during this request.",
                "cbdc_status", "CBDC settlement system is protected during maintenance."
            )));
    }

    @GetMapping("/murabaha")
    @PostMapping("/murabaha")
    public Mono<ResponseEntity<Map<String, Object>>> murabahaFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("X-Islamic-Banking", "true")
            .header("X-Sharia-Compliant", "true")
            .header("X-Fallback-Response", "true")
            .body(Map.of(
                "error", "service_unavailable",
                "error_description", "Islamic finance service is temporarily unavailable.",
                "service", "murabaha",
                "fallback", true,
                "timestamp", Instant.now(),
                "sharia_compliance_message", "All Islamic finance contracts remain Sharia-compliant.",
                "alternative_contact", "Please contact our Islamic banking advisors at +971-4-XXX-XXXX"
            )));
    }

    @GetMapping("/compliance")
    public Mono<ResponseEntity<Map<String, Object>>> complianceFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("X-Islamic-Banking", "true")
            .header("X-Regulatory-Compliance", "CBUAE,VARA")
            .header("X-Fallback-Response", "true")
            .body(Map.of(
                "error", "service_unavailable",
                "error_description", "Compliance service is temporarily unavailable.",
                "service", "compliance", 
                "fallback", true,
                "timestamp", Instant.now(),
                "compliance_message", "All regulatory requirements continue to be met during maintenance.",
                "escalation_contact", "For urgent compliance matters, contact compliance@amanahfi.ae"
            )));
    }

    @GetMapping("/admin")
    @PostMapping("/admin")
    public Mono<ResponseEntity<Map<String, Object>>> adminFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("X-Islamic-Banking", "true")
            .header("X-Enhanced-Security", "required")
            .header("X-Fallback-Response", "true")
            .body(Map.of(
                "error", "service_unavailable",
                "error_description", "Administrative service is temporarily unavailable.",
                "service", "admin",
                "fallback", true,
                "timestamp", Instant.now(),
                "security_message", "All administrative functions are secured during maintenance.",
                "emergency_contact", "For urgent administrative needs, contact admin@amanahfi.ae"
            )));
    }

    @GetMapping("/high-value")
    @PostMapping("/high-value")
    public Mono<ResponseEntity<Map<String, Object>>> highValueFallback() {
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("X-Islamic-Banking", "true")
            .header("X-High-Value", "true")
            .header("X-Enhanced-Monitoring", "true")
            .header("X-Fallback-Response", "true")
            .body(Map.of(
                "error", "service_unavailable",
                "error_description", "High-value transaction service is temporarily unavailable.",
                "service", "high-value-payments",
                "fallback", true,
                "timestamp", Instant.now(),
                "high_value_message", "High-value transactions are protected with enhanced security.",
                "alternative_process", "Please contact your relationship manager for high-value transactions.",
                "security_note", "No high-value transactions were processed during this request."
            )));
    }
}