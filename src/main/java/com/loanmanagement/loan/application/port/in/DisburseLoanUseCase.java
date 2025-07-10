package com.loanmanagement.loan.application.port.in;

import com.loanmanagement.loan.domain.model.Loan;

/**
 * Inbound Port for Loan Disbursement Operations
 * Handles the disbursement of approved loans
 */
public interface DisburseLoanUseCase {
    
    /**
     * Disburse an approved loan
     */
    Loan disburseLoan(DisburseLoanCommand command);
    
    record DisburseLoanCommand(
            Long loanId,
            String disbursedBy,
            String bankAccountNumber,
            String disbursementMethod,
            java.time.LocalDateTime disbursementDate,
            String disbursementNotes
    ) {}
}