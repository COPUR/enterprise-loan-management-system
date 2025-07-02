package com.bank.loanmanagement.loan.application.ports.out;

import com.bank.loanmanagement.loan.domain.loan.LoanAggregate;
import com.bank.loanmanagement.loan.domain.loan.LoanId;
import com.bank.loan.loan.domain.customer.CustomerId;
import java.util.List;
import java.util.Optional;

/**
 * Outbound Port for Loan persistence (Hexagonal Architecture)
 * This interface defines the contract for loan data access
 * Implementation will be in infrastructure layer
 */
public interface LoanRepository {
    
    /**
     * Save a loan aggregate and return the saved entity
     */
    LoanAggregate save(LoanAggregate loanAggregate);
    
    /**
     * Find loan by its unique identifier
     */
    Optional<LoanAggregate> findById(LoanId loanId);
    
    /**
     * Find all loans for a specific customer
     */
    List<LoanAggregate> findByCustomerId(CustomerId customerId);
    
    /**
     * Find loans by status
     */
    List<LoanAggregate> findByStatus(String status);
    
    /**
     * Check if loan exists
     */
    boolean existsById(LoanId loanId);
    
    /**
     * Delete loan (soft delete for audit purposes)
     */
    void deleteById(LoanId loanId);
    
    /**
     * Get total count of loans for reporting
     */
    long count();
    
    /**
     * Find loans requiring action (business query)
     */
    List<LoanAggregate> findLoansRequiringAction();
}