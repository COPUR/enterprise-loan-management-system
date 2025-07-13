package com.bank.loan.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Result object representing the outcome of a payment operation
 */
@Getter
@AllArgsConstructor
@ToString
public class PaymentResult {
    
    private final int installmentsPaid;
    private final Money totalAmountSpent;
    private final boolean isLoanFullyPaid;
    
    /**
     * Create a result for a successful payment
     */
    public static PaymentResult success(int installmentsPaid, Money totalAmountSpent, boolean isLoanFullyPaid) {
        return new PaymentResult(installmentsPaid, totalAmountSpent, isLoanFullyPaid);
    }
    
    /**
     * Create a result for a failed payment
     */
    public static PaymentResult failure() {
        return new PaymentResult(0, Money.zero("USD"), false);
    }
    
    /**
     * Check if any installments were paid
     */
    public boolean hasPayments() {
        return installmentsPaid > 0;
    }
    
    /**
     * Check if payment was successful
     */
    public boolean isSuccessful() {
        return installmentsPaid > 0 || totalAmountSpent.isPositive();
    }
}