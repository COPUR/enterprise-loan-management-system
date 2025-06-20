package com.bank.loanmanagement.application.port.in;

import com.bank.loanmanagement.domain.loan.Loan;
import com.bank.loanmanagement.domain.loan.LoanId;
import com.bank.loanmanagement.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

/**
 * Port for querying loan data
 * Following hexagonal architecture principles
 */
public interface LoanQueryPort {
    
    Optional<Loan> findById(LoanId loanId);
    
    List<Loan> findAll();
    
    List<Loan> findByCustomerId(CustomerId customerId);
    
    List<Loan> findByStatus(String status);
    
    List<Loan> findOverdueLoans();
}