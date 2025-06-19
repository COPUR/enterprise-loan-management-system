package com.bank.loanmanagement.customermanagement.domain.port.in;

import com.bank.loanmanagement.customermanagement.domain.model.Customer;
import com.bank.loanmanagement.customermanagement.domain.model.CustomerId;
import com.bank.loanmanagement.sharedkernel.domain.Money;

import java.util.List;
import java.util.Optional;

/**
 * Use case interface for customer management operations.
 * Defines the application's business operations for customer domain.
 */
public interface CustomerManagementUseCase {
    
    /**
     * Create a new customer with initial credit limit.
     */
    Customer createCustomer(CreateCustomerCommand command);
    
    /**
     * Update existing customer information.
     */
    Customer updateCustomer(UpdateCustomerCommand command);
    
    /**
     * Find customer by ID.
     */
    Optional<Customer> findCustomer(FindCustomerQuery query);
    
    /**
     * Get all customers with pagination.
     */
    List<Customer> getAllCustomers(GetAllCustomersQuery query);
    
    /**
     * Activate a pending customer account.
     */
    void activateCustomer(ActivateCustomerCommand command);
    
    /**
     * Suspend an active customer account.
     */
    void suspendCustomer(SuspendCustomerCommand command);
    
    /**
     * Reserve credit for a customer (e.g., for loan approval).
     */
    void reserveCredit(ReserveCreditCommand command);
    
    /**
     * Release previously reserved credit.
     */
    void releaseCredit(ReleaseCreditCommand command);
    
    /**
     * Update customer's credit limit.
     */
    void updateCreditLimit(UpdateCreditLimitCommand command);
    
    /**
     * Check if customer is eligible for a loan of specific amount.
     */
    boolean checkLoanEligibility(CheckLoanEligibilityQuery query);
}