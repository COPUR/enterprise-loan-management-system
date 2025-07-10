package com.loanmanagement.performance;

import com.loanmanagement.customer.application.port.in.CreateCustomerUseCase;
import com.loanmanagement.customer.application.port.in.GetCustomerUseCase;
import com.loanmanagement.loan.application.port.in.CreateLoanUseCase;
import com.loanmanagement.loan.application.port.in.GetLoanQuery;
import com.loanmanagement.loan.domain.model.Loan;
import com.loanmanagement.payment.application.port.in.ProcessPaymentUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Load Testing for Loan Management System
 * Tests system performance under various load conditions
 */
@SpringBootTest
@ActiveProfiles("performance")
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=50",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "logging.level.com.loanmanagement=INFO"
})
@DisplayName("Loan Management Load Tests")
class LoanManagementLoadTest {
    
    @Autowired
    private CreateCustomerUseCase createCustomerUseCase;
    
    @Autowired
    private GetCustomerUseCase getCustomerUseCase;
    
    @Autowired
    private CreateLoanUseCase createLoanUseCase;
    
    @Autowired
    private GetLoanQuery getLoanQuery;
    
    @Autowired
    private ProcessPaymentUseCase processPaymentUseCase;
    
    @Autowired
    private LoadTestDataGenerator dataGenerator;
    
    @Autowired
    private PerformanceMetricsCollector metricsCollector;
    
    @Autowired
    private ExecutorService loadTestExecutorService;
    
    private List<LoadTestDataGenerator.TestCustomerData> testCustomers;
    private List<CreateLoanUseCase.CreateLoanCommand> testLoanApplications;
    
    @BeforeEach
    void setUp() {
        metricsCollector.clearMetrics();
        
        // Generate test data
        testCustomers = dataGenerator.generateCustomers(100);
        testLoanApplications = dataGenerator.generateLoanApplications(testCustomers, 2);
    }
    
    @Test
    @DisplayName("Customer Creation Load Test")
    void customerCreationLoadTest() throws InterruptedException {
        // Test Parameters
        int concurrentUsers = 50;
        int operationsPerUser = 10;
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (int i = 0; i < concurrentUsers; i++) {
            final int userIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerUser; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        try {
                            LoadTestDataGenerator.TestCustomerData customerData = 
                                testCustomers.get((userIndex * operationsPerUser + j) % testCustomers.size());
                            
                            CreateCustomerUseCase.CreateCustomerCommand command = 
                                new CreateCustomerUseCase.CreateCustomerCommand(
                                    customerData.firstName(),
                                    customerData.lastName(),
                                    customerData.email() + "_load_" + userIndex + "_" + j, // Make unique
                                    customerData.phone(),
                                    customerData.dateOfBirth(),
                                    customerData.monthlyIncome()
                                );
                            
                            createCustomerUseCase.createCustomer(command);
                            
                            long responseTime = System.currentTimeMillis() - startTime;
                            metricsCollector.recordOperation("CreateCustomer", responseTime, true);
                            successCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            long responseTime = System.currentTimeMillis() - startTime;
                            metricsCollector.recordOperation("CreateCustomer", responseTime, false);
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all operations to complete (max 2 minutes)
        assertTrue(latch.await(120, TimeUnit.SECONDS), "Load test should complete within 2 minutes");
        
        metricsCollector.stopCollection();
        
        // Generate and log performance report
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println(report.generateSummary());
        
        // Assertions
        int totalOperations = concurrentUsers * operationsPerUser;
        assertEquals(totalOperations, successCount.get() + errorCount.get(), 
                "Total operations should match expected count");
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics metrics = 
            report.operationMetrics().get("CreateCustomer");
        
        assertNotNull(metrics, "CreateCustomer metrics should be available");
        assertTrue(metrics.avgResponseTime() < 1000, 
                "Average response time should be less than 1 second");
        assertTrue(metrics.p95ResponseTime() < 2000, 
                "95th percentile response time should be less than 2 seconds");
        assertTrue(metrics.errorRate() < 5.0, 
                "Error rate should be less than 5%");
        
        double throughput = metricsCollector.getCurrentThroughput("CreateCustomer");
        assertTrue(throughput > 10, "Throughput should be greater than 10 req/sec");
    }
    
    @Test
    @DisplayName("Loan Application Load Test")
    void loanApplicationLoadTest() throws InterruptedException {
        // First create some customers
        setupCustomersForLoanTest();
        
        // Test Parameters
        int concurrentUsers = 30;
        int operationsPerUser = 5;
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < concurrentUsers; i++) {
            final int userIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerUser; j++) {
                        long startTime = System.currentTimeMillis();
                        
                        try {
                            CreateLoanUseCase.CreateLoanCommand command = 
                                testLoanApplications.get((userIndex * operationsPerUser + j) % testLoanApplications.size());
                            
                            Loan loan = createLoanUseCase.createLoan(command);
                            assertNotNull(loan);
                            
                            long responseTime = System.currentTimeMillis() - startTime;
                            metricsCollector.recordOperation("CreateLoan", responseTime, true);
                            successCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            long responseTime = System.currentTimeMillis() - startTime;
                            metricsCollector.recordOperation("CreateLoan", responseTime, false);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(180, TimeUnit.SECONDS), "Loan creation load test should complete");
        
        metricsCollector.stopCollection();
        
        // Generate performance report
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println(report.generateSummary());
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics metrics = 
            report.operationMetrics().get("CreateLoan");
        
        assertNotNull(metrics);
        assertTrue(metrics.avgResponseTime() < 1500, 
                "Loan creation average response time should be reasonable");
        assertTrue(metrics.errorRate() < 10.0, 
                "Error rate should be acceptable");
    }
    
    @Test
    @DisplayName("Mixed Operations Load Test")
    void mixedOperationsLoadTest() throws InterruptedException {
        // Setup test data
        setupCustomersForLoanTest();
        
        int concurrentUsers = 40;
        int operationsPerUser = 8;
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        
        for (int i = 0; i < concurrentUsers; i++) {
            final int userIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerUser; j++) {
                        // Mix of operations: 40% reads, 30% loan creation, 30% customer queries
                        int operation = j % 10;
                        
                        if (operation < 4) {
                            // Customer queries (40%)
                            performCustomerQuery(userIndex, j);
                        } else if (operation < 7) {
                            // Loan creation (30%)
                            performLoanCreation(userIndex, j);
                        } else {
                            // Loan queries (30%)
                            performLoanQuery(userIndex, j);
                        }
                        
                        // Small delay to simulate realistic user behavior
                        Thread.sleep(10 + (int)(Math.random() * 50));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(300, TimeUnit.SECONDS), "Mixed operations test should complete");
        
        metricsCollector.stopCollection();
        
        // Generate comprehensive report
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println(report.generateSummary());
        
        // Verify overall system performance
        assertTrue(report.overallMetrics().overallSuccessRate() > 85.0, 
                "Overall success rate should be above 85%");
        assertTrue(report.overallMetrics().throughput() > 20, 
                "Overall throughput should be above 20 req/sec");
    }
    
    @Test
    @DisplayName("Stress Test - High Concurrency")
    @Disabled("Enable for stress testing - may impact system resources")
    void stressTestHighConcurrency() throws InterruptedException {
        int concurrentUsers = 200;
        int operationsPerUser = 3;
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(concurrentUsers);
        
        for (int i = 0; i < concurrentUsers; i++) {
            final int userIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerUser; j++) {
                        performCustomerQuery(userIndex, j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(600, TimeUnit.SECONDS), "Stress test should complete");
        
        metricsCollector.stopCollection();
        
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("STRESS TEST RESULTS:");
        System.out.println(report.generateSummary());
        
        // Stress test assertions - more lenient
        assertTrue(report.overallMetrics().overallSuccessRate() > 70.0, 
                "Under stress, success rate should still be above 70%");
    }
    
    // Helper methods
    
    private void setupCustomersForLoanTest() {
        // Create a subset of customers for loan testing
        for (int i = 0; i < Math.min(20, testCustomers.size()); i++) {
            LoadTestDataGenerator.TestCustomerData customerData = testCustomers.get(i);
            
            CreateCustomerUseCase.CreateCustomerCommand command = 
                new CreateCustomerUseCase.CreateCustomerCommand(
                    customerData.firstName(),
                    customerData.lastName(),
                    customerData.email(),
                    customerData.phone(),
                    customerData.dateOfBirth(),
                    customerData.monthlyIncome()
                );
            
            try {
                createCustomerUseCase.createCustomer(command);
            } catch (Exception e) {
                // Customer might already exist, ignore
            }
        }
    }
    
    private void performCustomerQuery(int userIndex, int operationIndex) {
        long startTime = System.currentTimeMillis();
        
        try {
            Long customerId = (long) ((userIndex + operationIndex) % 20 + 1);
            getCustomerUseCase.getCustomerById(customerId);
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("GetCustomer", responseTime, true);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("GetCustomer", responseTime, false);
        }
    }
    
    private void performLoanCreation(int userIndex, int operationIndex) {
        long startTime = System.currentTimeMillis();
        
        try {
            CreateLoanUseCase.CreateLoanCommand command = 
                testLoanApplications.get((userIndex * 8 + operationIndex) % testLoanApplications.size());
            
            createLoanUseCase.createLoan(command);
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("CreateLoan", responseTime, true);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("CreateLoan", responseTime, false);
        }
    }
    
    private void performLoanQuery(int userIndex, int operationIndex) {
        long startTime = System.currentTimeMillis();
        
        try {
            Long customerId = (long) ((userIndex + operationIndex) % 20 + 1);
            getLoanQuery.getLoansByCustomerId(customerId);
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("GetLoansByCustomer", responseTime, true);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("GetLoansByCustomer", responseTime, false);
        }
    }
}