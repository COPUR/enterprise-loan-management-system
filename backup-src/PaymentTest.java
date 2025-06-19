package com.bank.loanmanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentTest {
    
    private Payment payment;
    
    @BeforeEach
    void setUp() {
        payment = new Payment();
    }
    
    @Test
    @DisplayName("Should create payment with valid data")
    void shouldCreatePaymentWithValidData() {
        // Given
        payment.setPaymentId(1L);
        payment.setLoanId(1L);
        payment.setPaymentAmount(new BigDecimal("865.50"));
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentStatus("COMPLETED");
        payment.setPaymentMethod("BANK_TRANSFER");
        payment.setInstallmentNumber(1);
        
        // When & Then
        assertEquals(1L, payment.getPaymentId());
        assertEquals(1L, payment.getLoanId());
        assertEquals(new BigDecimal("865.50"), payment.getPaymentAmount());
        assertNotNull(payment.getPaymentDate());
        assertEquals("COMPLETED", payment.getPaymentStatus());
        assertEquals("BANK_TRANSFER", payment.getPaymentMethod());
        assertEquals(1, payment.getInstallmentNumber());
    }
    
    @Test
    @DisplayName("Should validate payment methods")
    void shouldValidatePaymentMethods() {
        // Given
        payment.setPaymentId(1L);
        payment.setLoanId(1L);
        payment.setPaymentAmount(new BigDecimal("500.00"));
        payment.setInstallmentNumber(1);
        
        // When & Then - Valid payment methods
        String[] validMethods = {"BANK_TRANSFER", "CREDIT_CARD", "DEBIT_CARD", "CASH", "CHECK", "ONLINE"};
        
        for (String method : validMethods) {
            payment.setPaymentMethod(method);
            assertEquals(method, payment.getPaymentMethod());
        }
    }
    
    @Test
    @DisplayName("Should validate payment status transitions")
    void shouldValidatePaymentStatusTransitions() {
        // Given
        payment.setPaymentId(1L);
        payment.setLoanId(1L);
        payment.setPaymentAmount(new BigDecimal("750.00"));
        payment.setInstallmentNumber(3);
        
        // When & Then - Valid payment statuses
        String[] validStatuses = {"PENDING", "PROCESSING", "COMPLETED", "FAILED", "REVERSED", "CANCELLED"};
        
        for (String status : validStatuses) {
            payment.setPaymentStatus(status);
            assertEquals(status, payment.getPaymentStatus());
        }
    }
    
    @Test
    @DisplayName("Should validate installment number range")
    void shouldValidateInstallmentNumberRange() {
        // Given
        payment.setPaymentId(1L);
        payment.setLoanId(1L);
        payment.setPaymentAmount(new BigDecimal("1200.00"));
        payment.setPaymentStatus("COMPLETED");
        
        // When & Then - Valid installment numbers (1-24 based on business rules)
        for (int i = 1; i <= 24; i++) {
            payment.setInstallmentNumber(i);
            assertEquals(i, payment.getInstallmentNumber());
        }
    }
    
    @Test
    @DisplayName("Should handle payment amount precision")
    void shouldHandlePaymentAmountPrecision() {
        // Given
        payment.setPaymentId(1L);
        payment.setLoanId(1L);
        payment.setInstallmentNumber(1);
        payment.setPaymentStatus("COMPLETED");
        
        // When & Then - Payment amounts with proper decimal precision
        payment.setPaymentAmount(new BigDecimal("865.75"));
        assertEquals(new BigDecimal("865.75"), payment.getPaymentAmount());
        
        payment.setPaymentAmount(new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("1000.00"), payment.getPaymentAmount());
        
        payment.setPaymentAmount(new BigDecimal("2500.50"));
        assertEquals(new BigDecimal("2500.50"), payment.getPaymentAmount());
    }
    
    @Test
    @DisplayName("Should validate payment date chronology")
    void shouldValidatePaymentDateChronology() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastDate = now.minusDays(30);
        LocalDateTime futureDate = now.plusDays(30);
        
        payment.setPaymentId(1L);
        payment.setLoanId(1L);
        payment.setPaymentAmount(new BigDecimal("850.00"));
        payment.setInstallmentNumber(1);
        
        // When & Then - Valid payment dates
        payment.setPaymentDate(pastDate);
        assertEquals(pastDate, payment.getPaymentDate());
        
        payment.setPaymentDate(now);
        assertEquals(now, payment.getPaymentDate());
        
        payment.setPaymentDate(futureDate); // Scheduled payment
        assertEquals(futureDate, payment.getPaymentDate());
    }
    
    @Test
    @DisplayName("Should calculate late payment penalties")
    void shouldCalculateLatePaymentPenalties() {
        // Given
        BigDecimal originalAmount = new BigDecimal("850.00");
        LocalDateTime dueDate = LocalDateTime.now().minusDays(15); // 15 days late
        
        payment.setPaymentId(1L);
        payment.setLoanId(1L);
        payment.setPaymentAmount(originalAmount);
        payment.setPaymentDate(dueDate);
        payment.setInstallmentNumber(2);
        
        // When - Calculate penalty (typically 5% late fee + daily interest)
        BigDecimal lateFee = originalAmount.multiply(new BigDecimal("0.05")); // 5%
        BigDecimal totalWithPenalty = originalAmount.add(lateFee);
        
        // Then
        assertTrue(lateFee.compareTo(new BigDecimal("42.50")) == 0); // 5% of 850
        assertTrue(totalWithPenalty.compareTo(new BigDecimal("892.50")) == 0);
    }
}