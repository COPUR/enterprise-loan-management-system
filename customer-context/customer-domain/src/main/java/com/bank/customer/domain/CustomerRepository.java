package com.bank.customer.domain;

import com.bank.shared.kernel.domain.CustomerId;
import java.util.Optional;

/**
 * Repository interface for Customer aggregate root
 * 
 * This is a domain interface that will be implemented in the infrastructure layer.
 * It follows the Repository pattern from Domain-Driven Design.
 */
public interface CustomerRepository {
    
    /**
     * Save a customer
     * @param customer the customer to save
     * @return the saved customer
     */
    Customer save(Customer customer);
    
    /**
     * Find a customer by their ID
     * @param customerId the customer identifier
     * @return the customer if found
     */
    Optional<Customer> findById(CustomerId customerId);
    
    /**
     * Find a customer by their email address
     * @param email the email address
     * @return the customer if found
     */
    Optional<Customer> findByEmail(String email);
    
    /**
     * Check if a customer exists with the given ID
     * @param customerId the customer identifier
     * @return true if the customer exists
     */
    boolean existsById(CustomerId customerId);
    
    /**
     * Check if a customer exists with the given email
     * @param email the email address
     * @return true if a customer with this email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Delete a customer
     * @param customerId the customer identifier
     */
    void deleteById(CustomerId customerId);
}