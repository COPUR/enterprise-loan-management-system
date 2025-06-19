package com.bank.loanmanagement.customermanagement.domain.port.out;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;
import com.bank.loanmanagement.customermanagement.domain.model.EmailAddress;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Customer aggregate.
 * Defines persistence operations without implementation details.
 */
public interface CustomerRepository {
    
    /**
     * Save a customer (create or update).
     */
    Customer save(Customer customer);
    
    /**
     * Find customer by unique identifier.
     */
    Optional<Customer> findById(CustomerId customerId);
    
    /**
     * Find customer by email address.
     */
    Optional<Customer> findByEmail(EmailAddress email);
    
    /**
     * Check if customer exists with given email.
     */
    boolean existsByEmail(EmailAddress email);
    
    /**
     * Get all customers with pagination.
     */
    List<Customer> findAll(int page, int size, String sortBy, String sortDirection);
    
    /**
     * Count total number of customers.
     */
    long count();
    
    /**
     * Delete customer by ID.
     */
    void deleteById(CustomerId customerId);
    
    /**
     * Health check for repository connectivity.
     */
    void healthCheck();
}