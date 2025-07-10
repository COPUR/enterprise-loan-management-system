package com.loanmanagement.loan.application.port.in;

import com.loanmanagement.loan.domain.model.Loan;

/**
 * Inbound Port for Loan Approval Operations
 * Follows the single responsibility principle by focusing only on loan approval
 */
public interface ApproveLoanUseCase {
    
    /**
     * Approve a loan application
     */
    Loan approveLoan(ApproveLoanCommand command);
    
    /**
     * Reject a loan application
     */
    Loan rejectLoan(RejectLoanCommand command);
    
    record ApproveLoanCommand(
            Long loanId,
            String approvedBy,
            String approvalNotes,
            java.time.LocalDateTime approvalDate
    ) {}
    
    record RejectLoanCommand(
            Long loanId,
            String rejectedBy,
            String reason,
            java.time.LocalDateTime rejectionDate
    ) {}
}