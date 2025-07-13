package com.bank.loan.infrastructure.web;

import com.bank.loan.application.LoanManagementService;
import com.bank.loan.application.dto.CreateLoanRequest;
import com.bank.loan.application.dto.LoanResponse;
import com.bank.shared.kernel.domain.Money;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Currency;

/**
 * REST Controller for Loan Management
 * 
 * Implements Hexagonal Architecture - Adapter for HTTP requests
 * Functional Requirements: FR-005 through FR-008
 */
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {
    
    private final LoanManagementService loanService;
    
    public LoanController(LoanManagementService loanService) {
        this.loanService = loanService;
    }
    
    /**
     * FR-005: Create loan application
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public ResponseEntity<LoanResponse> createLoanApplication(@Valid @RequestBody CreateLoanRequest request) {
        LoanResponse response = loanService.createLoanApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * FR-005: Get loan by ID
     */
    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public ResponseEntity<LoanResponse> getLoan(@PathVariable String loanId) {
        LoanResponse response = loanService.findLoanById(loanId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * FR-006: Approve loan
     */
    @PostMapping("/{loanId}/approve")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'BANKER', 'ADMIN')")
    public ResponseEntity<LoanResponse> approveLoan(@PathVariable String loanId) {
        LoanResponse response = loanService.approveLoan(loanId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * FR-006: Reject loan
     */
    @PostMapping("/{loanId}/reject")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'BANKER', 'ADMIN')")
    public ResponseEntity<LoanResponse> rejectLoan(
            @PathVariable String loanId,
            @RequestBody RejectLoanRequest request) {
        LoanResponse response = loanService.rejectLoan(loanId, request.reason());
        return ResponseEntity.ok(response);
    }
    
    /**
     * FR-007: Disburse loan
     */
    @PostMapping("/{loanId}/disburse")
    @PreAuthorize("hasAnyRole('BANKER', 'ADMIN')")
    public ResponseEntity<LoanResponse> disburseLoan(@PathVariable String loanId) {
        LoanResponse response = loanService.disburseLoan(loanId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * FR-008: Make loan payment
     */
    @PostMapping("/{loanId}/payments")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public ResponseEntity<LoanResponse> makePayment(
            @PathVariable String loanId,
            @RequestBody MakePaymentRequest request) {
        
        Money paymentAmount = Money.of(request.amount(), Currency.getInstance(request.currency()));
        LoanResponse response = loanService.makePayment(loanId, paymentAmount);
        return ResponseEntity.ok(response);
    }
    
    /**
     * FR-008: Cancel loan
     */
    @PostMapping("/{loanId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANKER', 'ADMIN')")
    public ResponseEntity<LoanResponse> cancelLoan(
            @PathVariable String loanId,
            @RequestBody CancelLoanRequest request) {
        LoanResponse response = loanService.cancelLoan(loanId, request.reason());
        return ResponseEntity.ok(response);
    }
    
    // Request DTOs
    public record RejectLoanRequest(String reason) {}
    public record MakePaymentRequest(java.math.BigDecimal amount, String currency) {}
    public record CancelLoanRequest(String reason) {}
}