package com.loanmanagement.loan.application.port.in;

import com.loanmanagement.loan.domain.model.Loan;

public interface LoanManagementUseCase {
    
    Loan approveLoan(Long loanId);
    
    Loan rejectLoan(Long loanId, String reason);
    
    Loan disburseLoan(Long loanId);
    
    Loan completeLoan(Long loanId);
    
    Loan markAsDefaulted(Long loanId);
}