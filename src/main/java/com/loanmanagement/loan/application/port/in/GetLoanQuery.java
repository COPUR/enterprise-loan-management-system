package com.loanmanagement.loan.application.port.in;

import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.loan.domain.model.LoanStatus;

import java.util.List;
import java.util.Optional;

/**
 * Inbound Port for Loan Query Operations
 * Defines the interface for retrieving loan information
 */
public interface GetLoanQuery {
    
    /**
     * Retrieve a loan by its ID
     */
    Optional<Loan> getLoanById(Long loanId);
    
    /**
     * Retrieve all loans for a specific customer
     */
    List<Loan> getLoansByCustomerId(Long customerId);
    
    /**
     * Retrieve all loans with a specific status
     */
    List<Loan> getLoansByStatus(LoanStatus status);
    
    /**
     * Get loan details with payment history
     */
    LoanWithPaymentHistory getLoanWithPaymentHistory(Long loanId);
    
    /**
     * Calculate loan eligibility for a customer
     */
    LoanEligibilityResult calculateLoanEligibility(LoanEligibilityQuery query);
    
    record LoanEligibilityQuery(
            Long customerId,
            java.math.BigDecimal requestedAmount,
            Integer termMonths
    ) {}
    
    record LoanEligibilityResult(
            boolean eligible,
            java.math.BigDecimal maxLoanAmount,
            String reason,
            java.time.LocalDateTime assessmentDate
    ) {}
    
    record LoanWithPaymentHistory(
            Loan loan,
            List<PaymentSummary> paymentHistory,
            java.math.BigDecimal outstandingBalance,
            java.time.LocalDate nextPaymentDate
    ) {}
    
    record PaymentSummary(
            Long paymentId,
            java.math.BigDecimal amount,
            java.time.LocalDate paymentDate,
            String type,
            String status
    ) {}
}