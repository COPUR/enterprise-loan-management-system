package com.banking.loan.domain.loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * PaymentSchedule value object following DDD principles
 * Encapsulates payment schedule business logic and invariants
 */
public record PaymentSchedule(
    LocalDate createdDate,
    int numberOfPayments,
    BigDecimal monthlyPayment,
    BigDecimal totalPrincipal,
    List<LoanInstallment> installments
) {
    public static PaymentSchedule monthly(LocalDate startDate, List<LoanInstallment> installments) {
        BigDecimal totalPrincipal = installments.stream()
            .map(LoanInstallment::getPrincipalPayment)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal monthlyPayment = installments.isEmpty() ? BigDecimal.ZERO : 
            installments.get(0).getTotalPayment();
        
        return new PaymentSchedule(startDate, installments.size(), monthlyPayment, totalPrincipal, installments);
    }
    
    /**
     * Get first payment date - follows DDD encapsulation principles
     */
    public LocalDate getFirstPaymentDate() {
        return installments.isEmpty() ? createdDate.plusMonths(1) : installments.get(0).getPaymentDate();
    }
    
    /**
     * Get monthly installment amount
     */
    public BigDecimal getMonthlyInstallment() {
        return monthlyPayment;
    }
    
    /**
     * Calculate total payable amount including interest
     */
    public BigDecimal getTotalPayableAmount() {
        return installments.stream()
            .map(LoanInstallment::getTotalPayment)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}