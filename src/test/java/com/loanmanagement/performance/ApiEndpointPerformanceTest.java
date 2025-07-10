package com.loanmanagement.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loanmanagement.customer.infrastructure.adapter.in.web.dto.CreateCustomerRequest;
import com.loanmanagement.loan.infrastructure.adapter.in.web.dto.CreateLoanRequest;
import com.loanmanagement.payment.infrastructure.adapter.in.web.dto.ProcessPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Endpoint Performance Testing
 * Tests REST API endpoints under load to measure response times and throughput
 */
@SpringBootTest
@AutoConfigureTestMvc
@ActiveProfiles("performance")
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=50",
        "server.tomcat.threads.max=200",
        "server.tomcat.accept-count=100",
        "logging.level.org.springframework.web=INFO"
})
@DisplayName("API Endpoint Performance Tests")
class ApiEndpointPerformanceTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ExecutorService loadTestExecutorService;
    
    @Autowired
    private PerformanceMetricsCollector metricsCollector;
    
    @Autowired
    private LoadTestDataGenerator dataGenerator;
    
    @BeforeEach
    void setUp() {
        metricsCollector.clearMetrics();
    }
    
    @Test
    @DisplayName("Customer API Endpoint Performance")
    void customerApiEndpointPerformance() throws InterruptedException {
        int concurrentRequests = 100;
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    // Test customer creation endpoint
                    testCreateCustomerEndpoint(requestIndex);
                    
                    // Test customer retrieval endpoint
                    testGetCustomerEndpoint(requestIndex);
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(60, TimeUnit.SECONDS), "Customer API tests should complete");
        
        metricsCollector.stopCollection();
        
        // Generate performance report
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("CUSTOMER API PERFORMANCE REPORT:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics createMetrics = 
            report.operationMetrics().get("CreateCustomerAPI");
        PerformanceMetricsCollector.OperationMetrics getMetrics = 
            report.operationMetrics().get("GetCustomerAPI");
        
        if (createMetrics != null) {
            assertTrue(createMetrics.avgResponseTime() < 1000, 
                    "Customer creation API should respond within 1 second");
            assertTrue(createMetrics.p95ResponseTime() < 2000, 
                    "95th percentile should be under 2 seconds");
        }
        
        if (getMetrics != null) {
            assertTrue(getMetrics.avgResponseTime() < 500, 
                    "Customer retrieval API should respond within 500ms");
        }
    }
    
    @Test
    @DisplayName("Loan API Endpoint Performance")
    void loanApiEndpointPerformance() throws InterruptedException {
        // First create some customers for loan testing
        setupCustomersForApiTest();
        
        int concurrentRequests = 50;
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    // Test loan creation endpoint
                    testCreateLoanEndpoint(requestIndex);
                    
                    // Test loan retrieval endpoint
                    testGetLoanEndpoint(requestIndex);
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(120, TimeUnit.SECONDS), "Loan API tests should complete");
        
        metricsCollector.stopCollection();
        
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("LOAN API PERFORMANCE REPORT:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics createMetrics = 
            report.operationMetrics().get("CreateLoanAPI");
        
        if (createMetrics != null) {
            assertTrue(createMetrics.avgResponseTime() < 2000, 
                    "Loan creation API should respond within 2 seconds");
            assertTrue(createMetrics.errorRate() < 10.0, 
                    "Error rate should be acceptable");
        }
    }
    
    @Test
    @DisplayName("Payment API Endpoint Performance")
    void paymentApiEndpointPerformance() throws InterruptedException {
        // Setup test data
        setupCustomersForApiTest();
        setupLoansForApiTest();
        
        int concurrentRequests = 75;
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    // Test payment processing endpoint
                    testProcessPaymentEndpoint(requestIndex);
                    
                    // Test payment history endpoint
                    testGetPaymentHistoryEndpoint(requestIndex);
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(150, TimeUnit.SECONDS), "Payment API tests should complete");
        
        metricsCollector.stopCollection();
        
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("PAYMENT API PERFORMANCE REPORT:");
        System.out.println(report.generateSummary());
        
        // Performance assertions
        PerformanceMetricsCollector.OperationMetrics processMetrics = 
            report.operationMetrics().get("ProcessPaymentAPI");
        
        if (processMetrics != null) {
            assertTrue(processMetrics.avgResponseTime() < 1500, 
                    "Payment processing API should respond within 1.5 seconds");
        }
    }
    
    @Test
    @DisplayName("Mixed API Endpoint Load Test")
    void mixedApiEndpointLoadTest() throws InterruptedException {
        setupCustomersForApiTest();
        setupLoansForApiTest();
        
        int concurrentRequests = 200;
        
        metricsCollector.startCollection();
        
        CountDownLatch latch = new CountDownLatch(concurrentRequests);
        
        for (int i = 0; i < concurrentRequests; i++) {
            final int requestIndex = i;
            
            loadTestExecutorService.submit(() -> {
                try {
                    // Mix of different API calls
                    switch (requestIndex % 5) {
                        case 0 -> testCreateCustomerEndpoint(requestIndex);
                        case 1 -> testGetCustomerEndpoint(requestIndex);
                        case 2 -> testCreateLoanEndpoint(requestIndex);
                        case 3 -> testGetLoanEndpoint(requestIndex);
                        case 4 -> testProcessPaymentEndpoint(requestIndex);
                    }
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(300, TimeUnit.SECONDS), "Mixed API load test should complete");
        
        metricsCollector.stopCollection();
        
        PerformanceMetricsCollector.PerformanceReport report = metricsCollector.generateReport();
        System.out.println("MIXED API LOAD TEST REPORT:");
        System.out.println(report.generateSummary());
        
        // Overall performance assertions
        assertTrue(report.overallMetrics().overallSuccessRate() > 85.0, 
                "Overall success rate should be above 85%");
        assertTrue(report.overallMetrics().throughput() > 30, 
                "Overall throughput should be above 30 req/sec");
    }
    
    // Helper methods for individual endpoint testing
    
    private void testCreateCustomerEndpoint(int requestIndex) {
        try {
            long startTime = System.currentTimeMillis();
            
            CreateCustomerRequest request = new CreateCustomerRequest(
                    "John" + requestIndex,
                    "Doe" + requestIndex,
                    "john.doe" + requestIndex + "@example.com",
                    "+1-555-" + String.format("%04d", requestIndex),
                    LocalDate.now().minusYears(25),
                    new BigDecimal("5000.00")
            );
            
            MvcResult result = mockMvc.perform(post("/api/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("CreateCustomerAPI", responseTime, true);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
            metricsCollector.recordOperation("CreateCustomerAPI", responseTime, false);
        }
    }
    
    private void testGetCustomerEndpoint(int requestIndex) {
        try {
            long startTime = System.currentTimeMillis();
            
            Long customerId = (long) (requestIndex % 10 + 1);
            
            mockMvc.perform(get("/api/customers/{id}", customerId))
                    .andExpect(status().isOk());
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("GetCustomerAPI", responseTime, true);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
            metricsCollector.recordOperation("GetCustomerAPI", responseTime, false);
        }
    }
    
    private void testCreateLoanEndpoint(int requestIndex) {
        try {
            long startTime = System.currentTimeMillis();
            
            CreateLoanRequest request = new CreateLoanRequest(
                    (long) (requestIndex % 10 + 1), // customerId
                    new BigDecimal("10000.00"),
                    new BigDecimal("5.5"),
                    36
            );
            
            mockMvc.perform(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("CreateLoanAPI", responseTime, true);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
            metricsCollector.recordOperation("CreateLoanAPI", responseTime, false);
        }
    }
    
    private void testGetLoanEndpoint(int requestIndex) {
        try {
            long startTime = System.currentTimeMillis();
            
            Long loanId = (long) (requestIndex % 5 + 1);
            
            mockMvc.perform(get("/api/loans/{id}", loanId))
                    .andExpect(status().isOk());
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("GetLoanAPI", responseTime, true);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
            metricsCollector.recordOperation("GetLoanAPI", responseTime, false);
        }
    }
    
    private void testProcessPaymentEndpoint(int requestIndex) {
        try {
            long startTime = System.currentTimeMillis();
            
            ProcessPaymentRequest request = new ProcessPaymentRequest(
                    (long) (requestIndex % 5 + 1), // loanId
                    new BigDecimal("500.00"),
                    LocalDate.now(),
                    "REGULAR",
                    "BANK_TRANSFER",
                    "REF" + requestIndex,
                    "LoadTest"
            );
            
            mockMvc.perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("ProcessPaymentAPI", responseTime, true);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
            metricsCollector.recordOperation("ProcessPaymentAPI", responseTime, false);
        }
    }
    
    private void testGetPaymentHistoryEndpoint(int requestIndex) {
        try {
            long startTime = System.currentTimeMillis();
            
            Long loanId = (long) (requestIndex % 5 + 1);
            
            mockMvc.perform(get("/api/loans/{id}/payments", loanId))
                    .andExpect(status().isOk());
            
            long responseTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordOperation("GetPaymentHistoryAPI", responseTime, true);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - System.currentTimeMillis();
            metricsCollector.recordOperation("GetPaymentHistoryAPI", responseTime, false);
        }
    }
    
    // Setup methods
    
    private void setupCustomersForApiTest() {
        for (int i = 1; i <= 10; i++) {
            try {
                CreateCustomerRequest request = new CreateCustomerRequest(
                        "TestCustomer" + i,
                        "LastName" + i,
                        "test" + i + "@example.com",
                        "+1-555-000" + i,
                        LocalDate.now().minusYears(25),
                        new BigDecimal("5000.00")
                );
                
                mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated());
                
            } catch (Exception e) {
                // Ignore setup errors
            }
        }
    }
    
    private void setupLoansForApiTest() {
        for (int i = 1; i <= 5; i++) {
            try {
                CreateLoanRequest request = new CreateLoanRequest(
                        (long) i, // customerId
                        new BigDecimal("15000.00"),
                        new BigDecimal("4.5"),
                        48
                );
                
                mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpected(status().isCreated());
                
            } catch (Exception e) {
                // Ignore setup errors
            }
        }
    }
}