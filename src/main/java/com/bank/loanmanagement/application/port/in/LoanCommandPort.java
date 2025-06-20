package com.bank.loanmanagement.application.port.in;

import com.bank.loanmanagement.domain.loan.Loan;
import com.bank.loanmanagement.domain.loan.LoanId;

/**
 * Port for loan command operations
 * Following hexagonal architecture principles
 */
public interface LoanCommandPort {
    
    Loan createLoan(Loan loan);
    
    Loan updateLoan(Loan loan);
    
    Loan approveLoan(LoanId loanId, String approverNotes);
    
    Loan rejectLoan(LoanId loanId, String rejectionReason);
    
    void deleteLoan(LoanId loanId);
}