package com.bank.loanmanagement.loanorigination.application;

import com.bank.loanmanagement.loanorigination.domain.Loan;
import com.bank.loanmanagement.loanorigination.infrastructure.LoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin(origins = "*")
public class LoanController {

    private final LoanRepository loanRepository;

    public LoanController(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLoans() {
        List<Loan> loans = loanRepository.findAll();
        
        Map<String, Object> response = new HashMap<>();
        response.put("loans", loans.stream().map(loan -> {
            Map<String, Object> loanData = new HashMap<>();
            loanData.put("id", loan.getId());
            loanData.put("loanNumber", loan.getLoanNumber());
            loanData.put("customerId", loan.getCustomerId());
            loanData.put("principalAmount", loan.getPrincipalAmount());
            loanData.put("installmentCount", loan.getInstallmentCount());
            loanData.put("monthlyInterestRate", loan.getMonthlyInterestRate());
            loanData.put("monthlyPaymentAmount", loan.getMonthlyPaymentAmount());
            loanData.put("totalAmount", loan.getTotalAmount());
            loanData.put("outstandingBalance", loan.getOutstandingBalance());
            loanData.put("loanStatus", loan.getLoanStatus());
            loanData.put("disbursementDate", loan.getDisbursementDate());
            loanData.put("maturityDate", loan.getMaturityDate());
            loanData.put("nextPaymentDate", loan.getNextPaymentDate());
            return loanData;
        }).toList());
        
        response.put("total", loans.size());
        response.put("boundedContext", "Loan Origination (DDD)");
        response.put("businessRules", Map.of(
            "installmentsAllowed", List.of(6, 9, 12, 24),
            "interestRateRange", "0.1% - 0.5% monthly",
            "principalAmountRange", "$1,000 - $500,000"
        ));
        response.put("dataSource", "PostgreSQL Database");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getLoanById(@PathVariable Long id) {
        Optional<Loan> loanOpt = loanRepository.findById(id);
        
        if (loanOpt.isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Loan not found");
            errorResponse.put("id", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        Loan loan = loanOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("id", loan.getId());
        response.put("loanNumber", loan.getLoanNumber());
        response.put("customerId", loan.getCustomerId());
        response.put("principalAmount", loan.getPrincipalAmount());
        response.put("installmentCount", loan.getInstallmentCount());
        response.put("monthlyInterestRate", loan.getMonthlyInterestRate());
        response.put("monthlyPaymentAmount", loan.getMonthlyPaymentAmount());
        response.put("totalAmount", loan.getTotalAmount());
        response.put("outstandingBalance", loan.getOutstandingBalance());
        response.put("loanStatus", loan.getLoanStatus());
        response.put("disbursementDate", loan.getDisbursementDate());
        response.put("maturityDate", loan.getMaturityDate());
        response.put("nextPaymentDate", loan.getNextPaymentDate());
        response.put("createdAt", loan.getCreatedAt());
        response.put("updatedAt", loan.getUpdatedAt());
        response.put("boundedContext", "Loan Origination (DDD)");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createLoan(@RequestBody Map<String, Object> loanData) {
        try {
            // Validate business rules
            Integer installmentCount = Integer.valueOf(loanData.get("installmentCount").toString());
            if (installmentCount != 6 && installmentCount != 9 && installmentCount != 12 && installmentCount != 24) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid installment count");
                errorResponse.put("message", "Installment count must be 6, 9, 12, or 24");
                errorResponse.put("businessRule", "Enterprise Loan Management System - Installment Validation");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            BigDecimal monthlyInterestRate = new BigDecimal(loanData.get("monthlyInterestRate").toString());
            if (monthlyInterestRate.compareTo(BigDecimal.valueOf(0.001)) < 0 || 
                monthlyInterestRate.compareTo(BigDecimal.valueOf(0.005)) > 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid interest rate");
                errorResponse.put("message", "Monthly interest rate must be between 0.1% and 0.5%");
                errorResponse.put("businessRule", "Enterprise Loan Management System - Interest Rate Validation");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            Loan loan = new Loan();
            loan.setLoanNumber("LOAN" + System.currentTimeMillis());
            loan.setCustomerId(Long.valueOf(loanData.get("customerId").toString()));
            loan.setPrincipalAmount(new BigDecimal(loanData.get("principalAmount").toString()));
            loan.setInstallmentCount(installmentCount);
            loan.setMonthlyInterestRate(monthlyInterestRate);
            loan.setDisbursementDate(LocalDateTime.now());
            loan.setMaturityDate(LocalDate.now().plusMonths(installmentCount));
            loan.setNextPaymentDate(LocalDate.now().plusMonths(1));
            
            Loan savedLoan = loanRepository.save(loan);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedLoan.getId());
            response.put("loanNumber", savedLoan.getLoanNumber());
            response.put("principalAmount", savedLoan.getPrincipalAmount());
            response.put("installmentCount", savedLoan.getInstallmentCount());
            response.put("monthlyPaymentAmount", savedLoan.getMonthlyPaymentAmount());
            response.put("totalAmount", savedLoan.getTotalAmount());
            response.put("loanStatus", savedLoan.getLoanStatus());
            response.put("message", "Loan created successfully");
            response.put("boundedContext", "Loan Origination (DDD)");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create loan");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getLoansByCustomerId(@PathVariable Long customerId) {
        List<Loan> loans = loanRepository.findByCustomerId(customerId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("loans", loans.stream().map(loan -> {
            Map<String, Object> loanData = new HashMap<>();
            loanData.put("id", loan.getId());
            loanData.put("loanNumber", loan.getLoanNumber());
            loanData.put("principalAmount", loan.getPrincipalAmount());
            loanData.put("installmentCount", loan.getInstallmentCount());
            loanData.put("outstandingBalance", loan.getOutstandingBalance());
            loanData.put("loanStatus", loan.getLoanStatus());
            loanData.put("nextPaymentDate", loan.getNextPaymentDate());
            return loanData;
        }).toList());
        
        response.put("customerId", customerId);
        response.put("total", loans.size());
        response.put("boundedContext", "Loan Origination (DDD)");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getLoanStats() {
        long totalLoans = loanRepository.count();
        List<Loan> activeLoans = loanRepository.findActiveLoans();
        
        BigDecimal totalOutstanding = activeLoans.stream()
            .map(Loan::getOutstandingBalance)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalLoans", totalLoans);
        response.put("activeLoans", activeLoans.size());
        response.put("totalOutstandingBalance", totalOutstanding);
        response.put("boundedContext", "Loan Origination (DDD)");
        response.put("dataSource", "PostgreSQL Database - Live Statistics");
        
        return ResponseEntity.ok(response);
    }
}