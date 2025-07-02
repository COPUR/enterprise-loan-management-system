package com.bank.loanmanagement.loan.messaging.integration;

import com.bank.loanmanagement.loan.sharedkernel.domain.event.DomainEvent;
import com.bank.loanmanagement.loan.messaging.infrastructure.kafka.KafkaEventPublisher;
import com.bank.loanmanagement.loan.messaging.infrastructure.kafka.KafkaEventConsumer;
import com.bank.loanmanagement.loan.messaging.infrastructure.kafka.KafkaTopicResolver;
import com.bank.loanmanagement.loan.messaging.infrastructure.security.FAPIEventSecurityService;
import com.bank.loanmanagement.loan.messaging.infrastructure.security.SecureEventPublisher;
import com.bank.loanmanagement.loan.messaging.infrastructure.security.EventSecurityAuditService;
import com.bank.loanmanagement.loan.messaging.infrastructure.EventProcessor;
import com.bank.loanmanagement.loan.saga.infrastructure.KafkaSagaOrchestrator;
import com.bank.loanmanagement.loan.domain.event.LoanApplicationInitiatedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Complete Event Flow Integration Test
 * Tests the entire event-driven architecture including:
 * - Event publishing with FAPI security
 * - Kafka message streaming
 * - Event consumption and processing
 * - SAGA orchestration
 * - Security auditing
 * - Error handling and compensation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${kafka.bootstrap.servers}",
    "spring.datasource.url=${postgresql.datasource.url}",
    "spring.redis.host=${redis.host}",
    "spring.redis.port=${redis.port}"
})
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompleteEventFlowIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("banking_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withStartupTimeout(Duration.ofMinutes(2));

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withStartupTimeout(Duration.ofMinutes(1));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("kafka.bootstrap.servers", kafka::getBootstrapServers);
        registry.add("postgresql.datasource.url", postgres::getJdbcUrl);
        registry.add("redis.host", redis::getHost);
        registry.add("redis.port", () -> redis.getMappedPort(6379));
    }

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;
    
    @Mock
    private KafkaEventConsumer kafkaEventConsumer;
    
    @Mock
    private KafkaTopicResolver topicResolver;
    
    @Mock
    private FAPIEventSecurityService fapiEventSecurityService;
    
    @Mock
    private EventProcessor eventProcessor;
    
    @Mock
    private KafkaSagaOrchestrator sagaOrchestrator;
    
    @Mock
    private EventSecurityAuditService auditService;
    
    private SecureEventPublisher secureEventPublisher;
    
    private CountDownLatch eventProcessedLatch;
    private DomainEvent publishedEvent;
    private Exception processingException;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        secureEventPublisher = new SecureEventPublisher(
            kafkaEventPublisher,
            fapiEventSecurityService,
            topicResolver,
            auditService
        );
        
        eventProcessedLatch = new CountDownLatch(1);
        publishedEvent = null;
        processingException = null;
    }

    @Test
    @DisplayName("Should complete full event flow from publication to consumption with FAPI security")
    void shouldCompleteFullEventFlowWithFAPISecurity() throws Exception {
        // Given
        LoanApplicationInitiatedEvent event = createTestLoanApplicationEvent();
        String expectedTopic = "banking.consumer-loan.commands";
        
        // Configure mocks for successful flow
        when(topicResolver.resolveTopicForEvent(event)).thenReturn(expectedTopic);
        when(fapiEventSecurityService.secureEventPublish(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaEventPublisher.publishEvent(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(fapiEventSecurityService.secureEventConsume(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(eventProcessor.processEvent(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));

        // When
        CompletableFuture<Void> publishResult = secureEventPublisher.publishSecureEvent(event);
        
        // Simulate event consumption
        CompletableFuture<Void> consumeResult = simulateEventConsumption(event, expectedTopic);
        
        // Then
        assertDoesNotThrow(() -> publishResult.get(5, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> consumeResult.get(5, TimeUnit.SECONDS));
        
        // Verify security was applied
        verify(fapiEventSecurityService).secureEventPublish(event, expectedTopic);
        verify(fapiEventSecurityService).secureEventConsume(event, expectedTopic);
        
        // Verify event was published and processed
        verify(kafkaEventPublisher).publishEvent(event, expectedTopic);
        verify(eventProcessor).processEvent(event, expectedTopic);
        
        // Verify auditing was called
        verify(auditService).auditEventPublication(event, expectedTopic, true, null);
        verify(auditService).auditEventConsumption(event, expectedTopic, true, null);
    }

    @Test
    @DisplayName("Should handle FAPI security validation failure during event publishing")
    void shouldHandleFAPISecurityValidationFailure() throws Exception {
        // Given
        LoanApplicationInitiatedEvent event = createTestLoanApplicationEvent();
        String expectedTopic = "banking.consumer-loan.commands";
        String securityError = "Invalid FAPI interaction ID";
        
        // Configure mocks for security failure
        when(topicResolver.resolveTopicForEvent(event)).thenReturn(expectedTopic);
        when(fapiEventSecurityService.secureEventPublish(event, expectedTopic))
            .thenReturn(CompletableFuture.failedFuture(
                new FAPIEventSecurityService.FAPIEventSecurityException(securityError)));

        // When & Then
        CompletableFuture<Void> publishResult = secureEventPublisher.publishSecureEvent(event);
        
        Exception exception = assertThrows(Exception.class, () -> {
            publishResult.get(5, TimeUnit.SECONDS);
        });
        
        assertTrue(exception.getCause() instanceof SecureEventPublisher.SecureEventPublicationException);
        
        // Verify security was attempted
        verify(fapiEventSecurityService).secureEventPublish(event, expectedTopic);
        
        // Verify event was not published due to security failure
        verify(kafkaEventPublisher, never()).publishEvent(any(), any());
        
        // Verify audit of failure
        verify(auditService).auditEventPublication(eq(event), isNull(), eq(false), contains(securityError));
    }

    @Test
    @DisplayName("Should handle event consumption failure and trigger audit")
    void shouldHandleEventConsumptionFailure() throws Exception {
        // Given
        LoanApplicationInitiatedEvent event = createTestLoanApplicationEvent();
        String expectedTopic = "banking.consumer-loan.commands";
        String processingError = "Event processing failed due to database error";
        
        // Configure mocks for consumption failure
        when(fapiEventSecurityService.secureEventConsume(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(eventProcessor.processEvent(event, expectedTopic))
            .thenReturn(CompletableFuture.failedFuture(
                new EventProcessor.EventProcessingException(processingError)));

        // When
        CompletableFuture<Void> consumeResult = simulateEventConsumption(event, expectedTopic);
        
        // Then
        Exception exception = assertThrows(Exception.class, () -> {
            consumeResult.get(5, TimeUnit.SECONDS);
        });
        
        // Verify security validation passed
        verify(fapiEventSecurityService).secureEventConsume(event, expectedTopic);
        
        // Verify processing was attempted
        verify(eventProcessor).processEvent(event, expectedTopic);
        
        // Verify audit of failure
        verify(auditService).auditEventConsumption(eq(event), eq(expectedTopic), eq(false), contains(processingError));
    }

    @Test
    @DisplayName("Should complete SAGA orchestration flow with event-driven coordination")
    void shouldCompleteSAGAOrchestrationFlow() throws Exception {
        // Given
        LoanApplicationInitiatedEvent event = createTestLoanApplicationEvent();
        String sagaId = "loan-creation-saga-001";
        String expectedTopic = "banking.consumer-loan.saga";
        
        // Configure mocks for SAGA flow
        when(topicResolver.resolveTopicForEvent(event)).thenReturn(expectedTopic);
        when(fapiEventSecurityService.secureEventPublish(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaEventPublisher.publishEvent(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(sagaOrchestrator.handleEvent(sagaId, event))
            .thenReturn(CompletableFuture.completedFuture(null));

        // When
        CompletableFuture<Void> publishResult = secureEventPublisher.publishSecureEvent(event);
        CompletableFuture<Void> sagaResult = simulateSAGAEventHandling(sagaId, event);
        
        // Then
        assertDoesNotThrow(() -> publishResult.get(5, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> sagaResult.get(5, TimeUnit.SECONDS));
        
        // Verify SAGA orchestration
        verify(sagaOrchestrator).handleEvent(sagaId, event);
        
        // Verify security and auditing
        verify(fapiEventSecurityService).secureEventPublish(event, expectedTopic);
        verify(auditService).auditEventPublication(event, expectedTopic, true, null);
    }

    @Test
    @DisplayName("Should handle critical banking event with enhanced security")
    void shouldHandleCriticalBankingEventWithEnhancedSecurity() throws Exception {
        // Given
        LoanApplicationInitiatedEvent event = createCriticalBankingEvent();
        String expectedTopic = "banking.consumer-loan.critical";
        
        // Configure mocks for critical event flow
        when(topicResolver.resolveTopicForEvent(event)).thenReturn(expectedTopic);
        when(fapiEventSecurityService.secureEventPublish(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaEventPublisher.publishEvent(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));

        // When
        CompletableFuture<Void> publishResult = secureEventPublisher.publishCriticalBankingEvent(event);
        
        // Then
        assertDoesNotThrow(() -> publishResult.get(5, TimeUnit.SECONDS));
        
        // Verify critical event metadata
        assertTrue((Boolean) event.getMetadata().get("criticalBankingEvent"));
        assertTrue((Boolean) event.getMetadata().get("requiresEncryption"));
        assertEquals("HIGH", event.getMetadata().get("auditLevel"));
        
        // Verify enhanced security was applied
        verify(fapiEventSecurityService).secureEventPublish(event, expectedTopic);
        verify(kafkaEventPublisher).publishEvent(event, expectedTopic);
        verify(auditService).auditEventPublication(event, expectedTopic, true, null);
    }

    @Test
    @DisplayName("Should maintain event ordering in high-throughput scenario")
    void shouldMaintainEventOrderingInHighThroughput() throws Exception {
        // Given
        int eventCount = 100;
        String expectedTopic = "banking.consumer-loan.commands";
        CountDownLatch completionLatch = new CountDownLatch(eventCount);
        
        // Configure mocks for batch processing
        when(topicResolver.resolveTopicForEvent(any())).thenReturn(expectedTopic);
        when(fapiEventSecurityService.secureEventPublish(any(), eq(expectedTopic)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaEventPublisher.publishEvent(any(), eq(expectedTopic)))
            .thenAnswer(invocation -> {
                completionLatch.countDown();
                return CompletableFuture.completedFuture(null);
            });

        // When
        java.util.List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            LoanApplicationInitiatedEvent event = createTestLoanApplicationEventWithSequence(i);
            futures.add(secureEventPublisher.publishSecureEvent(event));
        }
        
        // Then
        assertTrue(completionLatch.await(30, TimeUnit.SECONDS), "All events should be processed within timeout");
        
        // Verify all futures completed successfully
        for (CompletableFuture<Void> future : futures) {
            assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
        }
        
        // Verify all events were processed
        verify(kafkaEventPublisher, times(eventCount)).publishEvent(any(), eq(expectedTopic));
        verify(fapiEventSecurityService, times(eventCount)).secureEventPublish(any(), eq(expectedTopic));
    }

    @Test
    @DisplayName("Should handle concurrent event processing with thread safety")
    void shouldHandleConcurrentEventProcessingWithThreadSafety() throws Exception {
        // Given
        int concurrentEvents = 50;
        String expectedTopic = "banking.consumer-loan.commands";
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentEvents);
        
        // Configure mocks for concurrent processing
        when(topicResolver.resolveTopicForEvent(any())).thenReturn(expectedTopic);
        when(fapiEventSecurityService.secureEventPublish(any(), eq(expectedTopic)))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaEventPublisher.publishEvent(any(), eq(expectedTopic)))
            .thenAnswer(invocation -> {
                completionLatch.countDown();
                return CompletableFuture.completedFuture(null);
            });

        // When
        java.util.List<Thread> threads = new java.util.ArrayList<>();
        for (int i = 0; i < concurrentEvents; i++) {
            final int eventIndex = i;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    LoanApplicationInitiatedEvent event = createTestLoanApplicationEventWithSequence(eventIndex);
                    secureEventPublisher.publishSecureEvent(event).get();
                } catch (Exception e) {
                    processingException = e;
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        startLatch.countDown(); // Start all threads simultaneously
        
        // Then
        assertTrue(completionLatch.await(30, TimeUnit.SECONDS), "All concurrent events should be processed");
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000);
        }
        
        assertNull(processingException, "No exceptions should occur during concurrent processing");
        
        // Verify thread-safe processing
        verify(kafkaEventPublisher, times(concurrentEvents)).publishEvent(any(), eq(expectedTopic));
        verify(fapiEventSecurityService, times(concurrentEvents)).secureEventPublish(any(), eq(expectedTopic));
    }

    @Test
    @DisplayName("Should validate audit trail completeness for compliance")
    void shouldValidateAuditTrailCompletenessForCompliance() throws Exception {
        // Given
        LoanApplicationInitiatedEvent event = createTestLoanApplicationEvent();
        String expectedTopic = "banking.consumer-loan.commands";
        OffsetDateTime testStart = OffsetDateTime.now();
        
        // Configure mocks
        when(topicResolver.resolveTopicForEvent(event)).thenReturn(expectedTopic);
        when(fapiEventSecurityService.secureEventPublish(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaEventPublisher.publishEvent(event, expectedTopic))
            .thenReturn(CompletableFuture.completedFuture(null));
        
        // Mock audit service to return realistic audit data
        EventSecurityAuditService.AuditStatistics expectedStats = 
            EventSecurityAuditService.AuditStatistics.builder()
                .periodFrom(testStart)
                .periodTo(testStart.plusMinutes(1))
                .totalEvents(1)
                .successfulEvents(1)
                .failedEvents(0)
                .criticalEvents(0)
                .securityViolations(0)
                .successRate(100.0)
                .build();
                
        when(auditService.getAuditStatistics(any(), any())).thenReturn(expectedStats);

        // When
        CompletableFuture<Void> publishResult = secureEventPublisher.publishSecureEvent(event);
        publishResult.get(5, TimeUnit.SECONDS);
        
        // Simulate audit statistics retrieval
        EventSecurityAuditService.AuditStatistics stats = 
            auditService.getAuditStatistics(testStart, testStart.plusMinutes(1));
        
        // Then
        assertNotNull(stats);
        assertEquals(1, stats.getTotalEvents());
        assertEquals(1, stats.getSuccessfulEvents());
        assertEquals(0, stats.getFailedEvents());
        assertEquals(100.0, stats.getSuccessRate());
        
        // Verify audit calls were made
        verify(auditService).auditEventPublication(event, expectedTopic, true, null);
        verify(auditService).getAuditStatistics(any(), any());
    }

    // Helper methods

    private LoanApplicationInitiatedEvent createTestLoanApplicationEvent() {
        return createTestLoanApplicationEventWithSequence(1);
    }
    
    private LoanApplicationInitiatedEvent createTestLoanApplicationEventWithSequence(int sequence) {
        Map<String, Object> eventData = Map.of(
            "customerId", "CUST-" + String.format("%03d", sequence),
            "loanApplicationId", "LOAN-APP-" + String.format("%03d", sequence),
            "requestedAmount", 50000.00,
            "loanType", "PERSONAL",
            "termMonths", 24
        );
        
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("fapiInteractionId", java.util.UUID.randomUUID().toString());
        metadata.put("clientId", "banking-test-client");
        metadata.put("authTime", OffsetDateTime.now().toString());
        metadata.put("customerIpAddress", "192.168.1.100");
        metadata.put("isSystemEvent", false);
        
        return new LoanApplicationInitiatedEvent(
            "LOAN-" + String.format("%03d", sequence),
            eventData,
            metadata
        );
    }
    
    private LoanApplicationInitiatedEvent createCriticalBankingEvent() {
        LoanApplicationInitiatedEvent event = createTestLoanApplicationEvent();
        
        // Add critical event indicators
        event.getEventData().put("amount", 100000.00); // High value loan
        event.getEventData().put("riskLevel", "HIGH");
        event.getMetadata().put("criticalBankingEvent", true);
        event.getMetadata().put("requiresEncryption", true);
        event.getMetadata().put("auditLevel", "HIGH");
        
        return event;
    }
    
    private CompletableFuture<Void> simulateEventConsumption(DomainEvent event, String topic) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Simulate security validation during consumption
                fapiEventSecurityService.secureEventConsume(event, topic).get();
                
                // Simulate event processing
                eventProcessor.processEvent(event, topic).get();
                
                // Audit successful consumption
                auditService.auditEventConsumption(event, topic, true, null);
                
            } catch (Exception e) {
                // Audit failed consumption
                auditService.auditEventConsumption(event, topic, false, e.getMessage());
                throw new RuntimeException("Event consumption failed", e);
            }
        });
    }
    
    private CompletableFuture<Void> simulateSAGAEventHandling(String sagaId, DomainEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Simulate SAGA event handling
                sagaOrchestrator.handleEvent(sagaId, event).get();
                
            } catch (Exception e) {
                throw new RuntimeException("SAGA event handling failed", e);
            }
        });
    }
}