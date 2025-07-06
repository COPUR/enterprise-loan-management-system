
package com.bank.loanmanagement.domain.port.out;

import com.bank.loanmanagement.domain.model.Customer;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Customer aggregate persistence.
 * Defines the contract for customer data operations.
 */
public interface CustomerRepository {
    
    /**
     * Saves a customer entity.
     * @param customer Customer to save
     * @return Saved customer with assigned ID
     */
    Customer save(Customer customer);
    
    /**
     * Finds a customer by their unique identifier.
     * @param customerId Customer identifier
     * @return Customer if found, empty otherwise
     */
    Optional<Customer> findById(Long customerId);
    
    /**
     * Finds a customer by their email address.
     * @param email Customer email
     * @return Customer if found, empty otherwise
     */
    Optional<Customer> findByEmail(String email);
    
    /**
     * Finds all customers with pagination support.
     * @param page Page number (0-based)
     * @param size Page size
     * @return List of customers
     */
    List<Customer> findAll(int page, int size);
    
    /**
     * Checks if a customer exists with the given email.
     * @param email Email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Deletes a customer by their identifier.
     * @param customerId Customer identifier
     */
    void deleteById(Long customerId);
    
    /**
     * Gets the total count of customers.
     * @return Total number of customers
     */
    long count();
}
