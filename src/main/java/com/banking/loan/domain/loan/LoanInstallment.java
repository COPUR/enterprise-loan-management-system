package com.banking.loan.domain.loan;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * LoanInstallment value object following DDD principles
 * Encapsulates installment payment information and business rules
 */
public record LoanInstallment(
    int paymentNumber,
    LocalDate paymentDate,
    BigDecimal totalPayment,
    BigDecimal principalPayment,
    BigDecimal interestPayment,
    BigDecimal remainingBalance
) {
    /**
     * Factory method for creating installments
     * Following DDD factory pattern
     */
    public static LoanInstallment create(int number, LocalDate paymentDate, BigDecimal totalPayment, 
                                       BigDecimal principalPayment, BigDecimal interestPayment, 
                                       BigDecimal remainingBalance) {
        return new LoanInstallment(number, paymentDate, totalPayment, principalPayment, 
                                 interestPayment, remainingBalance);
    }
    
    /**
     * Get payment date for schedule calculations
     */
    public LocalDate getPaymentDate() {
        return paymentDate;
    }
    
    /**
     * Get total payment amount
     */
    public BigDecimal getTotalPayment() {
        return totalPayment;
    }
    
    /**
     * Get principal payment amount
     */
    public BigDecimal getPrincipalPayment() {
        return principalPayment;
    }
    
    /**
     * Get interest payment amount
     */
    public BigDecimal getInterestPayment() {
        return interestPayment;
    }
    
    /**
     * Check if this installment is paid
     * In a real implementation, this would check against payment records
     */
    public boolean isPaid() {
        // Simplified implementation for compilation
        // In real scenario, would check payment status from payment records
        return false;
    }
}