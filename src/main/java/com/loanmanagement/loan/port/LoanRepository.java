package com.loanmanagement.loan.port;

import com.loanmanagement.loan.domain.model.aggregate.Loan;
import com.loanmanagement.loan.domain.model.value.LoanId;
import java.util.Optional;
import java.util.List;

/**
 * Port interface for Loan persistence operations.
 * Follows Hexagonal Architecture by defining the domain's persistence contract.
 */
public interface LoanRepository {

    /**
     * Saves a loan aggregate to the persistence store.
     */
    Loan save(Loan loan);

    /**
     * Finds a loan by its unique identifier.
     */
    Optional<Loan> findById(Long id);

    /**
     * Finds all loans for a specific customer.
     */
    List<Loan> findByCustomerId(Long customerId);

    /**
     * Finds loans for a customer with optional filters.
     */
    List<Loan> findByCustomerIdWithFilters(Long customerId, Integer numberOfInstallments, Boolean isPaid);

    /**
     * Generates the next unique loan identifier.
     * Following DDD principles for identity generation.
     */
    LoanId nextId();
}