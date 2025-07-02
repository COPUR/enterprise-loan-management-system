package com.bank.loan.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootApplication
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}

@RestController
@RequestMapping("/api/v1")
class TestBankingController {
    
    private final Map<String, Map<String, Object>> loans = new HashMap<>();
    private final Map<String, List<Map<String, Object>>> installments = new HashMap<>();
    private final Map<String, Map<String, Object>> customers = new HashMap<>();
    
    // Health endpoint for Postman tests
    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }
    
    // Create loan endpoint
    @PostMapping("/loans")
    public ResponseEntity<Map<String, Object>> createLoan(@RequestBody Map<String, Object> loanRequest) {
        try {
            String loanId = "LOAN-" + System.currentTimeMillis();
            
            // Validate required fields
            if (!loanRequest.containsKey("customerId") || 
                !loanRequest.containsKey("amount")) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing required fields: customerId, amount")
                );
            }
            
            // Create loan
            Map<String, Object> loan = new HashMap<>();
            loan.put("loanId", loanId);
            loan.put("customerId", loanRequest.get("customerId"));
            loan.put("amount", loanRequest.get("amount"));
            loan.put("termInMonths", loanRequest.getOrDefault("termInMonths", 12));
            loan.put("loanType", loanRequest.getOrDefault("loanType", "PERSONAL"));
            loan.put("purpose", loanRequest.getOrDefault("purpose", "GENERAL"));
            loan.put("status", "APPROVED");
            loan.put("applicationReference", "REF-" + System.currentTimeMillis());
            loan.put("createdAt", LocalDateTime.now());
            
            loans.put(loanId, loan);
            
            // Create installments
            int termInMonths = Integer.parseInt(String.valueOf(loanRequest.getOrDefault("termInMonths", 12)));
            double amount = Double.parseDouble(String.valueOf(loanRequest.get("amount")));
            double monthlyPayment = amount / termInMonths;
            
            List<Map<String, Object>> loanInstallments = new ArrayList<>();
            for (int i = 1; i <= termInMonths; i++) {
                Map<String, Object> installment = new HashMap<>();
                installment.put("installmentNumber", i);
                installment.put("amount", monthlyPayment);
                installment.put("principalAmount", monthlyPayment * 0.8);
                installment.put("interestAmount", monthlyPayment * 0.2);
                installment.put("dueDate", LocalDateTime.now().plusMonths(i));
                installment.put("status", "PENDING");
                installment.put("paidAmount", 0.0);
                loanInstallments.add(installment);
            }
            installments.put(loanId, loanInstallments);
            
            Map<String, Object> response = new HashMap<>(loan);
            response.put("nextSteps", List.of("AI risk assessment will be performed", "Documentation review pending"));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create loan: " + e.getMessage()));
        }
    }
    
    // Get loans by customer
    @GetMapping("/loans")
    public ResponseEntity<List<Map<String, Object>>> getLoans(
            @RequestParam(required = false) String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        if (customerId != null) {
            loans.values().stream()
                .filter(loan -> customerId.equals(loan.get("customerId")))
                .forEach(result::add);
        } else {
            result.addAll(loans.values());
        }
        
        return ResponseEntity.ok(result);
    }
    
    // Get loan details
    @GetMapping("/loans/{loanId}")
    public ResponseEntity<Map<String, Object>> getLoan(@PathVariable String loanId) {
        Map<String, Object> loan = loans.get(loanId);
        if (loan == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>(loan);
        response.put("installments", installments.get(loanId));
        
        return ResponseEntity.ok(response);
    }
    
    // Get loan installments
    @GetMapping("/loans/{loanId}/installments")
    public ResponseEntity<List<Map<String, Object>>> getInstallments(@PathVariable String loanId) {
        List<Map<String, Object>> loanInstallments = installments.get(loanId);
        if (loanInstallments == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(loanInstallments);
    }
    
    // Pay loan installment
    @PostMapping("/loans/{loanId}/pay")
    public ResponseEntity<Map<String, Object>> payInstallment(
            @PathVariable String loanId,
            @RequestBody Map<String, Object> paymentRequest) {
        
        List<Map<String, Object>> loanInstallments = installments.get(loanId);
        if (loanInstallments == null) {
            return ResponseEntity.notFound().build();
        }
        
        double paymentAmount = Double.parseDouble(String.valueOf(paymentRequest.get("amount")));
        
        // Find first unpaid installment
        for (Map<String, Object> installment : loanInstallments) {
            if ("PENDING".equals(installment.get("status"))) {
                installment.put("status", "PAID");
                installment.put("paidAmount", paymentAmount);
                installment.put("paidDate", LocalDateTime.now());
                break;
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("transactionReference", "TXN-" + System.currentTimeMillis());
        response.put("amount", paymentAmount);
        response.put("fraudCheckResult", Map.of("status", "CLEAR", "riskScore", 15));
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    // Approve loan
    @PostMapping("/loans/{loanId}/approve")
    public ResponseEntity<Map<String, Object>> approveLoan(
            @PathVariable String loanId,
            @RequestBody Map<String, Object> approvalRequest) {
        
        Map<String, Object> loan = loans.get(loanId);
        if (loan == null) {
            return ResponseEntity.notFound().build();
        }
        
        loan.put("status", "APPROVED");
        loan.put("approvedAt", LocalDateTime.now());
        loan.put("approvalNotes", approvalRequest.get("approvalNotes"));
        loan.put("conditions", approvalRequest.get("conditions"));
        
        return ResponseEntity.ok(Map.of("success", true, "loan", loan));
    }
    
    // Create customer
    @PostMapping("/customers")
    public ResponseEntity<Map<String, Object>> createCustomer(@RequestBody Map<String, Object> customerRequest) {
        String customerId = "CUST-" + System.currentTimeMillis();
        
        Map<String, Object> customer = new HashMap<>(customerRequest);
        customer.put("customerId", customerId);
        customer.put("createdAt", LocalDateTime.now());
        customer.put("status", "ACTIVE");
        
        customers.put(customerId, customer);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }
    
    // Get customer
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomer(@PathVariable String customerId) {
        Map<String, Object> customer = customers.get(customerId);
        if (customer == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(customer);
    }
    
    // AI fraud detection
    @PostMapping("/ai/fraud/analyze")
    public ResponseEntity<Map<String, Object>> analyzeFraud(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("riskScore", 25);
        response.put("status", "LOW_RISK");
        response.put("modelUsed", "enhanced-fraud-detection");
        response.put("confidence", 0.87);
        response.put("factors", List.of("Normal transaction pattern", "Verified location", "Device recognized"));
        
        return ResponseEntity.ok(response);
    }
    
    // AI loan recommendations
    @GetMapping("/api/ai/recommendations/loans")
    public ResponseEntity<List<Map<String, Object>>> getLoanRecommendations(
            @RequestParam String customerId) {
        
        List<Map<String, Object>> recommendations = List.of(
            Map.of(
                "loanType", "PERSONAL",
                "recommendedAmount", 15000,
                "interestRate", 4.5,
                "reason", "Based on income and credit history"
            ),
            Map.of(
                "loanType", "MURABAHA",
                "recommendedAmount", 50000,
                "interestRate", 3.8,
                "reason", "Islamic banking product suitable for customer profile"
            )
        );
        
        return ResponseEntity.ok(recommendations);
    }
    
    // AI health
    @GetMapping("/api/ai/health")
    public ResponseEntity<Map<String, Object>> aiHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "OPERATIONAL",
            "services", List.of("fraud-detection", "recommendations", "risk-assessment"),
            "timestamp", LocalDateTime.now()
        ));
    }
}