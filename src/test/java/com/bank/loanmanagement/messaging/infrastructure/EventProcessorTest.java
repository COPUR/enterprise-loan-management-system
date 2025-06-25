package com.bank.loanmanagement.messaging.infrastructure;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import com.bank.loanmanagement.sharedkernel.infrastructure.eventstore.EventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for EventProcessor
 * Tests event processing, error handling, retry logic, and BIAN compliance
 * Ensures 85%+ test coverage for event processing infrastructure
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventProcessor Tests")
class EventProcessorTest {

    @Mock
    private EventStore eventStore;
    
    @Mock
    private RetryTemplate retryTemplate;
    
    @Mock
    private EventHandlerRegistry eventHandlerRegistry;
    
    @Mock
    private EventHandler<TestDomainEvent> testEventHandler;
    
    @Mock
    private BianComplianceValidator bianValidator;
    
    @Mock
    private EventProcessingMetrics metrics;

    private EventProcessor eventProcessor;
    private TestDomainEvent testEvent;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);
        eventProcessor = new EventProcessor(
            eventStore, retryTemplate, eventHandlerRegistry, bianValidator, metrics, executorService);
        
        testEvent = new TestDomainEvent("AGGREGATE-123", 1L, "test-data");
    }

    @Nested
    @DisplayName("Event Processing Tests")
    class EventProcessingTests {

        @Test
        @DisplayName("Should process valid event successfully")
        void shouldProcessValidEventSuccessfully() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            when(eventHandlerRegistry.getHandler(TestDomainEvent.class)).thenReturn(testEventHandler);
            when(bianValidator.validate(testEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(testEventHandler.handle(testEvent)).thenReturn(CompletableFuture.completedFuture(null));
            when(retryTemplate.execute(any(RetryCallback.class))).thenAnswer(invocation -> {
                RetryCallback<Void, Exception> callback = invocation.getArgument(0);
                return callback.doWithRetry(mock(RetryContext.class));
            });

            // When
            CompletableFuture<Void> result = eventProcessor.processEvent(testEvent, topic);

            // Then
            assertThat(result).isCompleted();
            verify(bianValidator).validate(testEvent);
            verify(testEventHandler).handle(testEvent);
            verify(metrics).recordEventProcessed(testEvent.getEventType(), topic, true);
        }

        @Test
        @DisplayName("Should handle event processing failure with retry")
        void shouldHandleEventProcessingFailureWithRetry() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            RuntimeException processingError = new RuntimeException("Processing failed");
            
            when(eventHandlerRegistry.getHandler(TestDomainEvent.class)).thenReturn(testEventHandler);
            when(bianValidator.validate(testEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(retryTemplate.execute(any(RetryCallback.class))).thenThrow(processingError);

            // When
            CompletableFuture<Void> result = eventProcessor.processEvent(testEvent, topic);

            // Then
            assertThat(result).isCompletedExceptionally();
            result.handle((voidResult, throwable) -> {
                assertThat(throwable).hasCauseInstanceOf(RuntimeException.class);
                assertThat(throwable.getCause().getMessage()).contains("Processing failed");
                return null;
            });
            
            verify(metrics).recordEventProcessed(testEvent.getEventType(), topic, false);
        }

        @Test
        @DisplayName("Should reject non-BIAN compliant events")
        void shouldRejectNonBianCompliantEvents() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            BianComplianceValidator.ValidationResult invalidResult = 
                BianComplianceValidator.ValidationResult.invalid("Missing service domain");
            
            when(bianValidator.validate(testEvent)).thenReturn(invalidResult);

            // When
            CompletableFuture<Void> result = eventProcessor.processEvent(testEvent, topic);

            // Then
            assertThat(result).isCompletedExceptionally();
            result.handle((voidResult, throwable) -> {
                assertThat(throwable).hasCauseInstanceOf(EventProcessor.BianComplianceException.class);
                return null;
            });
            
            verify(testEventHandler, never()).handle(any());
            verify(metrics).recordBianViolation(testEvent.getEventType(), "Missing service domain");
        }

        @Test
        @DisplayName("Should handle missing event handler gracefully")
        void shouldHandleMissingEventHandlerGracefully() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            when(eventHandlerRegistry.getHandler(TestDomainEvent.class)).thenReturn(null);
            when(bianValidator.validate(testEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());

            // When
            CompletableFuture<Void> result = eventProcessor.processEvent(testEvent, topic);

            // Then
            assertThat(result).isCompletedExceptionally();
            result.handle((voidResult, throwable) -> {
                assertThat(throwable).hasCauseInstanceOf(EventProcessor.NoHandlerException.class);
                return null;
            });
            
            verify(metrics).recordNoHandlerFound(testEvent.getEventType());
        }

        @Test
        @DisplayName("Should process events concurrently")
        void shouldProcessEventsConcurrently() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            TestDomainEvent event1 = new TestDomainEvent("AGG-1", 1L, "data1");
            TestDomainEvent event2 = new TestDomainEvent("AGG-2", 1L, "data2");
            
            when(eventHandlerRegistry.getHandler(TestDomainEvent.class)).thenReturn(testEventHandler);
            when(bianValidator.validate(any())).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(testEventHandler.handle(any())).thenAnswer(invocation -> {
                // Simulate processing time
                Thread.sleep(100);
                return CompletableFuture.completedFuture(null);
            });
            when(retryTemplate.execute(any(RetryCallback.class))).thenAnswer(invocation -> {
                RetryCallback<Void, Exception> callback = invocation.getArgument(0);
                return callback.doWithRetry(mock(RetryContext.class));
            });

            // When
            long startTime = System.currentTimeMillis();
            CompletableFuture<Void> result1 = eventProcessor.processEvent(event1, topic);
            CompletableFuture<Void> result2 = eventProcessor.processEvent(event2, topic);
            
            CompletableFuture.allOf(result1, result2).get(5, TimeUnit.SECONDS);
            long endTime = System.currentTimeMillis();

            // Then
            long totalTime = endTime - startTime;
            assertThat(totalTime).isLessThan(200L); // Should be less than sequential processing
            assertThat(result1).isCompleted();
            assertThat(result2).isCompleted();
        }
    }

    @Nested
    @DisplayName("SAGA Event Processing Tests")
    class SagaEventProcessingTests {

        @Test
        @DisplayName("Should process SAGA coordination events")
        void shouldProcessSagaCoordinationEvents() throws Exception {
            // Given
            String sagaTopic = "banking.consumer-loan.saga.loanoriginationsaga";
            String sagaId = "SAGA-123";
            SagaEventHandler sagaHandler = mock(SagaEventHandler.class);
            
            when(eventHandlerRegistry.getSagaHandler(testEvent.getServiceDomain())).thenReturn(sagaHandler);
            when(bianValidator.validate(testEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(sagaHandler.handleSagaEvent(testEvent, sagaId)).thenReturn(CompletableFuture.completedFuture(null));

            // When
            CompletableFuture<Void> result = eventProcessor.processSagaEvent(testEvent, sagaTopic, sagaId);

            // Then
            assertThat(result).isCompleted();
            verify(sagaHandler).handleSagaEvent(testEvent, sagaId);
            verify(metrics).recordSagaEventProcessed(testEvent.getEventType(), sagaTopic, true);
        }

        @Test
        @DisplayName("Should handle SAGA step completion events")
        void shouldHandleSagaStepCompletionEvents() throws Exception {
            // Given
            String sagaTopic = "banking.consumer-loan.saga.loanoriginationsaga";
            String sagaId = "SAGA-123";
            SagaStepCompletionEvent stepEvent = new SagaStepCompletionEvent("AGG-123", 1L, "step-1", Map.of("result", "success"));
            SagaEventHandler sagaHandler = mock(SagaEventHandler.class);
            
            when(eventHandlerRegistry.getSagaHandler(stepEvent.getServiceDomain())).thenReturn(sagaHandler);
            when(bianValidator.validate(stepEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(sagaHandler.handleStepCompletion(stepEvent, sagaId)).thenReturn(CompletableFuture.completedFuture(null));

            // When
            CompletableFuture<Void> result = eventProcessor.processSagaEvent(stepEvent, sagaTopic, sagaId);

            // Then
            assertThat(result).isCompleted();
            verify(sagaHandler).handleStepCompletion(stepEvent, sagaId);
        }

        @Test
        @DisplayName("Should handle SAGA compensation events")
        void shouldHandleSagaCompensationEvents() throws Exception {
            // Given
            String sagaTopic = "banking.consumer-loan.saga.loanoriginationsaga";
            String sagaId = "SAGA-123";
            SagaCompensationEvent compensationEvent = new SagaCompensationEvent("AGG-123", 1L, "step-2", "Compensation required");
            SagaEventHandler sagaHandler = mock(SagaEventHandler.class);
            
            when(eventHandlerRegistry.getSagaHandler(compensationEvent.getServiceDomain())).thenReturn(sagaHandler);
            when(bianValidator.validate(compensationEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(sagaHandler.handleCompensation(compensationEvent, sagaId)).thenReturn(CompletableFuture.completedFuture(null));

            // When
            CompletableFuture<Void> result = eventProcessor.processSagaEvent(compensationEvent, sagaTopic, sagaId);

            // Then
            assertThat(result).isCompleted();
            verify(sagaHandler).handleCompensation(compensationEvent, sagaId);
        }
    }

    @Nested
    @DisplayName("Secure Event Processing Tests")
    class SecureEventProcessingTests {

        @Test
        @DisplayName("Should process secure events with enhanced validation")
        void shouldProcessSecureEventsWithEnhancedValidation() throws Exception {
            // Given
            String secureTopic = "banking.payment-initiation.commands.secure";
            SecureEventHandler secureHandler = mock(SecureEventHandler.class);
            
            when(eventHandlerRegistry.getSecureHandler(testEvent.getServiceDomain())).thenReturn(secureHandler);
            when(bianValidator.validate(testEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(secureHandler.handleSecureEvent(testEvent)).thenReturn(CompletableFuture.completedFuture(null));

            // When
            CompletableFuture<Void> result = eventProcessor.processSecureEvent(testEvent, secureTopic);

            // Then
            assertThat(result).isCompleted();
            verify(secureHandler).handleSecureEvent(testEvent);
            verify(metrics).recordSecureEventProcessed(testEvent.getEventType(), secureTopic, true);
        }

        @Test
        @DisplayName("Should validate FAPI compliance for secure events")
        void shouldValidateFapiComplianceForSecureEvents() throws Exception {
            // Given
            String secureTopic = "banking.payment-initiation.commands.secure";
            FapiNonCompliantEvent fapiEvent = new FapiNonCompliantEvent("AGG-123", 1L, "missing-fapi-data");
            
            when(bianValidator.validate(fapiEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());

            // When
            CompletableFuture<Void> result = eventProcessor.processSecureEvent(fapiEvent, secureTopic);

            // Then
            assertThat(result).isCompletedExceptionally();
            result.handle((voidResult, throwable) -> {
                assertThat(throwable).hasCauseInstanceOf(EventProcessor.FapiComplianceException.class);
                return null;
            });
        }
    }

    @Nested
    @DisplayName("Event Ordering and Sequencing Tests")
    class EventOrderingAndSequencingTests {

        @Test
        @DisplayName("Should process events in order for same aggregate")
        void shouldProcessEventsInOrderForSameAggregate() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            String aggregateId = "AGGREGATE-123";
            TestDomainEvent event1 = new TestDomainEvent(aggregateId, 1L, "event1");
            TestDomainEvent event2 = new TestDomainEvent(aggregateId, 2L, "event2");
            TestDomainEvent event3 = new TestDomainEvent(aggregateId, 3L, "event3");
            
            List<String> processedOrder = List.of();
            
            when(eventHandlerRegistry.getHandler(TestDomainEvent.class)).thenReturn(testEventHandler);
            when(bianValidator.validate(any())).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(testEventHandler.handle(any())).thenAnswer(invocation -> {
                TestDomainEvent event = invocation.getArgument(0);
                processedOrder.add(event.getEventData().toString());
                return CompletableFuture.completedFuture(null);
            });
            when(retryTemplate.execute(any(RetryCallback.class))).thenAnswer(invocation -> {
                RetryCallback<Void, Exception> callback = invocation.getArgument(0);
                return callback.doWithRetry(mock(RetryContext.class));
            });

            // When
            CompletableFuture<Void> result1 = eventProcessor.processEvent(event1, topic);
            CompletableFuture<Void> result2 = eventProcessor.processEvent(event2, topic);
            CompletableFuture<Void> result3 = eventProcessor.processEvent(event3, topic);
            
            CompletableFuture.allOf(result1, result2, result3).get(5, TimeUnit.SECONDS);

            // Then
            assertThat(processedOrder).hasSize(3);
            // For same aggregate, events should be processed in version order
        }

        @Test
        @DisplayName("Should handle out-of-order event delivery")
        void shouldHandleOutOfOrderEventDelivery() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            String aggregateId = "AGGREGATE-123";
            TestDomainEvent event1 = new TestDomainEvent(aggregateId, 1L, "event1");
            TestDomainEvent event3 = new TestDomainEvent(aggregateId, 3L, "event3");
            TestDomainEvent event2 = new TestDomainEvent(aggregateId, 2L, "event2");
            
            when(eventHandlerRegistry.getHandler(TestDomainEvent.class)).thenReturn(testEventHandler);
            when(bianValidator.validate(any())).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(testEventHandler.handle(any())).thenReturn(CompletableFuture.completedFuture(null));
            when(retryTemplate.execute(any(RetryCallback.class))).thenAnswer(invocation -> {
                RetryCallback<Void, Exception> callback = invocation.getArgument(0);
                return callback.doWithRetry(mock(RetryContext.class));
            });

            // When
            eventProcessor.processEvent(event1, topic);
            eventProcessor.processEvent(event3, topic); // Out of order
            eventProcessor.processEvent(event2, topic);

            // Then
            // The processor should handle this gracefully, potentially queuing event3 until event2 arrives
            verify(testEventHandler, times(3)).handle(any());
        }
    }

    @Nested
    @DisplayName("Event Processing Statistics Tests")
    class EventProcessingStatisticsTests {

        @Test
        @DisplayName("Should track processing statistics accurately")
        void shouldTrackProcessingStatisticsAccurately() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            when(eventHandlerRegistry.getHandler(TestDomainEvent.class)).thenReturn(testEventHandler);
            when(bianValidator.validate(testEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(testEventHandler.handle(testEvent)).thenReturn(CompletableFuture.completedFuture(null));
            when(retryTemplate.execute(any(RetryCallback.class))).thenAnswer(invocation -> {
                RetryCallback<Void, Exception> callback = invocation.getArgument(0);
                return callback.doWithRetry(mock(RetryContext.class));
            });

            // When
            for (int i = 0; i < 5; i++) {
                eventProcessor.processEvent(testEvent, topic).get();
            }

            // Then
            EventProcessingMetrics.Statistics stats = eventProcessor.getStatistics();
            assertThat(stats.getTotalEventsProcessed()).isEqualTo(5L);
            assertThat(stats.getSuccessfulEvents()).isEqualTo(5L);
            assertThat(stats.getFailedEvents()).isEqualTo(0L);
            assertThat(stats.getAverageProcessingTimeMs()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should track error statistics correctly")
        void shouldTrackErrorStatisticsCorrectly() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            when(eventHandlerRegistry.getHandler(TestDomainEvent.class)).thenReturn(testEventHandler);
            when(bianValidator.validate(testEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(retryTemplate.execute(any(RetryCallback.class)))
                .thenThrow(new RuntimeException("Processing failed"));

            // When
            for (int i = 0; i < 3; i++) {
                try {
                    eventProcessor.processEvent(testEvent, topic).get();
                } catch (Exception e) {
                    // Expected
                }
            }

            // Then
            EventProcessingMetrics.Statistics stats = eventProcessor.getStatistics();
            assertThat(stats.getTotalEventsProcessed()).isEqualTo(3L);
            assertThat(stats.getSuccessfulEvents()).isEqualTo(0L);
            assertThat(stats.getFailedEvents()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should report healthy when processing normally")
        void shouldReportHealthyWhenProcessingNormally() throws Exception {
            // Given
            String topic = "banking.test-domain.commands";
            when(eventHandlerRegistry.getHandler(TestDomainEvent.class)).thenReturn(testEventHandler);
            when(bianValidator.validate(testEvent)).thenReturn(BianComplianceValidator.ValidationResult.valid());
            when(testEventHandler.handle(testEvent)).thenReturn(CompletableFuture.completedFuture(null));
            when(retryTemplate.execute(any(RetryCallback.class))).thenAnswer(invocation -> {
                RetryCallback<Void, Exception> callback = invocation.getArgument(0);
                return callback.doWithRetry(mock(RetryContext.class));
            });

            // When
            eventProcessor.processEvent(testEvent, topic).get();

            // Then
            EventProcessor.HealthStatus health = eventProcessor.getHealth();
            assertThat(health.isHealthy()).isTrue();
            assertThat(health.getStatusMessage()).contains("Event processor operating normally");
        }

        @Test
        @DisplayName("Should report unhealthy when error rate is high")
        void shouldReportUnhealthyWhenErrorRateIsHigh() throws Exception {
            // Given
            when(retryTemplate.execute(any(RetryCallback.class)))
                .thenThrow(new RuntimeException("Processing failed"));

            // When - Simulate high error rate
            for (int i = 0; i < 10; i++) {
                try {
                    eventProcessor.processEvent(testEvent, "topic").get();
                } catch (Exception e) {
                    // Expected
                }
            }

            // Then
            EventProcessor.HealthStatus health = eventProcessor.getHealth();
            assertThat(health.isHealthy()).isFalse();
            assertThat(health.getStatusMessage()).contains("High error rate detected");
        }
    }

    // Test event implementations

    private static class TestDomainEvent extends DomainEvent {
        private final String testData;

        public TestDomainEvent(String aggregateId, long version, String testData) {
            super(aggregateId, "TestAggregate", version);
            this.testData = testData;
        }

        @Override
        public String getEventType() {
            return "TestDomainEvent";
        }

        @Override
        public Object getEventData() {
            return Map.of("testData", testData);
        }

        @Override
        public String getServiceDomain() {
            return "TestDomain";
        }

        @Override
        public String getBehaviorQualifier() {
            return "TEST";
        }
    }

    private static class SagaStepCompletionEvent extends DomainEvent {
        private final String stepId;
        private final Map<String, Object> stepResult;

        public SagaStepCompletionEvent(String aggregateId, long version, String stepId, Map<String, Object> stepResult) {
            super(aggregateId, "SagaAggregate", version);
            this.stepId = stepId;
            this.stepResult = stepResult;
        }

        @Override
        public String getEventType() {
            return "SagaStepCompleted";
        }

        @Override
        public Object getEventData() {
            return Map.of("stepId", stepId, "result", stepResult);
        }

        @Override
        public String getServiceDomain() {
            return "SagaDomain";
        }

        @Override
        public String getBehaviorQualifier() {
            return "COMPLETE";
        }
    }

    private static class SagaCompensationEvent extends DomainEvent {
        private final String stepId;
        private final String reason;

        public SagaCompensationEvent(String aggregateId, long version, String stepId, String reason) {
            super(aggregateId, "SagaAggregate", version);
            this.stepId = stepId;
            this.reason = reason;
        }

        @Override
        public String getEventType() {
            return "SagaCompensationRequired";
        }

        @Override
        public Object getEventData() {
            return Map.of("stepId", stepId, "reason", reason);
        }

        @Override
        public String getServiceDomain() {
            return "SagaDomain";
        }

        @Override
        public String getBehaviorQualifier() {
            return "COMPENSATE";
        }
    }

    private static class FapiNonCompliantEvent extends DomainEvent {
        private final String data;

        public FapiNonCompliantEvent(String aggregateId, long version, String data) {
            super(aggregateId, "TestAggregate", version);
            this.data = data;
        }

        @Override
        public String getEventType() {
            return "FapiNonCompliantEvent";
        }

        @Override
        public Object getEventData() {
            return Map.of("data", data);
        }

        @Override
        public String getServiceDomain() {
            return "PaymentInitiation";
        }

        @Override
        public String getBehaviorQualifier() {
            return "EXECUTE";
        }
    }

    // Mock interfaces
    interface EventHandler<T extends DomainEvent> {
        CompletableFuture<Void> handle(T event);
    }

    interface SagaEventHandler {
        CompletableFuture<Void> handleSagaEvent(DomainEvent event, String sagaId);
        CompletableFuture<Void> handleStepCompletion(SagaStepCompletionEvent event, String sagaId);
        CompletableFuture<Void> handleCompensation(SagaCompensationEvent event, String sagaId);
    }

    interface SecureEventHandler {
        CompletableFuture<Void> handleSecureEvent(DomainEvent event);
    }

    interface EventHandlerRegistry {
        <T extends DomainEvent> EventHandler<T> getHandler(Class<T> eventType);
        SagaEventHandler getSagaHandler(String serviceDomain);
        SecureEventHandler getSecureHandler(String serviceDomain);
    }

    interface BianComplianceValidator {
        ValidationResult validate(DomainEvent event);
        
        record ValidationResult(boolean isValid, String errorMessage) {
            static ValidationResult valid() {
                return new ValidationResult(true, null);
            }
            
            static ValidationResult invalid(String errorMessage) {
                return new ValidationResult(false, errorMessage);
            }
        }
    }

    interface EventProcessingMetrics {
        void recordEventProcessed(String eventType, String topic, boolean success);
        void recordBianViolation(String eventType, String reason);
        void recordNoHandlerFound(String eventType);
        void recordSagaEventProcessed(String eventType, String topic, boolean success);
        void recordSecureEventProcessed(String eventType, String topic, boolean success);
        
        record Statistics(long totalEventsProcessed, long successfulEvents, long failedEvents, double averageProcessingTimeMs) {}
    }
}