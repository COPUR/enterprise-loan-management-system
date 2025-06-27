package com.banking.loan.application.ports.in;

import com.banking.loan.application.commands.*;
import com.banking.loan.application.queries.*;
import com.banking.loan.application.results.*;

/**
 * Customer Management Use Case (Hexagonal Architecture - Inbound Port)
 * Defines customer-related business operations
 */
public interface CustomerManagementUseCase {
    
    /**
     * Create a new customer
     */
    CustomerCreationResult createCustomer(CreateCustomerCommand command);
    
    /**
     * Update customer information
     */
    CustomerUpdateResult updateCustomer(UpdateCustomerCommand command);
    
    /**
     * Get customer details
     */
    CustomerDetails getCustomerDetails(GetCustomerDetailsQuery query);
    
    /**
     * Perform KYC verification
     */
    KYCVerificationResult performKYCVerification(PerformKYCCommand command);
    
    /**
     * Block customer
     */
    CustomerBlockResult blockCustomer(BlockCustomerCommand command);
}