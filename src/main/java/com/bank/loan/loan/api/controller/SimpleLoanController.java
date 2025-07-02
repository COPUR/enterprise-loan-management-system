package com.bank.loan.loan.api.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Simple Loan Controller for Business Requirements Validation
 * 
 * Implements the core Orange Solution case study requirements:
 * 1. Create loan
 * 2. List loans by customer
 * 3. List installments by loan
 * 4. Pay loan installment
 */
@RestController
@RequestMapping("/api/v1/loans")
public class SimpleLoanController {
    
    // In-memory storage for testing purposes
    private final Map<String, Map<String, Object>> loans = new HashMap<>();
    private final Map<String, List<Map<String, Object>>> installments = new HashMap<>();
    
    /**
     * BUSINESS REQUIREMENT 1: Create loan
     * POST /api/v1/loans
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createLoan(@RequestBody Map<String, Object> loanRequest) {
        try {
            // Generate loan ID
            String loanId = "LOAN-" + System.currentTimeMillis();
            
            // Validate required fields
            if (!loanRequest.containsKey("customerId") || 
                !loanRequest.containsKey("amount") ||
                !loanRequest.containsKey("interestRate") ||
                !loanRequest.containsKey("numberOfInstallments")) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing required fields: customerId, amount, interestRate, numberOfInstallments")
                );
            }
            
            // Create loan
            Map<String, Object> loan = new HashMap<>();
            loan.put("loanId", loanId);
            loan.put("customerId", loanRequest.get("customerId"));
            loan.put("amount", loanRequest.get("amount"));
            loan.put("interestRate", loanRequest.get("interestRate"));
            loan.put("numberOfInstallments", loanRequest.get("numberOfInstallments"));
            loan.put("status", "CREATED");
            loan.put("createdAt", LocalDateTime.now().toString());
            
            // Calculate installments
            double amount = ((Number) loanRequest.get("amount")).doubleValue();
            double interestRate = ((Number) loanRequest.get("interestRate")).doubleValue();
            int numberOfInstallments = ((Number) loanRequest.get("numberOfInstallments")).intValue();
            
            double monthlyPayment = calculateMonthlyPayment(amount, interestRate, numberOfInstallments);
            loan.put("monthlyPayment", monthlyPayment);
            
            // Store loan
            loans.put(loanId, loan);
            
            // Create installments
            createInstallments(loanId, amount, interestRate, numberOfInstallments, monthlyPayment);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(loan);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Failed to create loan: " + e.getMessage())
            );
        }
    }
    
    /**
     * BUSINESS REQUIREMENT 2: List loans by customer
     * GET /api/v1/loans?customerId={customerId}
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getLoans(@RequestParam String customerId) {
        List<Map<String, Object>> customerLoans = loans.values().stream()
            .filter(loan -> customerId.equals(loan.get("customerId")))
            .toList();
        
        return ResponseEntity.ok(customerLoans);
    }
    
    /**
     * BUSINESS REQUIREMENT 3: List installments by loan
     * GET /api/v1/loans/{loanId}/installments
     */
    @GetMapping("/{loanId}/installments")
    public ResponseEntity<List<Map<String, Object>>> getInstallments(@PathVariable String loanId) {
        if (!loans.containsKey(loanId)) {
            return ResponseEntity.notFound().build();
        }
        
        List<Map<String, Object>> loanInstallments = installments.getOrDefault(loanId, new ArrayList<>());
        return ResponseEntity.ok(loanInstallments);
    }
    
    /**
     * BUSINESS REQUIREMENT 4: Pay loan installment
     * POST /api/v1/loans/{loanId}/installments/{installmentNumber}/pay
     * 
     * WARNING: This implementation violates banking industry standards.
     * Missing critical features:
     * - Payment allocation waterfall
     * - Late fee assessment
     * - Partial payment handling
     * - Payment channel validation
     * - Regulatory compliance checks
     * 
     * This controller should be replaced with proper enterprise payment processing.
     */
    @PostMapping("/{loanId}/installments/{installmentNumber}/pay")
    public ResponseEntity<Map<String, Object>> payInstallment(
            @PathVariable String loanId, 
            @PathVariable int installmentNumber,
            @RequestBody Map<String, Object> paymentRequest) {
        
        if (!loans.containsKey(loanId)) {
            return ResponseEntity.notFound().build();
        }
        
        List<Map<String, Object>> loanInstallments = installments.get(loanId);
        if (loanInstallments == null || installmentNumber < 1 || installmentNumber > loanInstallments.size()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Invalid installment number")
            );
        }
        
        Map<String, Object> installment = loanInstallments.get(installmentNumber - 1);
        
        // Check if already paid
        if ("PAID".equals(installment.get("status"))) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Installment already paid")
            );
        }
        
        // Basic payment processing - INDUSTRY VIOLATIONS:
        // - No payment allocation waterfall (fees -> interest -> principal)
        // - No late fee assessment or penalty calculation
        // - No partial payment support with proper allocation
        // - No payment method validation or channel processing
        // - No regulatory compliance (TILA, RESPA, FDCPA)
        // - No transaction isolation or rollback capability
        // - No audit trail or payment history tracking
        
        installment.put("status", "PAID");
        installment.put("paidAt", LocalDateTime.now().toString());
        installment.put("paidAmount", paymentRequest.getOrDefault("amount", installment.get("amount")));
        
        // Create basic payment record - missing critical payment details
        Map<String, Object> paymentResult = new HashMap<>();
        paymentResult.put("loanId", loanId);
        paymentResult.put("installmentNumber", installmentNumber);
        paymentResult.put("amount", installment.get("paidAmount"));
        paymentResult.put("paidAt", installment.get("paidAt"));
        paymentResult.put("status", "SUCCESS");
        paymentResult.put("warning", "This is a simplified implementation. Production systems require proper payment allocation, late fee assessment, and regulatory compliance.");
        
        return ResponseEntity.ok(paymentResult);
    }
    
    // Helper methods
    private double calculateMonthlyPayment(double amount, double annualRate, int numberOfInstallments) {
        if (annualRate == 0) {
            return amount / numberOfInstallments;
        }
        
        double monthlyRate = annualRate / 12;
        return amount * (monthlyRate * Math.pow(1 + monthlyRate, numberOfInstallments)) / 
               (Math.pow(1 + monthlyRate, numberOfInstallments) - 1);
    }
    
    private void createInstallments(String loanId, double amount, double interestRate, 
                                  int numberOfInstallments, double monthlyPayment) {
        List<Map<String, Object>> loanInstallments = new ArrayList<>();
        double remainingBalance = amount;
        double monthlyRate = interestRate / 12;
        
        for (int i = 1; i <= numberOfInstallments; i++) {
            Map<String, Object> installment = new HashMap<>();
            
            double interestPayment = remainingBalance * monthlyRate;
            double principalPayment = monthlyPayment - interestPayment;
            remainingBalance -= principalPayment;
            
            installment.put("installmentNumber", i);
            installment.put("amount", monthlyPayment);
            installment.put("principalAmount", principalPayment);
            installment.put("interestAmount", interestPayment);
            installment.put("remainingBalance", Math.max(0, remainingBalance));
            installment.put("dueDate", LocalDateTime.now().plusMonths(i).toLocalDate().toString());
            installment.put("status", "PENDING");
            
            loanInstallments.add(installment);
        }
        
        installments.put(loanId, loanInstallments);
    }
}