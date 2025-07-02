
package com.bank.loanmanagement.loan.domain.port.in;

import com.bank.loanmanagement.loan.domain.model.Customer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Use case interface for customer management operations.
 * Defines the contract for customer-related business operations.
 */
public interface CustomerManagementUseCase {
    
    /**
     * Creates a new customer account.
     * @param command Customer creation details
     * @return Created customer
     */
    Customer createCustomer(CreateCustomerCommand command);
    
    /**
     * Updates an existing customer's information.
     * @param command Customer update details
     * @return Updated customer
     */
    Customer updateCustomer(UpdateCustomerCommand command);
    
    /**
     * Retrieves a customer by their unique identifier.
     * @param customerId Customer identifier
     * @return Customer if found, empty otherwise
     */
    Optional<Customer> getCustomerById(Long customerId);
    
    /**
     * Retrieves a customer by their email address.
     * @param email Customer email
     * @return Customer if found, empty otherwise
     */
    Optional<Customer> getCustomerByEmail(String email);
    
    /**
     * Retrieves all customers with pagination support.
     * @param page Page number (0-based)
     * @param size Page size
     * @return List of customers
     */
    List<Customer> getAllCustomers(int page, int size);
    
    /**
     * Reserves credit for a customer (used during loan approval).
     * @param customerId Customer identifier
     * @param amount Amount to reserve
     */
    void reserveCustomerCredit(Long customerId, BigDecimal amount);
    
    /**
     * Releases previously reserved credit for a customer.
     * @param customerId Customer identifier
     * @param amount Amount to release
     */
    void releaseCustomerCredit(Long customerId, BigDecimal amount);
    
    /**
     * Checks if a customer is eligible for a loan of specified amount.
     * @param customerId Customer identifier
     * @param loanAmount Requested loan amount
     * @return true if eligible, false otherwise
     */
    boolean isCustomerEligibleForLoan(Long customerId, BigDecimal loanAmount);
    
    /**
     * Updates customer's credit score.
     * @param customerId Customer identifier
     * @param newCreditScore New credit score
     */
    void updateCustomerCreditScore(Long customerId, Integer newCreditScore);
    
    /**
     * Deactivates a customer account.
     * @param customerId Customer identifier
     */
    void deactivateCustomer(Long customerId);
    
    /**
     * Reactivates a customer account.
     * @param customerId Customer identifier
     */
    void reactivateCustomer(Long customerId);
}
