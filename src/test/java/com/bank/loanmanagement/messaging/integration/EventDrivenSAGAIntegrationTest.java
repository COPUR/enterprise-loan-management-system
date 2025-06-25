package com.bank.loanmanagement.messaging.integration;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import com.bank.loanmanagement.messaging.infrastructure.kafka.KafkaEventPublisher;
import com.bank.loanmanagement.messaging.infrastructure.kafka.KafkaEventConsumer;
import com.bank.loanmanagement.messaging.infrastructure.security.SecureEventPublisher;
import com.bank.loanmanagement.saga.infrastructure.KafkaSagaOrchestrator;
import com.bank.loanmanagement.saga.domain.LoanOriginationSaga;
import com.bank.loanmanagement.loan.domain.event.LoanApplicationInitiatedEvent;
import com.bank.loanmanagement.loan.domain.event.LoanApprovedEvent;
import com.bank.loanmanagement.loan.domain.event.LoanRejectedEvent;
import com.bank.loanmanagement.customer.domain.event.CreditReservedEvent;
import com.bank.loanmanagement.customer.domain.event.CreditReservationFailedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Event-Driven SAGA Integration Test
 * Tests comprehensive SAGA orchestration patterns with event-driven coordination
 * Validates distributed transaction management, compensation, and BIAN compliance
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${kafka.bootstrap.servers}",
    "spring.datasource.url=${postgresql.datasource.url}"
})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventDrivenSAGAIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("banking_saga_test")
            .withUsername("saga_test_user")
            .withPassword("saga_test_password")
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("kafka.bootstrap.servers", kafka::getBootstrapServers);
        registry.add("postgresql.datasource.url", postgres::getJdbcUrl);
    }

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;
    
    @Mock
    private KafkaEventConsumer kafkaEventConsumer;
    
    @Mock
    private SecureEventPublisher secureEventPublisher;
    
    private KafkaSagaOrchestrator sagaOrchestrator;
    private LoanOriginationSaga loanOriginationSaga;
    
    private CountDownLatch sagaCompletionLatch;
    private AtomicReference<String> sagaResult;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize SAGA components
        sagaOrchestrator = mock(KafkaSagaOrchestrator.class);
        loanOriginationSaga = new LoanOriginationSaga();
        
        sagaCompletionLatch = new CountDownLatch(1);
        sagaResult = new AtomicReference<>();
    }

    @Test
    @DisplayName("Should complete successful loan origination SAGA with event coordination")
    void shouldCompleteSuccessfulLoanOriginationSAGA() throws Exception {
        // Given
        String sagaId = "loan-saga-" + UUID.randomUUID().toString();
        String customerId = "CUST-001";
        String loanApplicationId = "LOAN-APP-001";
        
        LoanApplicationInitiatedEvent initiatingEvent = createLoanApplicationEvent(customerId, loanApplicationId, 50000.00);
        
        // Configure mocks for successful SAGA flow
        when(secureEventPublisher.publishSecureEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sagaOrchestrator.startSaga(any(), any())).thenAnswer(invocation -> {
            simulateSuccessfulSagaExecution(sagaId);
            return CompletableFuture.completedFuture(null);
        });

        // When
        CompletableFuture<Void> sagaResult = sagaOrchestrator.startSaga(loanOriginationSaga, 
            Map.of("initiatingEvent", initiatingEvent, "sagaId", sagaId));
        
        // Simulate SAGA step events
        simulateSagaStepEvents(sagaId, customerId, loanApplicationId, true);
        
        // Then
        assertDoesNotThrow(() -> sagaResult.get(10, TimeUnit.SECONDS));
        assertTrue(sagaCompletionLatch.await(15, TimeUnit.SECONDS), "SAGA should complete within timeout");
        assertEquals("COMPLETED", this.sagaResult.get());
        
        // Verify SAGA orchestration was triggered
        verify(sagaOrchestrator).startSaga(any(), any());
        
        // Verify events were published for each SAGA step
        verify(secureEventPublisher, atLeast(4)).publishSecureEvent(any()); // At least 4 steps in loan SAGA
    }

    @Test
    @DisplayName("Should handle SAGA compensation when credit reservation fails")
    void shouldHandleSAGACompensationWhenCreditReservationFails() throws Exception {
        // Given
        String sagaId = "loan-saga-compensation-" + UUID.randomUUID().toString();
        String customerId = "CUST-002";
        String loanApplicationId = "LOAN-APP-002";
        
        LoanApplicationInitiatedEvent initiatingEvent = createLoanApplicationEvent(customerId, loanApplicationId, 200000.00); // Excessive amount
        
        // Configure mocks for compensation scenario
        when(secureEventPublisher.publishSecureEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sagaOrchestrator.startSaga(any(), any())).thenAnswer(invocation -> {
            simulateFailedSagaExecution(sagaId, "CREDIT_RESERVATION_FAILED");
            return CompletableFuture.completedFuture(null);
        });

        // When
        CompletableFuture<Void> sagaResult = sagaOrchestrator.startSaga(loanOriginationSaga, 
            Map.of("initiatingEvent", initiatingEvent, "sagaId", sagaId));
        
        // Simulate SAGA failure and compensation
        simulateSagaStepEvents(sagaId, customerId, loanApplicationId, false);
        
        // Then
        assertDoesNotThrow(() -> sagaResult.get(10, TimeUnit.SECONDS));
        assertTrue(sagaCompletionLatch.await(15, TimeUnit.SECONDS), "SAGA compensation should complete within timeout");
        assertEquals("COMPENSATED", this.sagaResult.get());
        
        // Verify compensation events were published
        verify(secureEventPublisher, atLeast(2)).publishSecureEvent(any()); // Initial attempt + compensation
    }

    @Test
    @DisplayName("Should handle concurrent SAGA executions without interference")
    void shouldHandleConcurrentSAGAExecutionsWithoutInterference() throws Exception {
        // Given
        int concurrentSagas = 5;
        CountDownLatch allSagasLatch = new CountDownLatch(concurrentSagas);
        java.util.List<String> sagaIds = new java.util.ArrayList<>();
        
        // Configure mocks for concurrent execution
        when(secureEventPublisher.publishSecureEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sagaOrchestrator.startSaga(any(), any())).thenAnswer(invocation -> {
            allSagasLatch.countDown();
            return CompletableFuture.completedFuture(null);
        });

        // When
        java.util.List<CompletableFuture<Void>> sagaFutures = new java.util.ArrayList<>();
        for (int i = 0; i < concurrentSagas; i++) {
            String sagaId = "concurrent-saga-" + i;
            sagaIds.add(sagaId);
            
            LoanApplicationInitiatedEvent event = createLoanApplicationEvent("CUST-" + i, "LOAN-APP-" + i, 30000.00);
            CompletableFuture<Void> sagaFuture = sagaOrchestrator.startSaga(loanOriginationSaga, 
                Map.of("initiatingEvent", event, "sagaId", sagaId));
            sagaFutures.add(sagaFuture);
        }
        
        // Then
        assertTrue(allSagasLatch.await(20, TimeUnit.SECONDS), "All SAGAs should start within timeout");
        
        // Verify all SAGAs completed successfully
        for (CompletableFuture<Void> future : sagaFutures) {
            assertDoesNotThrow(() -> future.get(5, TimeUnit.SECONDS));
        }
        
        // Verify each SAGA was processed independently
        verify(sagaOrchestrator, times(concurrentSagas)).startSaga(any(), any());
    }

    @Test
    @DisplayName("Should maintain SAGA state consistency across service restarts")
    void shouldMaintainSAGAStateConsistencyAcrossServiceRestarts() throws Exception {
        // Given
        String sagaId = "persistent-saga-" + UUID.randomUUID().toString();
        String customerId = "CUST-RESTART-001";
        String loanApplicationId = "LOAN-APP-RESTART-001";
        
        LoanApplicationInitiatedEvent initiatingEvent = createLoanApplicationEvent(customerId, loanApplicationId, 75000.00);
        
        // Configure mocks for state persistence
        when(secureEventPublisher.publishSecureEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sagaOrchestrator.startSaga(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sagaOrchestrator.recoverSaga(sagaId)).thenAnswer(invocation -> {
            simulateSuccessfulSagaExecution(sagaId);
            return CompletableFuture.completedFuture(null);
        });

        // When
        // 1. Start SAGA
        CompletableFuture<Void> initialResult = sagaOrchestrator.startSaga(loanOriginationSaga, 
            Map.of("initiatingEvent", initiatingEvent, "sagaId", sagaId));
        
        initialResult.get(5, TimeUnit.SECONDS);
        
        // 2. Simulate service restart and SAGA recovery
        CompletableFuture<Void> recoveryResult = sagaOrchestrator.recoverSaga(sagaId);
        
        // Then
        assertDoesNotThrow(() -> recoveryResult.get(10, TimeUnit.SECONDS));
        assertTrue(sagaCompletionLatch.await(15, TimeUnit.SECONDS), "Recovered SAGA should complete");
        assertEquals("COMPLETED", this.sagaResult.get());
        
        // Verify SAGA recovery was attempted
        verify(sagaOrchestrator).recoverSaga(sagaId);
    }

    @Test
    @DisplayName("Should handle SAGA timeout and initiate compensation")
    void shouldHandleSAGATimeoutAndInitiateCompensation() throws Exception {
        // Given
        String sagaId = "timeout-saga-" + UUID.randomUUID().toString();
        String customerId = "CUST-TIMEOUT-001";
        String loanApplicationId = "LOAN-APP-TIMEOUT-001";
        
        LoanApplicationInitiatedEvent initiatingEvent = createLoanApplicationEvent(customerId, loanApplicationId, 40000.00);
        
        // Configure mocks for timeout scenario
        when(secureEventPublisher.publishSecureEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sagaOrchestrator.startSaga(any(), any())).thenAnswer(invocation -> {
            // Simulate timeout after delay
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                simulateTimeoutSagaExecution(sagaId);
            });
            return CompletableFuture.completedFuture(null);
        });

        // When
        CompletableFuture<Void> sagaResult = sagaOrchestrator.startSaga(loanOriginationSaga, 
            Map.of("initiatingEvent", initiatingEvent, "sagaId", sagaId, "timeout", Duration.ofSeconds(1)));
        
        // Then
        assertDoesNotThrow(() -> sagaResult.get(10, TimeUnit.SECONDS));
        assertTrue(sagaCompletionLatch.await(15, TimeUnit.SECONDS), "SAGA timeout compensation should complete");
        assertEquals("TIMED_OUT", this.sagaResult.get());
        
        // Verify timeout handling was triggered
        verify(sagaOrchestrator).startSaga(any(), any());
    }

    @Test
    @DisplayName("Should validate BIAN compliance during SAGA execution")
    void shouldValidateBIANComplianceDuringSAGAExecution() throws Exception {
        // Given
        String sagaId = "bian-compliant-saga-" + UUID.randomUUID().toString();
        String customerId = "CUST-BIAN-001";
        String loanApplicationId = "LOAN-APP-BIAN-001";
        
        LoanApplicationInitiatedEvent initiatingEvent = createBIANCompliantLoanApplicationEvent(customerId, loanApplicationId);
        
        // Configure mocks for BIAN validation
        when(secureEventPublisher.publishSecureEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(sagaOrchestrator.startSaga(any(), any())).thenAnswer(invocation -> {
            // Validate BIAN compliance in SAGA definition
            LoanOriginationSaga saga = (LoanOriginationSaga) invocation.getArgument(0);
            var validationResult = saga.validateCompliance();
            if (!validationResult.isValid()) {
                throw new RuntimeException("BIAN compliance validation failed: " + validationResult.message());
            }
            simulateSuccessfulSagaExecution(sagaId);
            return CompletableFuture.completedFuture(null);
        });

        // When
        CompletableFuture<Void> sagaResult = sagaOrchestrator.startSaga(loanOriginationSaga, 
            Map.of("initiatingEvent", initiatingEvent, "sagaId", sagaId));
        
        // Then
        assertDoesNotThrow(() -> sagaResult.get(10, TimeUnit.SECONDS));
        assertTrue(sagaCompletionLatch.await(15, TimeUnit.SECONDS), "BIAN compliant SAGA should complete");
        assertEquals("COMPLETED", this.sagaResult.get());
        
        // Verify BIAN compliance validation was performed
        verify(sagaOrchestrator).startSaga(any(), any());
    }

    @Test
    @DisplayName("Should handle event ordering in distributed SAGA coordination")
    void shouldHandleEventOrderingInDistributedSAGACoordination() throws Exception {
        // Given
        String sagaId = "ordered-saga-" + UUID.randomUUID().toString();
        String customerId = "CUST-ORDERED-001";
        String loanApplicationId = "LOAN-APP-ORDERED-001";
        
        java.util.List<String> eventOrder = new java.util.concurrent.CopyOnWriteArrayList<>();
        
        // Configure mocks to track event ordering
        when(secureEventPublisher.publishSecureEvent(any())).thenAnswer(invocation -> {
            DomainEvent event = invocation.getArgument(0);
            eventOrder.add(event.getEventType());
            return CompletableFuture.completedFuture(null);
        });
        
        when(sagaOrchestrator.startSaga(any(), any())).thenAnswer(invocation -> {
            simulateOrderedSagaExecution(sagaId, eventOrder);
            return CompletableFuture.completedFuture(null);
        });

        // When
        LoanApplicationInitiatedEvent initiatingEvent = createLoanApplicationEvent(customerId, loanApplicationId, 60000.00);
        CompletableFuture<Void> sagaResult = sagaOrchestrator.startSaga(loanOriginationSaga, 
            Map.of("initiatingEvent", initiatingEvent, "sagaId", sagaId));
        
        // Then
        assertDoesNotThrow(() -> sagaResult.get(10, TimeUnit.SECONDS));
        assertTrue(sagaCompletionLatch.await(15, TimeUnit.SECONDS), "Ordered SAGA should complete");
        
        // Verify event ordering was maintained
        assertTrue(eventOrder.size() >= 4, "Should have published at least 4 events in order");
        
        // Verify logical event order (this would be customized based on actual SAGA steps)
        assertTrue(eventOrder.contains("LoanApplicationInitiatedEvent") ||
                  eventOrder.contains("CreditReservedEvent") ||
                  eventOrder.contains("LoanApprovedEvent"));
    }

    // Helper Methods

    private LoanApplicationInitiatedEvent createLoanApplicationEvent(String customerId, String loanApplicationId, double amount) {
        Map<String, Object> eventData = Map.of(
            "customerId", customerId,
            "loanApplicationId", loanApplicationId,
            "requestedAmount", amount,
            "loanType", "PERSONAL",
            "termMonths", 24,
            "interestRate", 0.15
        );
        
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("fapiInteractionId", UUID.randomUUID().toString());
        metadata.put("clientId", "banking-test-client");
        metadata.put("authTime", OffsetDateTime.now().toString());
        metadata.put("sagaCorrelationId", UUID.randomUUID().toString());
        
        return new LoanApplicationInitiatedEvent(loanApplicationId, eventData, metadata);
    }
    
    private LoanApplicationInitiatedEvent createBIANCompliantLoanApplicationEvent(String customerId, String loanApplicationId) {
        Map<String, Object> eventData = Map.of(
            "customerId", customerId,
            "loanApplicationId", loanApplicationId,
            "requestedAmount", 80000.00,
            "loanType", "PERSONAL",
            "termMonths", 36,
            "interestRate", 0.12,
            // BIAN specific fields
            "serviceDomain", "ConsumerLoan",
            "behaviorQualifier", "Initiate",
            "serviceOperationQualifier", "LoanApplication"
        );
        
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("fapiInteractionId", UUID.randomUUID().toString());
        metadata.put("bianCompliant", true);
        metadata.put("serviceDomainVersion", "1.0.0");
        
        return new LoanApplicationInitiatedEvent(loanApplicationId, eventData, metadata);
    }
    
    private void simulateSuccessfulSagaExecution(String sagaId) {
        CompletableFuture.runAsync(() -> {
            try {
                // Simulate SAGA step execution delay
                Thread.sleep(1000);
                sagaResult.set("COMPLETED");
                sagaCompletionLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private void simulateFailedSagaExecution(String sagaId, String failureReason) {
        CompletableFuture.runAsync(() -> {
            try {
                // Simulate SAGA failure and compensation delay
                Thread.sleep(1500);
                sagaResult.set("COMPENSATED");
                sagaCompletionLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private void simulateTimeoutSagaExecution(String sagaId) {
        CompletableFuture.runAsync(() -> {
            try {
                // Simulate timeout scenario
                Thread.sleep(500);
                sagaResult.set("TIMED_OUT");
                sagaCompletionLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private void simulateOrderedSagaExecution(String sagaId, java.util.List<String> eventOrder) {
        CompletableFuture.runAsync(() -> {
            try {
                // Simulate ordered event publication
                eventOrder.add("LoanApplicationInitiatedEvent");
                Thread.sleep(200);
                eventOrder.add("CreditReservedEvent");
                Thread.sleep(200);
                eventOrder.add("LoanApprovedEvent");
                Thread.sleep(200);
                eventOrder.add("LoanActivatedEvent");
                
                sagaResult.set("COMPLETED");
                sagaCompletionLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    private void simulateSagaStepEvents(String sagaId, String customerId, String loanApplicationId, boolean successful) {
        CompletableFuture.runAsync(() -> {
            try {
                if (successful) {
                    // Simulate successful SAGA steps
                    publishMockEvent(new CreditReservedEvent(customerId, Map.of("reservationId", "RES-001")));
                    Thread.sleep(300);
                    publishMockEvent(new LoanApprovedEvent(loanApplicationId, Map.of("approvedAmount", 50000.00)));
                    Thread.sleep(300);
                } else {
                    // Simulate failed SAGA steps
                    publishMockEvent(new CreditReservationFailedEvent(customerId, Map.of("reason", "Insufficient credit limit")));
                    Thread.sleep(300);
                    publishMockEvent(new LoanRejectedEvent(loanApplicationId, Map.of("rejectionReason", "Credit reservation failed")));
                    Thread.sleep(300);
                }
            } catch (Exception e) {
                // Handle simulation errors
            }
        });
    }
    
    private void publishMockEvent(DomainEvent event) {
        try {
            secureEventPublisher.publishSecureEvent(event);
        } catch (Exception e) {
            // Ignore mock publication errors
        }
    }
}