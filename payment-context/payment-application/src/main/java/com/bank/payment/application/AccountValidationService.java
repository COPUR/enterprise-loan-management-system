package com.bank.payment.application;

import com.bank.payment.domain.AccountId;
import com.bank.shared.kernel.domain.Money;

/**
 * Port interface for account validation operations
 * 
 * Follows hexagonal architecture - this is a port that will be implemented
 * by an adapter in the infrastructure layer
 */
public interface AccountValidationService {
    
    /**
     * Validate that an account exists and is active
     */
    boolean validateAccount(AccountId accountId);
    
    /**
     * Check if account has sufficient balance for payment
     */
    boolean hasBalance(AccountId accountId, Money amount);
    
    /**
     * Get account balance
     */
    Money getAccountBalance(AccountId accountId);
    
    /**
     * Check if account can accept incoming payments
     */
    boolean canReceivePayments(AccountId accountId);
    
    /**
     * Check if account can send outgoing payments
     */
    boolean canSendPayments(AccountId accountId);
}