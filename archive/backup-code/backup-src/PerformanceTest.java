package com.bank.loanmanagement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class PerformanceTest {
    
    private static final int PERFORMANCE_THRESHOLD_MS = 1000;
    private static final int LOAD_TEST_CUSTOMERS = 10000;
    private static final int CONCURRENT_THREADS = 20;
    
    @Test
    @DisplayName("Should process customer creation within performance threshold")
    void shouldProcessCustomerCreationWithinThreshold() {
        long startTime = System.currentTimeMillis();
        
        // Create 1000 customers
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Customer customer = new Customer();
            customer.setCustomerId((long) i);
            customer.setFirstName("Customer" + i);
            customer.setLastName("Test" + i);
            customer.setEmail("customer" + i + "@bank.com");
            customer.setCreditScore(300 + (i % 550));
            customer.setMonthlyIncome(new BigDecimal(2000 + (i % 50000)));
            customer.setPhoneNumber("555" + String.format("%07d", i));
            customer.setCreatedAt(LocalDateTime.now());
            customers.add(customer);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < PERFORMANCE_THRESHOLD_MS, 
            "Customer creation took " + duration + "ms, should be under " + PERFORMANCE_THRESHOLD_MS + "ms");
        assertEquals(1000, customers.size());
    }
    
    @Test
    @DisplayName("Should process loan calculations within performance threshold")
    void shouldProcessLoanCalculationsWithinThreshold() {
        long startTime = System.currentTimeMillis();
        
        // Process 5000 loan calculations
        List<BigDecimal> monthlyPayments = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            BigDecimal principal = new BigDecimal(10000 + (i * 10));
            BigDecimal monthlyRate = new BigDecimal("0.0025"); // 0.25%
            int months = (i % 4 == 0) ? 6 : (i % 4 == 1) ? 9 : (i % 4 == 2) ? 12 : 24;
            
            // Calculate monthly payment: PMT = P[r(1+r)^n]/[(1+r)^n-1]
            double r = monthlyRate.doubleValue();
            double p = principal.doubleValue();
            int n = months;
            
            double payment = p * (r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
            monthlyPayments.add(BigDecimal.valueOf(Math.round(payment * 100.0) / 100.0));
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < PERFORMANCE_THRESHOLD_MS, 
            "Loan calculations took " + duration + "ms, should be under " + PERFORMANCE_THRESHOLD_MS + "ms");
        assertEquals(5000, monthlyPayments.size());
    }
    
    @Test
    @DisplayName("Should handle concurrent customer operations efficiently")
    void shouldHandleConcurrentCustomerOperationsEfficiently() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<Customer> results = Collections.synchronizedList(new ArrayList<>());
        
        long startTime = System.currentTimeMillis();
        
        // Submit 1000 concurrent customer creation tasks
        for (int i = 0; i < 1000; i++) {
            final int customerId = i;
            executor.submit(() -> {
                Customer customer = new Customer();
                customer.setCustomerId((long) customerId);
                customer.setFirstName("Concurrent" + customerId);
                customer.setLastName("Customer" + customerId);
                customer.setEmail("concurrent" + customerId + "@bank.com");
                customer.setCreditScore(300 + (customerId % 550));
                customer.setMonthlyIncome(new BigDecimal(3000 + customerId));
                customer.setCreatedAt(LocalDateTime.now());
                
                // Simulate some processing time
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                results.add(customer);
            });
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Operations should complete within 5 seconds");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 5000, "Concurrent operations took " + duration + "ms, should be under 5000ms");
        assertEquals(1000, results.size());
    }
    
    @Test
    @DisplayName("Should handle large payment batch processing")
    void shouldHandleLargePaymentBatchProcessing() {
        long startTime = System.currentTimeMillis();
        
        List<Payment> payments = new ArrayList<>();
        
        // Process 2000 payments
        for (int i = 0; i < 2000; i++) {
            Payment payment = new Payment();
            payment.setPaymentId((long) i);
            payment.setLoanId((long) (i % 100));
            payment.setPaymentAmount(new BigDecimal(800 + (i % 200)));
            payment.setPaymentDate(LocalDateTime.now().minusDays(i % 30));
            payment.setPaymentStatus("COMPLETED");
            payment.setPaymentMethod("BANK_TRANSFER");
            payment.setInstallmentNumber((i % 24) + 1);
            
            // Simulate payment validation
            if (payment.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0 &&
                payment.getInstallmentNumber() > 0 && payment.getInstallmentNumber() <= 24) {
                payments.add(payment);
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < PERFORMANCE_THRESHOLD_MS, 
            "Payment batch processing took " + duration + "ms, should be under " + PERFORMANCE_THRESHOLD_MS + "ms");
        assertEquals(2000, payments.size());
    }
    
    @Test
    @DisplayName("Should maintain performance under memory pressure")
    void shouldMaintainPerformanceUnderMemoryPressure() {
        long startTime = System.currentTimeMillis();
        
        // Create large data structures to simulate memory pressure
        List<List<Customer>> customerBatches = new ArrayList<>();
        
        for (int batch = 0; batch < 10; batch++) {
            List<Customer> batchCustomers = new ArrayList<>();
            
            for (int i = 0; i < 1000; i++) {
                Customer customer = new Customer();
                customer.setCustomerId((long) (batch * 1000 + i));
                customer.setFirstName("Batch" + batch + "Customer" + i);
                customer.setLastName("LoadTest" + i);
                customer.setEmail("batch" + batch + "customer" + i + "@bank.com");
                customer.setCreditScore(300 + (i % 550));
                customer.setMonthlyIncome(new BigDecimal(2000 + i));
                customer.setCreatedAt(LocalDateTime.now());
                batchCustomers.add(customer);
            }
            
            customerBatches.add(batchCustomers);
        }
        
        // Process all batches
        long totalCustomers = customerBatches.stream()
            .mapToLong(List::size)
            .sum();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 2000, "Memory pressure test took " + duration + "ms, should be under 2000ms");
        assertEquals(10000, totalCustomers);
    }
    
    @Test
    @DisplayName("Should handle rapid loan status transitions")
    void shouldHandleRapidLoanStatusTransitions() {
        long startTime = System.currentTimeMillis();
        
        String[] statuses = {"PENDING", "UNDER_REVIEW", "APPROVED", "ACTIVE", "COMPLETED"};
        List<Loan> loans = new ArrayList<>();
        
        // Create 1000 loans and transition them through statuses
        for (int i = 0; i < 1000; i++) {
            Loan loan = new Loan();
            loan.setLoanId((long) i);
            loan.setCustomerId((long) (i % 100));
            loan.setLoanAmount(new BigDecimal(10000 + (i * 100)));
            loan.setInterestRate(new BigDecimal("0.25"));
            loan.setInstallments((i % 4 == 0) ? 6 : (i % 4 == 1) ? 9 : (i % 4 == 2) ? 12 : 24);
            loan.setCreatedAt(LocalDateTime.now());
            
            // Transition through all statuses
            for (String status : statuses) {
                loan.setLoanStatus(status);
                // Simulate processing time for status change
                if (i % 100 == 0) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            
            loans.add(loan);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 1500, "Status transitions took " + duration + "ms, should be under 1500ms");
        assertEquals(1000, loans.size());
        assertEquals("COMPLETED", loans.get(999).getLoanStatus()); // Last status should be COMPLETED
    }
    
    @Test
    @DisplayName("Should maintain response time under sustained load")
    void shouldMaintainResponseTimeUnderSustainedLoad() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
        
        // Run 100 operations with sustained load
        for (int i = 0; i < 100; i++) {
            final int operationId = i;
            executor.submit(() -> {
                long operationStart = System.currentTimeMillis();
                
                // Simulate complex loan processing
                Loan loan = new Loan();
                loan.setLoanId((long) operationId);
                loan.setCustomerId((long) (operationId % 50));
                loan.setLoanAmount(new BigDecimal(15000 + (operationId * 500)));
                loan.setInterestRate(new BigDecimal("0.3"));
                loan.setInstallments(12);
                
                // Calculate payment schedule
                BigDecimal monthlyRate = loan.getInterestRate().divide(new BigDecimal("100"));
                for (int month = 1; month <= loan.getInstallments(); month++) {
                    Payment payment = new Payment();
                    payment.setPaymentId((long) (operationId * 100 + month));
                    payment.setLoanId(loan.getLoanId());
                    payment.setInstallmentNumber(month);
                    payment.setPaymentAmount(new BigDecimal("1350.75")); // Simplified calculation
                    payment.setPaymentStatus("SCHEDULED");
                }
                
                long operationEnd = System.currentTimeMillis();
                responseTimes.add(operationEnd - operationStart);
            });
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        
        // Check that 95% of operations completed within acceptable time
        responseTimes.sort(Long::compareTo);
        int percentile95Index = (int) (responseTimes.size() * 0.95);
        long percentile95Time = responseTimes.get(percentile95Index - 1);
        
        assertTrue(percentile95Time < 100, "95th percentile response time: " + percentile95Time + "ms, should be under 100ms");
        assertEquals(100, responseTimes.size());
    }
}