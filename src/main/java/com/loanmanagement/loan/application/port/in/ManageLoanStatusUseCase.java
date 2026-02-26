package com.loanmanagement.loan.application.port.in;

import com.loanmanagement.loan.domain.model.Loan;

/**
 * Inbound Port for Loan Status Management Operations
 * Handles loan lifecycle status changes
 */
public interface ManageLoanStatusUseCase {
    
    /**
     * Mark a loan as completed
     */
    Loan completeLoan(CompleteLoanCommand command);
    
    /**
     * Mark a loan as defaulted
     */
    Loan markLoanAsDefaulted(MarkDefaultedCommand command);
    
    /**
     * Restructure a loan
     */
    Loan restructureLoan(RestructureLoanCommand command);
    
    record CompleteLoanCommand(
            Long loanId,
            String completedBy,
            java.time.LocalDateTime completionDate,
            String completionNotes
    ) {}
    
    record MarkDefaultedCommand(
            Long loanId,
            String defaultedBy,
            String defaultReason,
            java.time.LocalDateTime defaultDate,
            java.math.BigDecimal outstandingAmount
    ) {}
    
    record RestructureLoanCommand(
            Long loanId,
            java.math.BigDecimal newInterestRate,
            Integer newTermMonths,
            String restructureReason,
            String restructuredBy,
            java.time.LocalDateTime restructureDate
    ) {}
}