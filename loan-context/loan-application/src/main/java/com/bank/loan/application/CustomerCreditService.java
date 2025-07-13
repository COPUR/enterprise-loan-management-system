package com.bank.loan.application;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;

/**
 * Port interface for customer credit operations
 * 
 * Follows hexagonal architecture - this is a port that will be implemented
 * by an adapter in the infrastructure layer
 */
public interface CustomerCreditService {
    
    /**
     * Check if customer has available credit for the requested amount
     */
    boolean hasAvailableCredit(CustomerId customerId, Money amount);
    
    /**
     * Reserve credit for a customer (when loan is disbursed)
     */
    boolean reserveCredit(CustomerId customerId, Money amount);
    
    /**
     * Release reserved credit (when loan is paid off)
     */
    boolean releaseCredit(CustomerId customerId, Money amount);
    
    /**
     * Get customer's available credit limit
     */
    Money getAvailableCredit(CustomerId customerId);
}