package com.bank.loan.loan.api.controller;

import com.bank.loan.loan.security.dpop.annotation.DPoPSecured;
import com.bank.loan.loan.security.dpop.filter.DPoPValidationFilter;
import com.bank.loan.loan.security.fapi.annotation.FAPISecured;
import com.bank.loan.loan.security.fapi.validation.FAPISecurityHeaders;
import com.bank.loan.loan.service.LoanService;
import com.bank.loan.loan.service.PaymentService;
import com.bank.loan.loan.service.AuditService;
import com.bank.loan.loan.dto.LoanApplicationRequest;
import com.bank.loan.loan.dto.LoanResponse;
import com.bank.loan.loan.dto.PaymentRequest;
import com.bank.loan.loan.dto.PaymentResponse;
import com.bank.loan.loan.dto.InstallmentResponse;
import com.bank.loan.loan.entity.Loan;
import com.bank.loan.loan.entity.Payment;
import com.bank.loan.loan.exception.LoanNotFoundException;
import com.bank.loan.loan.exception.PaymentProcessingException;
import com.bank.loan.loan.exception.InsufficientAuthorizationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FAPI 2.0 + DPoP Compliant Loan Controller
 * 
 * Implements enterprise-grade security for loan management operations:
 * - DPoP token binding validation
 * - FAPI 2.0 security headers
 * - Role-based access control
 * - Comprehensive audit logging
 * - Banking regulatory compliance
 * - Payment allocation waterfall
 * - Idempotency protection
 * - Rate limiting
 * 
 * Replaces SimpleLoanController with proper security implementation
 */
@RestController
@RequestMapping("/api/v1/loans")
@DPoPSecured
@FAPISecured
@Validated
public class SecureLoanController {

    private final LoanService loanService;
    private final PaymentService paymentService;
    private final AuditService auditService;

    @Autowired
    public SecureLoanController(LoanService loanService, 
                              PaymentService paymentService,
                              AuditService auditService) {
        this.loanService = loanService;
        this.paymentService = paymentService;
        this.auditService = auditService;
    }

    /**
     * Create Loan Application
     * 
     * FAPI 2.0 Requirements:
     * - DPoP-bound access token required
     * - FAPI security headers mandatory
     * - Idempotency key required for financial operations
     * - Comprehensive audit logging
     * 
     * Authorization: LOAN_OFFICER, SENIOR_LOAN_OFFICER
     */
    @PostMapping
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('SENIOR_LOAN_OFFICER')")
    public ResponseEntity<LoanResponse> createLoan(
            @Valid @RequestBody LoanApplicationRequest request,
            @RequestHeader("X-FAPI-Interaction-ID") @NotNull String fiapiInteractionId,
            @RequestHeader("X-FAPI-Auth-Date") String fapiAuthDate,
            @RequestHeader("X-FAPI-Customer-IP-Address") String customerIpAddress,
            @RequestHeader("X-Idempotency-Key") @NotNull String idempotencyKey,
            HttpServletRequest httpRequest) {

        try {
            // Validate FAPI security headers
            FAPISecurityHeaders.validateHeaders(fiapiInteractionId, fapiAuthDate, customerIpAddress);
            
            // Get authenticated user context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            // Check idempotency
            if (loanService.isIdempotentRequestProcessed(idempotencyKey)) {
                LoanResponse existingResponse = loanService.getIdempotentResponse(idempotencyKey);
                return ResponseEntity.ok()
                    .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .body(existingResponse);
            }

            // Validate loan application business rules
            loanService.validateLoanApplication(request);
            
            // Create loan with full audit trail
            Loan loan = loanService.createLoan(request, userId, fiapiInteractionId, idempotencyKey);
            
            // Audit log the loan creation
            auditService.logLoanCreation(loan, userId, httpRequest.getRemoteAddr(), 
                                       fiapiInteractionId, getUserAgent(httpRequest));
            
            // Convert to response DTO
            LoanResponse response = loanService.convertToLoanResponse(loan);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .header("X-Idempotency-Key", idempotencyKey)
                .body(response);
                
        } catch (Exception e) {
            // Log security/business violation
            auditService.logSecurityViolation("LOAN_CREATION_FAILED", e.getMessage(), 
                                             SecurityContextHolder.getContext().getAuthentication().getName(),
                                             httpRequest.getRemoteAddr(), fiapiInteractionId);
            throw e;
        }
    }

    /**
     * Get Loans by Customer
     * 
     * Authorization: Customer can view own loans, Officers can view any customer's loans
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('LOAN_OFFICER') or hasRole('SENIOR_LOAN_OFFICER')")
    public ResponseEntity<List<LoanResponse>> getLoans(
            @RequestParam @NotNull String customerId,
            @RequestHeader("X-FAPI-Interaction-ID") @NotNull String fiapiInteractionId,
            @RequestHeader(value = "X-FAPI-Auth-Date", required = false) String fapiAuthDate,
            @RequestHeader(value = "X-FAPI-Customer-IP-Address", required = false) String customerIpAddress,
            HttpServletRequest httpRequest) {

        try {
            // Validate FAPI security headers
            FAPISecurityHeaders.validateHeaders(fiapiInteractionId, fapiAuthDate, customerIpAddress);
            
            // Get authenticated user context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            // Authorization check - customers can only view their own loans
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!loanService.isCustomerOwnedByUser(customerId, userId)) {
                    throw new InsufficientAuthorizationException("Customer can only view their own loans");
                }
            }
            
            // Get loans with proper authorization filtering
            List<Loan> loans = loanService.getLoansByCustomer(customerId, userId, auth.getAuthorities());
            
            // Audit log the data access
            auditService.logDataAccess("LOANS_VIEWED", customerId, userId, 
                                     httpRequest.getRemoteAddr(), fiapiInteractionId);
            
            // Convert to response DTOs
            List<LoanResponse> response = loanService.convertToLoanResponseList(loans);
            
            return ResponseEntity.ok()
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .body(response);
                
        } catch (Exception e) {
            auditService.logSecurityViolation("LOAN_ACCESS_DENIED", e.getMessage(), 
                                             SecurityContextHolder.getContext().getAuthentication().getName(),
                                             httpRequest.getRemoteAddr(), fiapiInteractionId);
            throw e;
        }
    }

    /**
     * Get Loan Installments
     * 
     * Authorization: Customer can view own loan installments, Officers can view any
     */
    @GetMapping("/{loanId}/installments")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('LOAN_OFFICER') or hasRole('SENIOR_LOAN_OFFICER')")
    public ResponseEntity<List<InstallmentResponse>> getInstallments(
            @PathVariable @NotNull String loanId,
            @RequestHeader("X-FAPI-Interaction-ID") @NotNull String fiapiInteractionId,
            @RequestHeader(value = "X-FAPI-Auth-Date", required = false) String fapiAuthDate,
            @RequestHeader(value = "X-FAPI-Customer-IP-Address", required = false) String customerIpAddress,
            HttpServletRequest httpRequest) {

        try {
            // Validate FAPI security headers
            FAPISecurityHeaders.validateHeaders(fiapiInteractionId, fapiAuthDate, customerIpAddress);
            
            // Get authenticated user context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            // Verify loan exists and user has access
            Loan loan = loanService.getLoanById(loanId);
            if (loan == null) {
                throw new LoanNotFoundException("Loan not found: " + loanId);
            }
            
            // Authorization check - customers can only view their own loan installments
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!loanService.isLoanOwnedByUser(loanId, userId)) {
                    throw new InsufficientAuthorizationException("Customer can only view their own loan installments");
                }
            }
            
            // Get installments
            List<InstallmentResponse> installments = loanService.getInstallmentsByLoan(loanId);
            
            // Audit log the data access
            auditService.logDataAccess("INSTALLMENTS_VIEWED", loanId, userId, 
                                     httpRequest.getRemoteAddr(), fiapiInteractionId);
            
            return ResponseEntity.ok()
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .body(installments);
                
        } catch (Exception e) {
            auditService.logSecurityViolation("INSTALLMENT_ACCESS_DENIED", e.getMessage(), 
                                             SecurityContextHolder.getContext().getAuthentication().getName(),
                                             httpRequest.getRemoteAddr(), fiapiInteractionId);
            throw e;
        }
    }

    /**
     * Process Loan Payment
     * 
     * BANKING COMPLIANCE IMPLEMENTATION:
     * - Payment allocation waterfall (fees -> interest -> principal)
     * - Late fee assessment and penalty calculation
     * - Partial payment support with proper allocation
     * - Payment method validation and channel processing
     * - Regulatory compliance (TILA, RESPA, FDCPA)
     * - Transaction isolation and rollback capability
     * - Comprehensive audit trail and payment history tracking
     * 
     * Authorization: Customer can pay own loans, Officers can process any payment
     */
    @PostMapping("/{loanId}/payments")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('LOAN_OFFICER') or hasRole('PAYMENT_PROCESSOR')")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable @NotNull String loanId,
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("X-FAPI-Interaction-ID") @NotNull String fiapiInteractionId,
            @RequestHeader("X-FAPI-Auth-Date") String fapiAuthDate,
            @RequestHeader("X-FAPI-Customer-IP-Address") String customerIpAddress,
            @RequestHeader("X-Idempotency-Key") @NotNull String idempotencyKey,
            HttpServletRequest httpRequest) {

        try {
            // Validate FAPI security headers
            FAPISecurityHeaders.validateHeaders(fiapiInteractionId, fapiAuthDate, customerIpAddress);
            
            // Get authenticated user context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            // Check idempotency for payment processing
            if (paymentService.isIdempotentPaymentProcessed(idempotencyKey)) {
                PaymentResponse existingResponse = paymentService.getIdempotentPaymentResponse(idempotencyKey);
                return ResponseEntity.ok()
                    .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .body(existingResponse);
            }
            
            // Verify loan exists and user has access
            Loan loan = loanService.getLoanById(loanId);
            if (loan == null) {
                throw new LoanNotFoundException("Loan not found: " + loanId);
            }
            
            // Authorization check - customers can only pay their own loans
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!loanService.isLoanOwnedByUser(loanId, userId)) {
                    throw new InsufficientAuthorizationException("Customer can only pay their own loans");
                }
            }
            
            // Validate payment request
            paymentService.validatePaymentRequest(request, loan);
            
            // BANKING INDUSTRY COMPLIANCE: Process payment with proper allocation waterfall
            Payment payment = paymentService.processPaymentWithWaterfall(
                loan, request, userId, fiapiInteractionId, idempotencyKey);
            
            // Audit log the payment processing
            auditService.logPaymentProcessing(payment, userId, httpRequest.getRemoteAddr(), 
                                            fiapiInteractionId, getUserAgent(httpRequest));
            
            // Convert to response DTO
            PaymentResponse response = paymentService.convertToPaymentResponse(payment);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .header("X-Idempotency-Key", idempotencyKey)
                .body(response);
                
        } catch (Exception e) {
            auditService.logSecurityViolation("PAYMENT_PROCESSING_FAILED", e.getMessage(), 
                                             SecurityContextHolder.getContext().getAuthentication().getName(),
                                             httpRequest.getRemoteAddr(), fiapiInteractionId);
            throw e;
        }
    }

    /**
     * Get Payment History
     * 
     * Authorization: Customer can view own payment history, Officers can view any
     */
    @GetMapping("/{loanId}/payments")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('LOAN_OFFICER') or hasRole('SENIOR_LOAN_OFFICER')")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @PathVariable @NotNull String loanId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-FAPI-Interaction-ID") @NotNull String fiapiInteractionId,
            @RequestHeader(value = "X-FAPI-Auth-Date", required = false) String fapiAuthDate,
            @RequestHeader(value = "X-FAPI-Customer-IP-Address", required = false) String customerIpAddress,
            HttpServletRequest httpRequest) {

        try {
            // Validate FAPI security headers
            FAPISecurityHeaders.validateHeaders(fiapiInteractionId, fapiAuthDate, customerIpAddress);
            
            // Get authenticated user context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            // Verify loan exists and user has access
            Loan loan = loanService.getLoanById(loanId);
            if (loan == null) {
                throw new LoanNotFoundException("Loan not found: " + loanId);
            }
            
            // Authorization check
            if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
                if (!loanService.isLoanOwnedByUser(loanId, userId)) {
                    throw new InsufficientAuthorizationException("Customer can only view their own payment history");
                }
            }
            
            // Get payment history with pagination
            List<Payment> payments = paymentService.getPaymentHistory(loanId, page, size);
            
            // Audit log the data access
            auditService.logDataAccess("PAYMENT_HISTORY_VIEWED", loanId, userId, 
                                     httpRequest.getRemoteAddr(), fiapiInteractionId);
            
            // Convert to response DTOs
            List<PaymentResponse> response = paymentService.convertToPaymentResponseList(payments);
            
            return ResponseEntity.ok()
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .body(response);
                
        } catch (Exception e) {
            auditService.logSecurityViolation("PAYMENT_HISTORY_ACCESS_DENIED", e.getMessage(), 
                                             SecurityContextHolder.getContext().getAuthentication().getName(),
                                             httpRequest.getRemoteAddr(), fiapiInteractionId);
            throw e;
        }
    }

    /**
     * Approve Loan (Senior Officers Only)
     * 
     * High-security operation requiring senior authorization
     */
    @PostMapping("/{loanId}/approve")
    @PreAuthorize("hasRole('SENIOR_LOAN_OFFICER')")
    public ResponseEntity<LoanResponse> approveLoan(
            @PathVariable @NotNull String loanId,
            @RequestBody Map<String, Object> approvalRequest,
            @RequestHeader("X-FAPI-Interaction-ID") @NotNull String fiapiInteractionId,
            @RequestHeader("X-FAPI-Auth-Date") String fapiAuthDate,
            @RequestHeader("X-FAPI-Customer-IP-Address") String customerIpAddress,
            @RequestHeader("X-Idempotency-Key") @NotNull String idempotencyKey,
            HttpServletRequest httpRequest) {

        try {
            // Validate FAPI security headers
            FAPISecurityHeaders.validateHeaders(fiapiInteractionId, fapiAuthDate, customerIpAddress);
            
            // Get authenticated user context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth.getName();
            
            // Verify loan exists
            Loan loan = loanService.getLoanById(loanId);
            if (loan == null) {
                throw new LoanNotFoundException("Loan not found: " + loanId);
            }
            
            // Check idempotency
            if (loanService.isIdempotentRequestProcessed(idempotencyKey)) {
                LoanResponse existingResponse = loanService.getIdempotentResponse(idempotencyKey);
                return ResponseEntity.ok()
                    .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .body(existingResponse);
            }
            
            // Approve loan with business validation
            Loan approvedLoan = loanService.approveLoan(loan, approvalRequest, userId, fiapiInteractionId, idempotencyKey);
            
            // Audit log the approval
            auditService.logLoanApproval(approvedLoan, userId, httpRequest.getRemoteAddr(), 
                                       fiapiInteractionId, getUserAgent(httpRequest));
            
            // Convert to response DTO
            LoanResponse response = loanService.convertToLoanResponse(approvedLoan);
            
            return ResponseEntity.ok()
                .header("X-FAPI-Interaction-ID", fiapiInteractionId)
                .header("X-Idempotency-Key", idempotencyKey)
                .body(response);
                
        } catch (Exception e) {
            auditService.logSecurityViolation("LOAN_APPROVAL_FAILED", e.getMessage(), 
                                             SecurityContextHolder.getContext().getAuthentication().getName(),
                                             httpRequest.getRemoteAddr(), fiapiInteractionId);
            throw e;
        }
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    // ========================================================================
    // Exception Handlers
    // ========================================================================

    @ExceptionHandler(LoanNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleLoanNotFoundException(
            LoanNotFoundException e, HttpServletRequest request) {
        
        String fiapiInteractionId = request.getHeader("X-FAPI-Interaction-ID");
        
        Map<String, Object> error = Map.of(
            "error", "loan_not_found",
            "error_description", e.getMessage(),
            "fapi_interaction_id", fiapiInteractionId != null ? fiapiInteractionId : UUID.randomUUID().toString()
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .header("X-FAPI-Interaction-ID", fiapiInteractionId)
            .body(error);
    }

    @ExceptionHandler(InsufficientAuthorizationException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientAuthorizationException(
            InsufficientAuthorizationException e, HttpServletRequest request) {
        
        String fiapiInteractionId = request.getHeader("X-FAPI-Interaction-ID");
        
        Map<String, Object> error = Map.of(
            "error", "insufficient_authorization",
            "error_description", e.getMessage(),
            "fapi_interaction_id", fiapiInteractionId != null ? fiapiInteractionId : UUID.randomUUID().toString()
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .header("X-FAPI-Interaction-ID", fiapiInteractionId)
            .body(error);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentProcessingException(
            PaymentProcessingException e, HttpServletRequest request) {
        
        String fiapiInteractionId = request.getHeader("X-FAPI-Interaction-ID");
        
        Map<String, Object> error = Map.of(
            "error", "payment_processing_failed",
            "error_description", e.getMessage(),
            "fapi_interaction_id", fiapiInteractionId != null ? fiapiInteractionId : UUID.randomUUID().toString()
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .header("X-FAPI-Interaction-ID", fiapiInteractionId)
            .body(error);
    }
}