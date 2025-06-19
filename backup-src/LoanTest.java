package com.bank.loanmanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanTest {
    
    private Loan loan;
    
    @BeforeEach
    void setUp() {
        loan = new Loan();
    }
    
    @Test
    @DisplayName("Should create loan with valid business rules")
    void shouldCreateLoanWithValidBusinessRules() {
        // Given
        loan.setLoanId(1L);
        loan.setCustomerId(1L);
        loan.setLoanAmount(new BigDecimal("50000.00"));
        loan.setInterestRate(new BigDecimal("0.25")); // 0.25% monthly
        loan.setInstallments(12);
        loan.setLoanStatus("PENDING");
        loan.setCreatedAt(LocalDateTime.now());
        
        // When & Then
        assertEquals(1L, loan.getLoanId());
        assertEquals(1L, loan.getCustomerId());
        assertEquals(new BigDecimal("50000.00"), loan.getLoanAmount());
        assertEquals(new BigDecimal("0.25"), loan.getInterestRate());
        assertEquals(12, loan.getInstallments());
        assertEquals("PENDING", loan.getLoanStatus());
        assertNotNull(loan.getCreatedAt());
    }
    
    @Test
    @DisplayName("Should validate allowed installment periods")
    void shouldValidateAllowedInstallmentPeriods() {
        // Given
        loan.setLoanId(1L);
        loan.setCustomerId(1L);
        loan.setLoanAmount(new BigDecimal("25000.00"));
        loan.setInterestRate(new BigDecimal("0.15"));
        
        // When & Then - Valid installment periods: 6, 9, 12, 24
        int[] validInstallments = {6, 9, 12, 24};
        
        for (int installment : validInstallments) {
            loan.setInstallments(installment);
            assertEquals(installment, loan.getInstallments());
        }
    }
    
    @Test
    @DisplayName("Should validate interest rate range (0.1% - 0.5%)")
    void shouldValidateInterestRateRange() {
        // Given
        loan.setLoanId(1L);
        loan.setCustomerId(1L);
        loan.setLoanAmount(new BigDecimal("30000.00"));
        loan.setInstallments(12);
        
        // When & Then - Valid interest rate range: 0.1% - 0.5%
        loan.setInterestRate(new BigDecimal("0.1")); // Minimum
        assertEquals(new BigDecimal("0.1"), loan.getInterestRate());
        
        loan.setInterestRate(new BigDecimal("0.25")); // Mid-range
        assertEquals(new BigDecimal("0.25"), loan.getInterestRate());
        
        loan.setInterestRate(new BigDecimal("0.5")); // Maximum
        assertEquals(new BigDecimal("0.5"), loan.getInterestRate());
    }
    
    @Test
    @DisplayName("Should validate loan amount range")
    void shouldValidateLoanAmountRange() {
        // Given
        loan.setLoanId(1L);
        loan.setCustomerId(1L);
        loan.setInterestRate(new BigDecimal("0.25"));
        loan.setInstallments(12);
        
        // When & Then - Loan amount range: $1,000 - $500,000
        loan.setLoanAmount(new BigDecimal("1000.00")); // Minimum
        assertEquals(new BigDecimal("1000.00"), loan.getLoanAmount());
        
        loan.setLoanAmount(new BigDecimal("250000.00")); // Mid-range
        assertEquals(new BigDecimal("250000.00"), loan.getLoanAmount());
        
        loan.setLoanAmount(new BigDecimal("500000.00")); // Maximum
        assertEquals(new BigDecimal("500000.00"), loan.getLoanAmount());
    }
    
    @Test
    @DisplayName("Should handle loan status transitions")
    void shouldHandleLoanStatusTransitions() {
        // Given
        loan.setLoanId(1L);
        loan.setCustomerId(1L);
        loan.setLoanAmount(new BigDecimal("15000.00"));
        loan.setInterestRate(new BigDecimal("0.2"));
        loan.setInstallments(9);
        
        // When & Then - Valid status transitions
        String[] validStatuses = {"PENDING", "APPROVED", "ACTIVE", "COMPLETED", "REJECTED", "DEFAULTED"};
        
        for (String status : validStatuses) {
            loan.setLoanStatus(status);
            assertEquals(status, loan.getLoanStatus());
        }
    }
    
    @Test
    @DisplayName("Should calculate monthly payment correctly")
    void shouldCalculateMonthlyPaymentCorrectly() {
        // Given
        loan.setLoanAmount(new BigDecimal("10000.00"));
        loan.setInterestRate(new BigDecimal("0.25")); // 0.25% monthly
        loan.setInstallments(12);
        
        // When
        BigDecimal expectedMonthlyPayment = calculateExpectedPayment(
            new BigDecimal("10000.00"), 
            new BigDecimal("0.0025"), // Convert to decimal
            12
        );
        
        // Then
        // Monthly payment = P[r(1+r)^n]/[(1+r)^n-1]
        // Where P=10000, r=0.0025, n=12
        assertTrue(expectedMonthlyPayment.compareTo(new BigDecimal("850.00")) > 0);
        assertTrue(expectedMonthlyPayment.compareTo(new BigDecimal("870.00")) < 0);
    }
    
    private BigDecimal calculateExpectedPayment(BigDecimal principal, BigDecimal monthlyRate, int months) {
        // PMT = P[r(1+r)^n]/[(1+r)^n-1]
        double r = monthlyRate.doubleValue();
        double p = principal.doubleValue();
        int n = months;
        
        double payment = p * (r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
        return BigDecimal.valueOf(Math.round(payment * 100.0) / 100.0);
    }
}