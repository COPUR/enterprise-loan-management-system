// test/domain/model/entity/LoanTest.java
package com.loanmanagement.loan.model.entity;

import com.loanmanagement.loan.domain.aggregate.Loan;
import com.loanmanagement.domain.model.value.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

class LoanTest {
    
    @Test
    void shouldCalculateTotalLoanAmountCorrectly() {
        // Given
        Money principal = new Money(new BigDecimal("10000"));
        InterestRate rate = new InterestRate(new BigDecimal("0.2"));
        InstallmentCount count = new InstallmentCount(12);
        
        // When
        Loan loan = Loan.create(1L, 1L, principal, rate, count);
        
        // Then
        assertThat(loan.getLoanAmount().getValue())
            .isEqualByComparingTo(new BigDecimal("12000.00"));
    }
    
    @Test
    void shouldGenerateCorrectNumberOfInstallments() {
        // Given
        Money principal = new Money(new BigDecimal("10000"));
        InterestRate rate = new InterestRate(new BigDecimal("0.2"));
        InstallmentCount count = new InstallmentCount(12);
        
        // When
        Loan loan = Loan.create(1L, 1L, principal, rate, count);
        
        // Then
        assertThat(loan.getInstallments()).hasSize(12);
    }
    
    @Test
    void shouldCalculateEarlyPaymentDiscountCorrectly() {
        // Given
        Money principal = new Money(new BigDecimal("12000"));
        InterestRate rate = new InterestRate(new BigDecimal("0.1"));
        InstallmentCount count = new InstallmentCount(6);
        Loan loan = Loan.create(1L, 1L, principal, rate, count);
        
        LocalDate paymentDate = LocalDate.now();
        Money paymentAmount = new Money(new BigDecimal("2200")); // Enough for one installment
        
        // When
        PaymentResult result = loan.makePayment(paymentAmount, paymentDate);
        
        // Then
        assertThat(result.getInstallmentsPaid()).isEqualTo(1);
        assertThat(result.getTotalAmountSpent().getValue()).isLessThan(new BigDecimal("2200"));
    }
}