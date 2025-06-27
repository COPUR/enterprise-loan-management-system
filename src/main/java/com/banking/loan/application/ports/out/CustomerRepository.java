package com.banking.loan.application.ports.out;

import com.banking.loan.domain.shared.Customer;
import com.banking.loan.domain.loan.CustomerId;
import java.util.Optional;
import java.util.List;

/**
 * Outbound Port for Customer persistence (Hexagonal Architecture)
 * Following DDD Repository pattern
 */
public interface CustomerRepository {
    
    /**
     * Save customer aggregate
     */
    Customer save(Customer customer);
    
    /**
     * Find customer by unique identifier
     */
    Optional<Customer> findById(CustomerId customerId);
    
    /**
     * Find customer by email (unique business identifier)
     */
    Optional<Customer> findByEmail(String email);
    
    /**
     * Find customer by national ID
     */
    Optional<Customer> findByNationalId(String nationalId);
    
    /**
     * Find customers requiring KYC verification
     */
    List<Customer> findCustomersRequiringKYC();
    
    /**
     * Check if customer exists
     */
    boolean existsById(CustomerId customerId);
    
    /**
     * Check if email is already registered
     */
    boolean existsByEmail(String email);
}