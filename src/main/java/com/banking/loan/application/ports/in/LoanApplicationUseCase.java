package com.banking.loan.application.ports.in;

import com.banking.loan.application.commands.*;
import com.banking.loan.application.queries.*;
import com.banking.loan.application.results.*;
import java.util.List;

/**
 * Loan Application Use Case - Hexagonal Architecture Port
 * Defines the contract for loan application operations
 */
public interface LoanApplicationUseCase {
    
    /**
     * Submit a new loan application
     */
    LoanApplicationResult submitLoanApplication(SubmitLoanApplicationCommand command);
    
    /**
     * Approve a loan application
     */
    LoanApprovalResult approveLoan(ApproveLoanCommand command);
    
    /**
     * Reject a loan application
     */
    LoanRejectionResult rejectLoan(RejectLoanCommand command);
    
    /**
     * Get detailed information about a loan
     */
    LoanDetails getLoanDetails(GetLoanDetailsQuery query);
    
    /**
     * Get all loans for a customer
     */
    List<LoanSummary> getCustomerLoans(GetCustomerLoansQuery query);
}