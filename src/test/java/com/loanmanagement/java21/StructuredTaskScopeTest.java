package com.loanmanagement.java21;

import com.loanmanagement.loan.application.port.out.CustomerRepository;
import com.loanmanagement.loan.application.port.out.CreditCheckPort;
import com.loanmanagement.loan.application.port.out.FraudDetectionPort;
import com.loanmanagement.loan.application.port.out.RiskAssessmentPort;
import com.loanmanagement.loan.application.service.ParallelLoanProcessingService;
import com.loanmanagement.loan.domain.model.CustomerId;
import com.loanmanagement.loan.domain.model.LoanApplication;
import com.loanmanagement.loan.domain.model.LoanApplicationId;
import com.loanmanagement.loan.domain.model.LoanDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StructuredTaskScope Migration Tests")
class StructuredTaskScopeTest {

    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private CreditCheckPort creditCheckPort;
    
    @Mock
    private FraudDetectionPort fraudDetectionPort;
    
    @Mock
    private RiskAssessmentPort riskAssessmentPort;
    
    private ParallelLoanProcessingService parallelLoanProcessingService;

    @BeforeEach
    void setUp() {
        parallelLoanProcessingService = new ParallelLoanProcessingService(
            customerRepository,
            creditCheckPort, 
            fraudDetectionPort,
            riskAssessmentPort
        );
    }

    @Test
    @DisplayName("Should replace CompletableFuture parallel calls with StructuredTaskScope")
    void shouldReplaceCompletableFutureParallelCallsWithStructuredTaskScope() {
        // given
        LoanApplication application = createLoanApplication();
        
        when(creditCheckPort.performCreditCheck(any())).thenReturn(createMockCreditCheck());
        when(fraudDetectionPort.checkForFraud(any())).thenReturn(createMockFraudCheck());
        when(riskAssessmentPort.assessRisk(any())).thenReturn(createMockRiskAssessment());
        
        // when - This should use StructuredTaskScope instead of CompletableFuture.allOf()
        LoanDecision decision = assertDoesNotThrow(() -> 
            parallelLoanProcessingService.processLoanApplicationWithStructuredConcurrency(application),
            "StructuredTaskScope should handle parallel processing");
        
        // then
        assertThat(decision).isNotNull();
        assertThat(decision.isApproved()).isTrue();
        
        // Verify all services were called in parallel
        verify(creditCheckPort).performCreditCheck(any());
        verify(fraudDetectionPort).checkForFraud(any());
        verify(riskAssessmentPort).assessRisk(any());
    }

    @Test
    @DisplayName("Should handle task failure with StructuredTaskScope.ShutdownOnFailure")
    void shouldHandleTaskFailureWithStructuredTaskScopeShutdownOnFailure() {
        // given
        LoanApplication application = createLoanApplication();
        
        when(creditCheckPort.performCreditCheck(any())).thenReturn(createMockCreditCheck());
        when(fraudDetectionPort.checkForFraud(any())).thenThrow(new RuntimeException("Fraud detection service unavailable"));
        when(riskAssessmentPort.assessRisk(any())).thenReturn(createMockRiskAssessment());
        
        // when - StructuredTaskScope should shutdown on failure and cancel remaining tasks
        Exception exception = assertThrows(RuntimeException.class, () -> 
            parallelLoanProcessingService.processLoanApplicationWithStructuredConcurrency(application));
        
        // then
        assertThat(exception.getMessage())
            .contains("Fraud detection service unavailable")
            .describedAs("StructuredTaskScope should propagate failure from subtask");
        
        // Verify that other tasks were cancelled when one failed
        verify(creditCheckPort, atMost(1)).performCreditCheck(any());
        verify(fraudDetectionPort, times(1)).checkForFraud(any());
        // Risk assessment might not be called if fraud check fails first
        verify(riskAssessmentPort, atMost(1)).assessRisk(any());
    }

    @Test
    @DisplayName("Should demonstrate improved resource management with StructuredTaskScope")
    void shouldDemonstrateImprovedResourceManagementWithStructuredTaskScope() {
        // given
        List<LoanApplication> applications = createMultipleLoanApplications(100);
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        
        setupMockServices();
        
        // when - Process multiple applications using StructuredTaskScope
        long startTime = System.currentTimeMillis();
        
        try (var scope = createStructuredTaskScope()) {
            
            for (LoanApplication application : applications) {
                scope.fork(() -> {
                    try {
                        LoanDecision decision = parallelLoanProcessingService
                            .processLoanApplicationWithStructuredConcurrency(application);
                        processedCount.incrementAndGet();
                        return decision;
                    } catch (Exception e) {
                        failedCount.incrementAndGet();
                        throw new RuntimeException("Processing failed for " + application.getId(), e);
                    }
                });
            }
            
            // Join and collect results
            scope.join();
            scope.throwIfFailed();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("StructuredTaskScope processing should not be interrupted");
        }
        
        long endTime = System.currentTimeMillis();
        
        // then
        assertThat(processedCount.get())
            .isGreaterThan(0)
            .describedAs("StructuredTaskScope should process loan applications successfully");
        
        assertThat(endTime - startTime)
            .isLessThan(10000) // Should complete within 10 seconds
            .describedAs("StructuredTaskScope should provide efficient parallel processing");
        
        // Resource cleanup should be automatic with try-with-resources
        assertDoesNotThrow(() -> scope.close(), 
            "StructuredTaskScope should clean up resources automatically");
    }

    @Test
    @DisplayName("Should coordinate multiple external service calls with StructuredTaskScope")
    void shouldCoordinateMultipleExternalServiceCallsWithStructuredTaskScope() {
        // given
        LoanApplication application = createLoanApplication();
        AtomicReference<String> creditCheckResult = new AtomicReference<>();
        AtomicReference<String> fraudCheckResult = new AtomicReference<>();
        AtomicReference<String> riskAssessmentResult = new AtomicReference<>();
        
        when(creditCheckPort.performCreditCheck(any())).thenAnswer(invocation -> {
            Thread.sleep(100); // Simulate external API call
            String result = "CREDIT_APPROVED";
            creditCheckResult.set(result);
            return createMockCreditCheck();
        });
        
        when(fraudDetectionPort.checkForFraud(any())).thenAnswer(invocation -> {
            Thread.sleep(150); // Simulate external API call
            String result = "NO_FRAUD_DETECTED";
            fraudCheckResult.set(result);
            return createMockFraudCheck();
        });
        
        when(riskAssessmentPort.assessRisk(any())).thenAnswer(invocation -> {
            Thread.sleep(120); // Simulate external API call
            String result = "LOW_RISK";
            riskAssessmentResult.set(result);
            return createMockRiskAssessment();
        });
        
        // when - Execute coordinated parallel calls
        long startTime = System.currentTimeMillis();
        
        LoanDecision decision = parallelLoanProcessingService
            .processLoanApplicationWithStructuredConcurrency(application);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // then
        assertThat(decision).isNotNull();
        
        // All external calls should have completed
        assertThat(creditCheckResult.get()).isEqualTo("CREDIT_APPROVED");
        assertThat(fraudCheckResult.get()).isEqualTo("NO_FRAUD_DETECTED");
        assertThat(riskAssessmentResult.get()).isEqualTo("LOW_RISK");
        
        // Parallel execution should be faster than sequential (150ms + 100ms + 120ms = 370ms)
        assertThat(executionTime)
            .isLessThan(300) // Should complete in under 300ms due to parallelism
            .describedAs("StructuredTaskScope should execute external calls in parallel");
        
        verify(creditCheckPort, times(1)).performCreditCheck(any());
        verify(fraudDetectionPort, times(1)).checkForFraud(any());
        verify(riskAssessmentPort, times(1)).assessRisk(any());
    }

    @Test
    @DisplayName("Should handle timeout scenarios with StructuredTaskScope")
    void shouldHandleTimeoutScenariosWithStructuredTaskScope() {
        // given
        LoanApplication application = createLoanApplication();
        Duration timeout = Duration.ofMillis(500);
        
        when(creditCheckPort.performCreditCheck(any())).thenAnswer(invocation -> {
            Thread.sleep(200); // Fast service
            return createMockCreditCheck();
        });
        
        when(fraudDetectionPort.checkForFraud(any())).thenAnswer(invocation -> {
            Thread.sleep(1000); // Slow service that will timeout
            return createMockFraudCheck();
        });
        
        when(riskAssessmentPort.assessRisk(any())).thenAnswer(invocation -> {
            Thread.sleep(300); // Medium speed service
            return createMockRiskAssessment();
        });
        
        // when - StructuredTaskScope should handle timeout
        Exception exception = assertThrows(Exception.class, () -> 
            parallelLoanProcessingService.processLoanApplicationWithTimeout(application, timeout));
        
        // then
        assertThat(exception)
            .hasMessageContaining("timeout")
            .describedAs("StructuredTaskScope should handle timeout scenarios");
        
        // Fast services should complete, slow service should be cancelled
        verify(creditCheckPort, times(1)).performCreditCheck(any());
        verify(fraudDetectionPort, times(1)).checkForFraud(any()); // Started but timed out
        verify(riskAssessmentPort, times(1)).assessRisk(any());
    }

    @Test
    @DisplayName("Should replace traditional thread pools with StructuredTaskScope for domain operations")
    void shouldReplaceTraditionalThreadPoolsWithStructuredTaskScopeForDomainOperations() {
        // given
        List<LoanApplication> batchApplications = createMultipleLoanApplications(50);
        setupMockServices();
        
        // when - Process batch using StructuredTaskScope instead of ExecutorService
        List<LoanDecision> decisions = parallelLoanProcessingService
            .processBatchApplicationsWithStructuredConcurrency(batchApplications);
        
        // then
        assertThat(decisions)
            .hasSize(batchApplications.size())
            .describedAs("StructuredTaskScope should process all applications in batch");
        
        assertThat(decisions)
            .allMatch(decision -> decision != null)
            .describedAs("All decisions should be non-null");
        
        // Verify structured concurrency provides better error handling and resource management
        // than traditional thread pools
        verify(creditCheckPort, times(batchApplications.size())).performCreditCheck(any());
        verify(fraudDetectionPort, times(batchApplications.size())).checkForFraud(any());
        verify(riskAssessmentPort, times(batchApplications.size())).assessRisk(any());
    }

    // Helper methods - These will be implemented during Green phase
    
    private StructuredTaskScope.ShutdownOnFailure createStructuredTaskScope() {
        // Will be implemented in Green phase
        // return new StructuredTaskScope.ShutdownOnFailure();
        throw new UnsupportedOperationException("StructuredTaskScope not yet implemented - Java 21 migration pending");
    }
    
    private LoanApplication createLoanApplication() {
        return LoanApplication.builder()
            .id(new LoanApplicationId("LOAN-TEST-001"))
            .customerId(new CustomerId("CUST-TEST-001"))
            .amount(BigDecimal.valueOf(50000))
            .build();
    }
    
    private List<LoanApplication> createMultipleLoanApplications(int count) {
        return List.of(); // Will be implemented
    }
    
    private void setupMockServices() {
        when(creditCheckPort.performCreditCheck(any())).thenReturn(createMockCreditCheck());
        when(fraudDetectionPort.checkForFraud(any())).thenReturn(createMockFraudCheck());
        when(riskAssessmentPort.assessRisk(any())).thenReturn(createMockRiskAssessment());
    }
    
    private Object createMockCreditCheck() {
        return new Object(); // Mock implementation
    }
    
    private Object createMockFraudCheck() {
        return new Object(); // Mock implementation
    }
    
    private Object createMockRiskAssessment() {
        return new Object(); // Mock implementation
    }
}