package com.banking.loan.infrastructure.adapters.in;

import com.banking.loan.application.ports.in.LoanApplicationUseCase;
import com.banking.loan.application.ports.in.PaymentProcessingUseCase;
import com.banking.loan.application.commands.*;
import com.banking.loan.application.queries.*;
import com.banking.loan.application.results.*;
import com.banking.loan.infrastructure.adapters.in.dto.*;
import com.banking.loan.infrastructure.config.BankingSecurityContext;
import com.banking.loan.resilience.BankingCircuitBreakerService;
import com.banking.loan.ratelimit.AdaptiveRateLimitingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Loan REST Controller - Hexagonal Architecture Adapter
 * Handles HTTP requests and delegates to application services
 */
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loan Management", description = "Loan application and management operations")
public class LoanController {
    
    private final LoanApplicationUseCase loanApplicationUseCase;
    private final PaymentProcessingUseCase paymentProcessingUseCase;
    private final BankingCircuitBreakerService circuitBreakerService;
    private final AdaptiveRateLimitingService rateLimitingService;
    private final BankingSecurityContext securityContext;
    
    @PostMapping
    @Operation(summary = "Submit loan application", description = "Submit a new loan application with AI-powered risk assessment")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('LOAN_OFFICER')")
    public ResponseEntity<LoanApplicationResult> submitLoanApplication(
            @Valid @RequestBody SubmitLoanApplicationRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Received loan application request for customer: {} with correlation: {}", 
            request.customerId(), correlationId);
        
        // Check rate limiting
        var rateLimitResult = rateLimitingService.checkRateLimit(
            "loan:create", 
            request.customerId(), 
            securityContext.getClientId()
        );
        
        if (!rateLimitResult.allowed) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(rateLimitResult.retryAfter.getSeconds()))
                .build();
        }
        
        // Execute with circuit breaker protection
        LoanApplicationResult result = circuitBreakerService.executeLoanOperation(
            () -> loanApplicationUseCase.submitLoanApplication(
                new SubmitLoanApplicationCommand(
                    request.customerId(),
                    request.amount(),
                    request.termInMonths(),
                    request.loanType(),
                    request.purpose(),
                    request.collateralDescription(),
                    request.monthlyIncome(),
                    securityContext.getCurrentUserId().orElse("anonymous"),
                    correlationId,
                    securityContext.getTenantId()
                )
            ),
            "submit-loan-application"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
    
    @PostMapping("/{loanId}/approve")
    @Operation(summary = "Approve loan", description = "Approve a loan application after assessment")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('SENIOR_UNDERWRITER')")
    public ResponseEntity<LoanApprovalResult> approveLoan(
            @PathVariable String loanId,
            @Valid @RequestBody ApproveLoanRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Approving loan: {} with correlation: {}", loanId, correlationId);
        
        LoanApprovalResult result = circuitBreakerService.executeLoanOperation(
            () -> loanApplicationUseCase.approveLoan(
                new ApproveLoanCommand(
                    loanId,
                    request.approverId(),
                    request.approvalNotes(),
                    request.approvedAmount(),
                    request.approvedInterestRate(),
                    securityContext.getCurrentUserId().orElse("anonymous"),
                    correlationId,
                    request.conditions()
                )
            ),
            "approve-loan"
        );
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{loanId}/reject")
    @Operation(summary = "Reject loan", description = "Reject a loan application with reasons")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('SENIOR_UNDERWRITER')")
    public ResponseEntity<LoanRejectionResult> rejectLoan(
            @PathVariable String loanId,
            @Valid @RequestBody RejectLoanRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Rejecting loan: {} with correlation: {}", loanId, correlationId);
        
        LoanRejectionResult result = circuitBreakerService.executeLoanOperation(
            () -> loanApplicationUseCase.rejectLoan(
                new RejectLoanCommand(
                    loanId,
                    request.rejecterId(),
                    request.rejectionReason(),
                    request.additionalNotes(),
                    request.rejectionReasons(),
                    securityContext.getCurrentUserId().orElse("anonymous"),
                    correlationId
                )
            ),
            "reject-loan"
        );
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{loanId}")
    @Operation(summary = "Get loan details", description = "Retrieve detailed information about a loan")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('LOAN_OFFICER') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<LoanDetails> getLoanDetails(@PathVariable String loanId, HttpServletRequest request) {
        
        log.debug("Retrieving loan details for: {}", loanId);
        
        // Check rate limiting for queries
        var rateLimitResult = rateLimitingService.checkRateLimit(
            "loan:query", 
            securityContext.getCurrentUserId().orElse("anonymous"), 
            securityContext.getClientId()
        );
        
        if (!rateLimitResult.allowed) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(rateLimitResult.retryAfter.getSeconds()))
                .build();
        }
        
        LoanDetails result = circuitBreakerService.executeLoanOperation(
            () -> loanApplicationUseCase.getLoanDetails(
                new GetLoanDetailsQuery(
                    loanId,
                    null, // customerId - could be extracted from loan or security context
                    securityContext.getCurrentUserId().orElse("anonymous"),
                    request.getParameter("includeAI") != null,
                    request.getParameter("includeCompliance") != null
                )
            ),
            "get-loan-details"
        );
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping
    @Operation(summary = "Get customer loans", description = "Retrieve all loans for a customer")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('LOAN_OFFICER') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<List<LoanSummary>> getCustomerLoans(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.debug("Retrieving loans for customer: {}", customerId);
        
        List<LoanSummary> result = circuitBreakerService.executeLoanOperation(
            () -> loanApplicationUseCase.getCustomerLoans(
                new GetCustomerLoansQuery(
                    customerId,
                    page,
                    size,
                    status,
                    securityContext.getCurrentUserId().orElse("anonymous")
                )
            ),
            "get-customer-loans"
        );
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{loanId}/payments")
    @Operation(summary = "Process payment", description = "Process a loan payment with fraud detection")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('PAYMENT_PROCESSOR')")
    public ResponseEntity<PaymentResult> processPayment(
            @PathVariable String loanId,
            @Valid @RequestBody ProcessPaymentRequest request) {
        
        String correlationId = UUID.randomUUID().toString();
        log.info("Processing payment for loan: {} with correlation: {}", loanId, correlationId);
        
        // Check payment rate limiting (stricter)
        var rateLimitResult = rateLimitingService.checkRateLimit(
            "payment:transfer", 
            securityContext.getCurrentUserId().orElse("anonymous"), 
            securityContext.getClientId()
        );
        
        if (!rateLimitResult.allowed) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(rateLimitResult.retryAfter.getSeconds()))
                .build();
        }
        
        PaymentResult result = circuitBreakerService.executePaymentOperation(
            () -> paymentProcessingUseCase.processPayment(
                new ProcessPaymentCommand(
                    loanId,
                    request.amount(),
                    request.paymentMethod(),
                    request.paymentReference(),
                    request.notes(),
                    null, // customerId - could be extracted from loan
                    request.paymentChannel(),
                    securityContext.getCurrentUserId().orElse("anonymous"),
                    correlationId,
                    true // fraudCheckRequired
                )
            ),
            "process-payment"
        );
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{loanId}/payments")
    @Operation(summary = "Get payment history", description = "Retrieve payment history for a loan")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('LOAN_OFFICER') or hasRole('CUSTOMER_SERVICE')")
    public ResponseEntity<List<PaymentHistory>> getPaymentHistory(
            @PathVariable String loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Retrieving payment history for loan: {}", loanId);
        
        List<PaymentHistory> result = circuitBreakerService.executePaymentOperation(
            () -> paymentProcessingUseCase.getPaymentHistory(
                new GetPaymentHistoryQuery(
                    loanId,
                    null, // customerId
                    null, // fromDate
                    null, // toDate
                    page,
                    size,
                    securityContext.getCurrentUserId().orElse("anonymous")
                )
            ),
            "get-payment-history"
        );
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/{loanId}/early-payment")
    @Operation(summary = "Calculate early payment", description = "Calculate options for early loan payment")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('LOAN_OFFICER')")
    public ResponseEntity<EarlyPaymentOptions> calculateEarlyPayment(@PathVariable String loanId) {
        
        log.debug("Calculating early payment options for loan: {}", loanId);
        
        EarlyPaymentOptions result = circuitBreakerService.executeLoanOperation(
            () -> paymentProcessingUseCase.calculateEarlyPayment(
                new CalculateEarlyPaymentQuery(
                    loanId,
                    null, // paymentAmount
                    null, // paymentDate
                    "STANDARD", // calculationMethod
                    java.time.LocalDate.now(),
                    securityContext.getCurrentUserId().orElse("anonymous")
                )
            ),
            "calculate-early-payment"
        );
        
        return ResponseEntity.ok(result);
    }
}

