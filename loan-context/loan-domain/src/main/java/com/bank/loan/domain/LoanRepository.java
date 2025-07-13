package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Loan aggregate
 * 
 * This is a domain service interface that will be implemented
 * by the infrastructure layer following DDD patterns
 */
public interface LoanRepository {
    
    /**
     * Save a loan aggregate
     */
    Loan save(Loan loan);
    
    /**
     * Find loan by ID
     */
    Optional<Loan> findById(LoanId loanId);
    
    /**
     * Find all loans for a customer
     */
    List<Loan> findByCustomerId(CustomerId customerId);
    
    /**
     * Find loans by status
     */
    List<Loan> findByStatus(LoanStatus status);
    
    /**
     * Check if loan exists
     */
    boolean existsById(LoanId loanId);
    
    /**
     * Delete a loan
     */
    void delete(Loan loan);
    
    /**
     * Find overdue loans
     */
    List<Loan> findOverdueLoans();
}