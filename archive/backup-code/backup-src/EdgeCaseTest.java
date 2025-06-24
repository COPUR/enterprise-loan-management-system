package com.bank.loanmanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EdgeCaseTest {
    
    private Customer customer;
    private Loan loan;
    private Payment payment;
    
    @BeforeEach
    void setUp() {
        customer = new Customer();
        loan = new Loan();
        payment = new Payment();
    }
    
    @Test
    @DisplayName("Should handle minimum boundary loan amounts")
    void shouldHandleMinimumBoundaryLoanAmounts() {
        // Test exact minimum boundary ($1,000)
        loan.setLoanAmount(new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("1000.00"), loan.getLoanAmount());
        
        // Test just below minimum ($999.99)
        loan.setLoanAmount(new BigDecimal("999.99"));
        assertEquals(new BigDecimal("999.99"), loan.getLoanAmount());
        
        // Test just above minimum ($1000.01)
        loan.setLoanAmount(new BigDecimal("1000.01"));
        assertEquals(new BigDecimal("1000.01"), loan.getLoanAmount());
    }
    
    @Test
    @DisplayName("Should handle maximum boundary loan amounts")
    void shouldHandleMaximumBoundaryLoanAmounts() {
        // Test exact maximum boundary ($500,000)
        loan.setLoanAmount(new BigDecimal("500000.00"));
        assertEquals(new BigDecimal("500000.00"), loan.getLoanAmount());
        
        // Test just below maximum ($499,999.99)
        loan.setLoanAmount(new BigDecimal("499999.99"));
        assertEquals(new BigDecimal("499999.99"), loan.getLoanAmount());
        
        // Test just above maximum ($500,000.01)
        loan.setLoanAmount(new BigDecimal("500000.01"));
        assertEquals(new BigDecimal("500000.01"), loan.getLoanAmount());
    }
    
    @Test
    @DisplayName("Should handle minimum boundary interest rates")
    void shouldHandleMinimumBoundaryInterestRates() {
        // Test exact minimum boundary (0.1%)
        loan.setInterestRate(new BigDecimal("0.1"));
        assertEquals(new BigDecimal("0.1"), loan.getInterestRate());
        
        // Test just below minimum (0.09%)
        loan.setInterestRate(new BigDecimal("0.09"));
        assertEquals(new BigDecimal("0.09"), loan.getInterestRate());
        
        // Test just above minimum (0.11%)
        loan.setInterestRate(new BigDecimal("0.11"));
        assertEquals(new BigDecimal("0.11"), loan.getInterestRate());
    }
    
    @Test
    @DisplayName("Should handle maximum boundary interest rates")
    void shouldHandleMaximumBoundaryInterestRates() {
        // Test exact maximum boundary (0.5%)
        loan.setInterestRate(new BigDecimal("0.5"));
        assertEquals(new BigDecimal("0.5"), loan.getInterestRate());
        
        // Test just below maximum (0.49%)
        loan.setInterestRate(new BigDecimal("0.49"));
        assertEquals(new BigDecimal("0.49"), loan.getInterestRate());
        
        // Test just above maximum (0.51%)
        loan.setInterestRate(new BigDecimal("0.51"));
        assertEquals(new BigDecimal("0.51"), loan.getInterestRate());
    }
    
    @Test
    @DisplayName("Should handle credit score boundaries")
    void shouldHandleCreditScoreBoundaries() {
        // Test minimum credit score (300)
        customer.setCreditScore(300);
        assertEquals(300, customer.getCreditScore());
        
        // Test maximum credit score (850)
        customer.setCreditScore(850);
        assertEquals(850, customer.getCreditScore());
        
        // Test just below minimum (299)
        customer.setCreditScore(299);
        assertEquals(299, customer.getCreditScore());
        
        // Test just above maximum (851)
        customer.setCreditScore(851);
        assertEquals(851, customer.getCreditScore());
    }
    
    @Test
    @DisplayName("Should handle extreme decimal precision")
    void shouldHandleExtremeDecimalPrecision() {
        // Test high precision amounts
        loan.setLoanAmount(new BigDecimal("12345.123456789"));
        assertEquals(new BigDecimal("12345.123456789"), loan.getLoanAmount());
        
        payment.setPaymentAmount(new BigDecimal("867.999999999"));
        assertEquals(new BigDecimal("867.999999999"), payment.getPaymentAmount());
        
        // Test scientific notation
        loan.setInterestRate(new BigDecimal("2.5E-1")); // 0.25
        assertEquals(new BigDecimal("2.5E-1"), loan.getInterestRate());
    }
    
    @Test
    @DisplayName("Should handle future and past date boundaries")
    void shouldHandleFuturePastDateBoundaries() {
        LocalDateTime now = LocalDateTime.now();
        
        // Test far future date (100 years)
        LocalDateTime farFuture = now.plusYears(100);
        customer.setCreatedAt(farFuture);
        assertEquals(farFuture, customer.getCreatedAt());
        
        // Test far past date (100 years ago)
        LocalDateTime farPast = now.minusYears(100);
        loan.setCreatedAt(farPast);
        assertEquals(farPast, loan.getCreatedAt());
        
        // Test epoch boundary
        LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
        payment.setPaymentDate(epoch);
        assertEquals(epoch, payment.getPaymentDate());
    }
    
    @Test
    @DisplayName("Should handle very long string inputs")
    void shouldHandleVeryLongStringInputs() {
        // Test extremely long names
        String veryLongName = "A".repeat(1000);
        customer.setFirstName(veryLongName);
        assertEquals(veryLongName, customer.getFirstName());
        
        customer.setLastName(veryLongName);
        assertEquals(veryLongName, customer.getLastName());
        
        // Test very long email
        String longEmail = "user" + "x".repeat(200) + "@domain.com";
        customer.setEmail(longEmail);
        assertEquals(longEmail, customer.getEmail());
        
        // Test very long phone number
        String longPhone = "1".repeat(50);
        customer.setPhoneNumber(longPhone);
        assertEquals(longPhone, customer.getPhoneNumber());
    }
    
    @Test
    @DisplayName("Should handle special characters in inputs")
    void shouldHandleSpecialCharactersInInputs() {
        // Test names with special characters
        String specialChars = "José María O'Connor-Smith";
        customer.setFirstName(specialChars);
        assertEquals(specialChars, customer.getFirstName());
        
        // Test email with plus addressing
        String emailWithPlus = "user+tag@example.com";
        customer.setEmail(emailWithPlus);
        assertEquals(emailWithPlus, customer.getEmail());
        
        // Test international phone numbers
        String intlPhone = "+1-555-123-4567 ext.123";
        customer.setPhoneNumber(intlPhone);
        assertEquals(intlPhone, customer.getPhoneNumber());
    }
    
    @Test
    @DisplayName("Should handle Unicode and emoji inputs")
    void shouldHandleUnicodeEmojiInputs() {
        // Test Unicode names
        customer.setFirstName("王伟");
        assertEquals("王伟", customer.getFirstName());
        
        customer.setLastName("Müller");
        assertEquals("Müller", customer.getLastName());
        
        // Test emoji in names (edge case)
        customer.setFirstName("John 😊");
        assertEquals("John 😊", customer.getFirstName());
        
        // Test mixed Unicode
        customer.setEmail("用户@example.com");
        assertEquals("用户@example.com", customer.getEmail());
    }
    
    @Test
    @DisplayName("Should handle concurrent loan applications")
    void shouldHandleConcurrentLoanApplications() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(100);
        
        // Create 100 concurrent loan applications
        for (int i = 0; i < 100; i++) {
            final int loanId = i;
            executor.submit(() -> {
                try {
                    Loan concurrentLoan = new Loan();
                    concurrentLoan.setLoanId((long) loanId);
                    concurrentLoan.setCustomerId((long) (loanId % 10));
                    concurrentLoan.setLoanAmount(new BigDecimal(10000 + loanId));
                    concurrentLoan.setInterestRate(new BigDecimal("0.25"));
                    concurrentLoan.setInstallments(12);
                    concurrentLoan.setLoanStatus("PENDING");
                    concurrentLoan.setCreatedAt(LocalDateTime.now());
                    
                    // Simulate processing time
                    Thread.sleep(10);
                    
                    assertNotNull(concurrentLoan.getLoanId());
                    assertEquals("PENDING", concurrentLoan.getLoanStatus());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await();
        executor.shutdown();
    }
    
    @Test
    @DisplayName("Should handle rapid payment processing")
    void shouldHandleRapidPaymentProcessing() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(50);
        
        // Process 50 concurrent payments
        for (int i = 0; i < 50; i++) {
            final int paymentId = i;
            executor.submit(() -> {
                try {
                    Payment rapidPayment = new Payment();
                    rapidPayment.setPaymentId((long) paymentId);
                    rapidPayment.setLoanId((long) (paymentId % 5));
                    rapidPayment.setPaymentAmount(new BigDecimal(850 + paymentId));
                    rapidPayment.setPaymentDate(LocalDateTime.now());
                    rapidPayment.setPaymentStatus("PROCESSING");
                    rapidPayment.setPaymentMethod("BANK_TRANSFER");
                    rapidPayment.setInstallmentNumber(paymentId % 24 + 1);
                    
                    // Simulate payment processing
                    Thread.sleep(5);
                    rapidPayment.setPaymentStatus("COMPLETED");
                    
                    assertEquals("COMPLETED", rapidPayment.getPaymentStatus());
                    assertTrue(rapidPayment.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
    }
    
    @Test
    @DisplayName("Should handle installment edge cases")
    void shouldHandleInstallmentEdgeCases() {
        // Test all valid installment periods with edge amounts
        int[] validInstallments = {6, 9, 12, 24};
        BigDecimal[] edgeAmounts = {
            new BigDecimal("1000.00"),    // Minimum
            new BigDecimal("1000.01"),    // Just above minimum
            new BigDecimal("499999.99"),  // Just below maximum
            new BigDecimal("500000.00")   // Maximum
        };
        
        for (int installment : validInstallments) {
            for (BigDecimal amount : edgeAmounts) {
                loan.setInstallments(installment);
                loan.setLoanAmount(amount);
                
                assertEquals(installment, loan.getInstallments());
                assertEquals(amount, loan.getLoanAmount());
                
                // Calculate expected monthly payment
                BigDecimal monthlyRate = new BigDecimal("0.0025"); // 0.25% monthly
                double r = monthlyRate.doubleValue();
                double p = amount.doubleValue();
                int n = installment;
                
                if (r > 0) {
                    double monthlyPayment = p * (r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
                    assertTrue(monthlyPayment > 0, "Monthly payment should be positive");
                    assertTrue(monthlyPayment < p, "Monthly payment should be less than principal");
                }
            }
        }
    }
}