package com.bank.loanmanagement.loan.domain.loan;

import com.bank.loan.loan.domain.customer.CustomerId;

import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    
    Loan save(Loan loan);
    
    Optional<Loan> findById(LoanId id);
    
    List<Loan> findByCustomerId(CustomerId customerId);
    
    List<Loan> findByStatus(LoanStatus status);
    
    List<Loan> findByType(LoanType type);
    
    List<Loan> findOverdueLoans();
    
    List<Loan> findAll();
    
    void delete(Loan loan);
}