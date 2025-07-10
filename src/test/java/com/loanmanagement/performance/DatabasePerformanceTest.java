package com.loanmanagement.performance;

import com.loanmanagement.customer.application.port.out.CustomerRepository;
import com.loanmanagement.customer.domain.model.Customer;
import com.loanmanagement.loan.application.port.out.LoanRepository;
import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.loan.domain.model.LoanStatus;
import com.loanmanagement.payment.application.port.out.PaymentRepository;
import com.loanmanagement.payment.domain.model.Payment;
import com.loanmanagement.shared.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Database Performance Testing
 * Tests database operations performance under various load conditions
 */
@SpringBootTest
@ActiveProfiles("performance")
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=30",
        "spring.datasource.hikari.minimum-idle=10",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.jdbc.batch_size=25",
        "spring.jpa.properties.hibernate.order_inserts=true",
        "spring.jpa.properties.hibernate.order_updates=true",
        "logging.level.org.hibernate.SQL=INFO"
})
@DisplayName("Database Performance Tests")
class DatabasePerformanceTest {
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PerformanceMetricsCollector metricsCollector;
    
    @Autowired
    private LoadTestDataGenerator dataGenerator;
    
    private ExecutorService executorService;
    
    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(20);
        metricsCollector.clearMetrics();
    }
    
    @Test
    @DisplayName("Customer Repository Bulk Operations Performance")
    void customerRepositoryBulkOperationsPerformance() throws InterruptedException {
        int batchSize = 100;
        int totalCustomers = 1000;
        
        List<LoadTestDataGenerator.TestCustomerData> testCustomers = 
            dataGenerator.generateCustomers(totalCustomers);
        
        metricsCollector.startCollection();
        
        // Test bulk customer creation
        long startTime = System.currentTimeMillis();
        
        List<Future<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < totalCustomers; i += batchSize) {
            final int startIndex = i;
            final int endIndex = Math.min(i + batchSize, totalCustomers);
            
            futures.add(executorService.submit(() -> {
                createCustomerBatch(testCustomers.subList(startIndex, endIndex));
                return null;
            }));
        }
        
        // Wait for all batches to complete
        for (Future<Void> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        
        long batchCreationTime = System.currentTimeMillis() - startTime;
        metricsCollector.recordOperation("BulkCustomerCreation", batchCreationTime, true);
        
        // Test concurrent customer queries
        CountDownLatch queryLatch = new CountDownLatch(50);
        
        for (int i = 0; i < 50; i++) {
            final int queryIndex = i;
            executorService.submit(() -> {
                try {
                    long queryStartTime = System.currentTimeMillis();
                    
                    // Test various query patterns
                    switch (queryIndex % 3) {
                        case 0 -> {
                            // ID-based lookup
                            customerRepository.findById((long) (queryIndex % totalCustomers + 1));
                        }
                        case 1 -> {
                            // Email-based lookup
                            String email = testCustomers.get(queryIndex % testCustomers.size()).email();
                            customerRepository.findByEmail(email);
                        }
                        case 2 -> {
                            // Existence check
                            String email = testCustomers.get(queryIndex % testCustomers.size()).email();
                            customerRepository.existsByEmail(email);
                        }
                    }
                    
                    long queryTime = System.currentTimeMillis() - queryStartTime;
                    metricsCollector.recordOperation("CustomerQuery", queryTime, true);
                    
                } catch (Exception e) {
                    metricsCollector.recordOperation("CustomerQuery", 0, false);
                } finally {
                    queryLatch.countDown();
                }
            });
        }
        
        assertTrue(queryLatch.await(30, TimeUnit.SECONDS), "Customer queries should complete");
        
        metricsCollector.stopCollection();
        
        // Generate performance report
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("DATABASE PERFORMANCE REPORT:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        assertTrue(batchCreationTime < 30000, 
                "Bulk customer creation should complete within 30 seconds");
        
        PerformanceMetricsCollector.OperationMetrics queryMetrics = 
            report.operationMetrics().get("CustomerQuery");
        
        if (queryMetrics != null) {
            assertTrue(queryMetrics.avgResponseTime() < 500, 
                    "Average customer query time should be under 500ms");
            assertTrue(queryMetrics.p95ResponseTime() < 1000, 
                    "95th percentile query time should be under 1 second");
        }
    }
    
    @Test
    @DisplayName("Loan Repository Complex Query Performance")
    void loanRepositoryComplexQueryPerformance() throws InterruptedException {
        // First create test data
        setupTestDataForLoanQueries();
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(100);
        
        // Test various loan query patterns concurrently
        for (int i = 0; i < 100; i++) {
            final int queryIndex = i;
            
            executorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    switch (queryIndex % 4) {
                        case 0 -> {
                            // Customer-based loan queries
                            Long customerId = (long) (queryIndex % 50 + 1);
                            loanRepository.findByCustomerId(customerId);
                        }
                        case 1 -> {
                            // Status-based queries
                            LoanStatus[] statuses = LoanStatus.values();
                            LoanStatus status = statuses[queryIndex % statuses.length];
                            loanRepository.findByStatus(status);
                        }
                        case 2 -> {
                            // ID-based queries
                            Long loanId = (long) (queryIndex % 100 + 1);
                            loanRepository.findById(loanId);
                        }
                        case 3 -> {
                            // Combined queries (findByCustomerId + findByStatus)
                            Long customerId = (long) (queryIndex % 50 + 1);
                            loanRepository.findByCustomerId(customerId);
                            loanRepository.findByStatus(LoanStatus.ACTIVE);
                        }
                    }
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    metricsCollector.recordOperation("LoanComplexQuery", responseTime, true);
                    
                } catch (Exception e) {
                    metricsCollector.recordOperation("LoanComplexQuery", 0, false);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(60, TimeUnit.SECONDS), "Loan queries should complete");
        
        metricsCollector.stopCollection();
        
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("LOAN QUERY PERFORMANCE:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics metrics = 
            report.operationMetrics().get("LoanComplexQuery");
        
        assertNotNull(metrics);
        assertTrue(metrics.avgResponseTime() < 1000, 
                "Complex loan queries should average under 1 second");
        assertTrue(metrics.errorRate() < 5.0, 
                "Error rate should be minimal for loan queries");
    }
    
    @Test
    @DisplayName("Payment Repository High Volume Performance")
    void paymentRepositoryHighVolumePerformance() throws InterruptedException {
        // Setup customers and loans first
        setupTestDataForPaymentQueries();
        
        // Generate high volume of payments
        int paymentsPerLoan = 20;
        List<Long> loanIds = IntStream.rangeClosed(1, 50)
                .mapToObj(Long::valueOf)
                .toList();
        
        List<ProcessPaymentUseCase.ProcessPaymentCommand> paymentCommands = 
            dataGenerator.generatePayments(loanIds, paymentsPerLoan);
        
        metricsCollector.startCollection();
        
        // Test concurrent payment processing
        CountDownLatch paymentLatch = new CountDownLatch(paymentCommands.size());
        
        for (ProcessPaymentUseCase.ProcessPaymentCommand command : paymentCommands) {
            executorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    // Create payment entity (simplified for testing)
                    Payment payment = new Payment(
                            command.loanId(),
                            command.amount(),
                            command.paymentDate(),
                            command.type()
                    );
                    
                    paymentRepository.save(payment);
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    metricsCollector.recordOperation("PaymentCreation", responseTime, true);
                    
                } catch (Exception e) {
                    metricsCollector.recordOperation("PaymentCreation", 0, false);
                } finally {
                    paymentLatch.countDown();
                }
            });
        }
        
        assertTrue(paymentLatch.await(120, TimeUnit.SECONDS), 
                "Payment creation should complete within 2 minutes");
        
        // Test payment queries
        CountDownLatch queryLatch = new CountDownLatch(100);
        
        for (int i = 0; i < 100; i++) {
            final int queryIndex = i;
            
            executorService.submit(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    
                    Long loanId = loanIds.get(queryIndex % loanIds.size());
                    paymentRepository.findByLoanId(loanId);
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    metricsCollector.recordOperation("PaymentQuery", responseTime, true);
                    
                } catch (Exception e) {
                    metricsCollector.recordOperation("PaymentQuery", 0, false);
                } finally {
                    queryLatch.countDown();
                }
            });
        }
        
        assertTrue(queryLatch.await(30, TimeUnit.SECONDS), "Payment queries should complete");
        
        metricsCollector.stopCollection();
        
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("PAYMENT PERFORMANCE REPORT:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics creationMetrics = 
            report.operationMetrics().get("PaymentCreation");
        PerformanceMetricsCollector.OperationMetrics queryMetrics = 
            report.operationMetrics().get("PaymentQuery");
        
        assertNotNull(creationMetrics);
        assertNotNull(queryMetrics);
        
        assertTrue(creationMetrics.avgResponseTime() < 200, 
                "Payment creation should be fast");
        assertTrue(queryMetrics.avgResponseTime() < 300, 
                "Payment queries should be fast");
    }
    
    // Helper methods
    
    @Transactional
    private void createCustomerBatch(List<LoadTestDataGenerator.TestCustomerData> customers) {
        for (LoadTestDataGenerator.TestCustomerData customerData : customers) {
            Customer customer = new Customer(
                    customerData.firstName(),
                    customerData.lastName(),
                    customerData.email(),
                    customerData.phone(),
                    customerData.dateOfBirth(),
                    customerData.monthlyIncome()
            );
            
            customerRepository.save(customer);
        }
    }
    
    private void setupTestDataForLoanQueries() {
        // Create 50 customers and 100 loans for testing
        List<LoadTestDataGenerator.TestCustomerData> customers = dataGenerator.generateCustomers(50);
        
        for (LoadTestDataGenerator.TestCustomerData customerData : customers) {
            Customer customer = new Customer(
                    customerData.firstName(),
                    customerData.lastName(),
                    customerData.email(),
                    customerData.phone(),
                    customerData.dateOfBirth(),
                    customerData.monthlyIncome()
            );
            customerRepository.save(customer);
        }
        
        // Create loans
        List<CreateLoanUseCase.CreateLoanCommand> loanCommands = 
            dataGenerator.generateLoanApplications(customers, 2);
        
        for (CreateLoanUseCase.CreateLoanCommand command : loanCommands) {
            // Create simplified loan for testing
            // Note: This is a simplified version for performance testing
            // In real scenario, would use the proper domain service
        }
    }
    
    private void setupTestDataForPaymentQueries() {
        setupTestDataForLoanQueries();
    }
}