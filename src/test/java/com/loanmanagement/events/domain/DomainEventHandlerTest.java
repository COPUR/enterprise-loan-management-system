package com.loanmanagement.events.domain;

import com.loanmanagement.events.domain.model.*;
import com.loanmanagement.events.domain.service.DomainEventHandler;
import com.loanmanagement.loan.domain.model.LoanId;
import com.loanmanagement.shared.domain.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Domain Event Handlers
 * Comprehensive testing of event handling, processing, and error recovery
 */
@DisplayName("Domain Event Handler Tests")
class DomainEventHandlerTest {

    private LoanApplicationEventHandler loanApplicationHandler;
    private LoanApprovalEventHandler loanApprovalHandler;
    private PaymentEventHandler paymentHandler;
    private NotificationEventHandler notificationHandler;

    @Mock
    private NotificationService notificationService;
    
    @Mock
    private CreditCheckService creditCheckService;
    
    @Mock
    private PaymentProcessingService paymentProcessingService;
    
    @Mock
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loanApplicationHandler = new LoanApplicationEventHandler(creditCheckService, auditService);
        loanApprovalHandler = new LoanApprovalEventHandler(notificationService, auditService);
        paymentHandler = new PaymentEventHandler(paymentProcessingService, auditService);
        notificationHandler = new NotificationEventHandler(notificationService);
    }

    @Nested
    @DisplayName("Event Handler Registration Tests")
    class EventHandlerRegistrationTests {

        @Test
        @DisplayName("Should register event handler successfully")
        void shouldRegisterEventHandlerSuccessfully() {
            // Given
            EventHandlerRegistry registry = new EventHandlerRegistry();

            // When
            HandlerRegistrationResult result = registry.registerHandler(
                LoanApplicationSubmittedEvent.class, loanApplicationHandler);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertNotNull(result.getHandlerId());
            assertEquals(LoanApplicationSubmittedEvent.class, result.getEventType());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should register multiple handlers for same event type")
        void shouldRegisterMultipleHandlersForSameEventType() {
            // Given
            EventHandlerRegistry registry = new EventHandlerRegistry();
            DomainEventHandler<LoanApplicationSubmittedEvent> secondHandler = 
                new SecondLoanApplicationHandler();

            // When
            HandlerRegistrationResult result1 = registry.registerHandler(
                LoanApplicationSubmittedEvent.class, loanApplicationHandler);
            HandlerRegistrationResult result2 = registry.registerHandler(
                LoanApplicationSubmittedEvent.class, secondHandler);

            // Then
            assertTrue(result1.isSuccessful());
            assertTrue(result2.isSuccessful());
            assertNotEquals(result1.getHandlerId(), result2.getHandlerId());
            
            List<DomainEventHandler<LoanApplicationSubmittedEvent>> handlers = 
                registry.getHandlers(LoanApplicationSubmittedEvent.class);
            assertEquals(2, handlers.size());
        }

        @Test
        @DisplayName("Should fail to register null handler")
        void shouldFailToRegisterNullHandler() {
            // Given
            EventHandlerRegistry registry = new EventHandlerRegistry();

            // When
            HandlerRegistrationResult result = registry.registerHandler(
                LoanApplicationSubmittedEvent.class, null);

            // Then
            assertFalse(result.isSuccessful());
            assertNotNull(result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("Handler cannot be null"));
        }

        @Test
        @DisplayName("Should unregister event handler")
        void shouldUnregisterEventHandler() {
            // Given
            EventHandlerRegistry registry = new EventHandlerRegistry();
            HandlerRegistrationResult registration = registry.registerHandler(
                LoanApplicationSubmittedEvent.class, loanApplicationHandler);

            // When
            HandlerUnregistrationResult result = registry.unregisterHandler(registration.getHandlerId());

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(registration.getHandlerId(), result.getHandlerId());
            
            List<DomainEventHandler<LoanApplicationSubmittedEvent>> handlers = 
                registry.getHandlers(LoanApplicationSubmittedEvent.class);
            assertTrue(handlers.isEmpty());
        }
    }

    @Nested
    @DisplayName("Event Processing Tests")
    class EventProcessingTests {

        @Test
        @DisplayName("Should process loan application event successfully")
        void shouldProcessLoanApplicationEventSuccessfully() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            when(creditCheckService.performCreditCheck(any())).thenReturn(
                CreditCheckResult.builder().approved(true).creditScore(750).build());

            // When
            EventHandlingResult result = loanApplicationHandler.handle(event);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals(event.getEventId(), result.getEventId());
            assertNotNull(result.getProcessedAt());
            assertNull(result.getErrorMessage());
            
            verify(creditCheckService, times(1)).performCreditCheck(any());
            verify(auditService, times(1)).auditEvent(eq(event), any());
        }

        @Test
        @DisplayName("Should process loan approval event successfully")
        void shouldProcessLoanApprovalEventSuccessfully() {
            // Given
            LoanApprovedEvent event = createLoanApprovedEvent();
            when(notificationService.sendNotification(any())).thenReturn(
                NotificationResult.builder().successful(true).build());

            // When
            EventHandlingResult result = loanApprovalHandler.handle(event);

            // Then
            assertTrue(result.isSuccessful());
            verify(notificationService, times(1)).sendNotification(any());
            verify(auditService, times(1)).auditEvent(eq(event), any());
        }

        @Test
        @DisplayName("Should process payment event successfully")
        void shouldProcessPaymentEventSuccessfully() {
            // Given
            PaymentProcessedEvent event = createPaymentProcessedEvent();
            when(paymentProcessingService.updatePaymentRecords(any())).thenReturn(
                PaymentUpdateResult.builder().successful(true).build());

            // When
            EventHandlingResult result = paymentHandler.handle(event);

            // Then
            assertTrue(result.isSuccessful());
            verify(paymentProcessingService, times(1)).updatePaymentRecords(any());
            verify(auditService, times(1)).auditEvent(eq(event), any());
        }

        @Test
        @DisplayName("Should handle event processing errors gracefully")
        void shouldHandleEventProcessingErrorsGracefully() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            when(creditCheckService.performCreditCheck(any())).thenThrow(
                new RuntimeException("Credit check service unavailable"));

            // When
            EventHandlingResult result = loanApplicationHandler.handle(event);

            // Then
            assertFalse(result.isSuccessful());
            assertNotNull(result.getErrorMessage());
            assertTrue(result.getErrorMessage().contains("Credit check service unavailable"));
            assertEquals(EventHandlingStatus.FAILED, result.getStatus());
        }

        @Test
        @DisplayName("Should retry failed event processing")
        void shouldRetryFailedEventProcessing() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            when(creditCheckService.performCreditCheck(any()))
                .thenThrow(new RuntimeException("Temporary failure"))
                .thenReturn(CreditCheckResult.builder().approved(true).creditScore(720).build());

            RetryableEventHandler retryableHandler = new RetryableEventHandler(
                loanApplicationHandler, RetryPolicy.builder().maxRetries(2).build());

            // When
            EventHandlingResult result = retryableHandler.handleWithRetry(event);

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(2, result.getRetryCount());
            verify(creditCheckService, times(2)).performCreditCheck(any());
        }
    }

    @Nested
    @DisplayName("Asynchronous Event Processing Tests")
    class AsynchronousEventProcessingTests {

        @Test
        @DisplayName("Should process events asynchronously")
        void shouldProcessEventsAsynchronously() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            AsyncEventHandler asyncHandler = new AsyncEventHandler(loanApplicationHandler);
            
            when(creditCheckService.performCreditCheck(any())).thenReturn(
                CreditCheckResult.builder().approved(true).creditScore(780).build());

            // When
            CompletableFuture<EventHandlingResult> futureResult = asyncHandler.handleAsync(event);

            // Then
            assertNotNull(futureResult);
            assertFalse(futureResult.isDone()); // Should be processing asynchronously
            
            // Wait for completion
            EventHandlingResult result = futureResult.join();
            assertTrue(result.isSuccessful());
            assertEquals(ProcessingMode.ASYNC, result.getProcessingMode());
        }

        @Test
        @DisplayName("Should handle async processing timeouts")
        void shouldHandleAsyncProcessingTimeouts() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            SlowEventHandler slowHandler = new SlowEventHandler(5000); // 5 second delay
            AsyncEventHandler asyncHandler = new AsyncEventHandler(slowHandler, 1000); // 1 second timeout

            // When
            CompletableFuture<EventHandlingResult> futureResult = asyncHandler.handleAsync(event);

            // Then
            EventHandlingResult result = futureResult.join();
            assertFalse(result.isSuccessful());
            assertEquals(EventHandlingStatus.TIMEOUT, result.getStatus());
            assertTrue(result.getErrorMessage().contains("timeout"));
        }

        @Test
        @DisplayName("Should process multiple events concurrently")
        void shouldProcessMultipleEventsConcurrently() {
            // Given
            List<LoanApplicationSubmittedEvent> events = List.of(
                createLoanApplicationEvent(),
                createLoanApplicationEvent(),
                createLoanApplicationEvent()
            );
            
            ConcurrentEventProcessor processor = new ConcurrentEventProcessor(loanApplicationHandler);
            when(creditCheckService.performCreditCheck(any())).thenReturn(
                CreditCheckResult.builder().approved(true).creditScore(750).build());

            // When
            long startTime = System.currentTimeMillis();
            List<EventHandlingResult> results = processor.processEvents(events);
            long endTime = System.currentTimeMillis();

            // Then
            assertEquals(3, results.size());
            assertTrue(results.stream().allMatch(EventHandlingResult::isSuccessful));
            assertTrue(endTime - startTime < 2000); // Should complete quickly due to concurrency
        }
    }

    @Nested
    @DisplayName("Event Handler Chain Tests")
    class EventHandlerChainTests {

        @Test
        @DisplayName("Should execute handler chain in order")
        void shouldExecuteHandlerChainInOrder() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            EventHandlerChain chain = EventHandlerChain.builder()
                .addHandler(loanApplicationHandler)
                .addHandler(notificationHandler)
                .addHandler(auditHandler())
                .build();

            when(creditCheckService.performCreditCheck(any())).thenReturn(
                CreditCheckResult.builder().approved(true).creditScore(760).build());
            when(notificationService.sendNotification(any())).thenReturn(
                NotificationResult.builder().successful(true).build());

            // When
            ChainExecutionResult result = chain.execute(event);

            // Then
            assertTrue(result.isSuccessful());
            assertEquals(3, result.getExecutedHandlers());
            assertEquals(0, result.getFailedHandlers());
            
            List<EventHandlingResult> handlerResults = result.getHandlerResults();
            assertEquals(3, handlerResults.size());
            assertTrue(handlerResults.stream().allMatch(EventHandlingResult::isSuccessful));
        }

        @Test
        @DisplayName("Should stop chain execution on handler failure")
        void shouldStopChainExecutionOnHandlerFailure() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            EventHandlerChain chain = EventHandlerChain.builder()
                .addHandler(loanApplicationHandler)
                .addHandler(failingHandler())
                .addHandler(notificationHandler)
                .stopOnFailure(true)
                .build();

            when(creditCheckService.performCreditCheck(any())).thenReturn(
                CreditCheckResult.builder().approved(true).creditScore(760).build());

            // When
            ChainExecutionResult result = chain.execute(event);

            // Then
            assertFalse(result.isSuccessful());
            assertEquals(2, result.getExecutedHandlers()); // First two handlers
            assertEquals(1, result.getFailedHandlers());
            
            // Third handler should not be executed
            verify(notificationService, never()).sendNotification(any());
        }

        @Test
        @DisplayName("Should continue chain execution despite handler failures")
        void shouldContinueChainExecutionDespiteHandlerFailures() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            EventHandlerChain chain = EventHandlerChain.builder()
                .addHandler(loanApplicationHandler)
                .addHandler(failingHandler())
                .addHandler(notificationHandler)
                .stopOnFailure(false) // Continue on failure
                .build();

            when(creditCheckService.performCreditCheck(any())).thenReturn(
                CreditCheckResult.builder().approved(true).creditScore(760).build());
            when(notificationService.sendNotification(any())).thenReturn(
                NotificationResult.builder().successful(true).build());

            // When
            ChainExecutionResult result = chain.execute(event);

            // Then
            assertFalse(result.isSuccessful()); // Overall failed due to one failure
            assertEquals(3, result.getExecutedHandlers()); // All handlers executed
            assertEquals(1, result.getFailedHandlers());
            
            // Third handler should be executed
            verify(notificationService, times(1)).sendNotification(any());
        }
    }

    @Nested
    @DisplayName("Event Handler Metrics Tests")
    class EventHandlerMetricsTests {

        @Test
        @DisplayName("Should collect handler performance metrics")
        void shouldCollectHandlerPerformanceMetrics() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            MetricsCollectingEventHandler metricsHandler = new MetricsCollectingEventHandler(loanApplicationHandler);
            
            when(creditCheckService.performCreditCheck(any())).thenReturn(
                CreditCheckResult.builder().approved(true).creditScore(740).build());

            // When
            EventHandlingResult result = metricsHandler.handle(event);

            // Then
            assertTrue(result.isSuccessful());
            assertNotNull(result.getMetrics());
            
            HandlerMetrics metrics = result.getMetrics();
            assertTrue(metrics.getExecutionTimeMs() > 0);
            assertNotNull(metrics.getStartTime());
            assertNotNull(metrics.getEndTime());
            assertEquals(1, metrics.getExecutionCount());
        }

        @Test
        @DisplayName("Should track handler success and failure rates")
        void shouldTrackHandlerSuccessAndFailureRates() {
            // Given
            MetricsCollectingEventHandler metricsHandler = new MetricsCollectingEventHandler(loanApplicationHandler);
            
            // Simulate successful and failed processing
            when(creditCheckService.performCreditCheck(any()))
                .thenReturn(CreditCheckResult.builder().approved(true).creditScore(740).build())
                .thenThrow(new RuntimeException("Service error"))
                .thenReturn(CreditCheckResult.builder().approved(true).creditScore(760).build());

            // When
            metricsHandler.handle(createLoanApplicationEvent()); // Success
            metricsHandler.handle(createLoanApplicationEvent()); // Failure
            metricsHandler.handle(createLoanApplicationEvent()); // Success

            // Then
            HandlerStatistics stats = metricsHandler.getStatistics();
            assertEquals(3, stats.getTotalExecutions());
            assertEquals(2, stats.getSuccessfulExecutions());
            assertEquals(1, stats.getFailedExecutions());
            assertEquals(0.67, stats.getSuccessRate(), 0.01);
        }
    }

    // Helper methods and test implementations

    private LoanApplicationSubmittedEvent createLoanApplicationEvent() {
        return LoanApplicationSubmittedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(LoanId.generate().toString())
                .loanId(LoanId.generate())
                .customerId("CUST-123")
                .loanAmount(Money.of("USD", new BigDecimal("50000.00")))
                .loanType("PERSONAL")
                .applicationDate(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .version(1L)
                .build();
    }

    private LoanApprovedEvent createLoanApprovedEvent() {
        return LoanApprovedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(LoanId.generate().toString())
                .loanId(LoanId.generate())
                .approvedAmount(Money.of("USD", new BigDecimal("45000.00")))
                .interestRate(new BigDecimal("5.5"))
                .approvalDate(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .version(2L)
                .build();
    }

    private PaymentProcessedEvent createPaymentProcessedEvent() {
        return PaymentProcessedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId("PAYMENT-123")
                .paymentId("PAY-123")
                .loanId(LoanId.generate())
                .paymentAmount(Money.of("USD", new BigDecimal("1000.00")))
                .paymentDate(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .version(1L)
                .build();
    }

    private DomainEventHandler<LoanApplicationSubmittedEvent> failingHandler() {
        return event -> {
            throw new RuntimeException("Handler intentionally failed");
        };
    }

    private DomainEventHandler<LoanApplicationSubmittedEvent> auditHandler() {
        return event -> {
            auditService.auditEvent(event, "PROCESSED");
            return EventHandlingResult.builder()
                    .eventId(event.getEventId())
                    .successful(true)
                    .processedAt(LocalDateTime.now())
                    .build();
        };
    }

    // Test implementations of handlers and services

    private static class SecondLoanApplicationHandler implements DomainEventHandler<LoanApplicationSubmittedEvent> {
        @Override
        public EventHandlingResult handle(LoanApplicationSubmittedEvent event) {
            return EventHandlingResult.builder()
                    .eventId(event.getEventId())
                    .successful(true)
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }

    private static class SlowEventHandler implements DomainEventHandler<LoanApplicationSubmittedEvent> {
        private final long delayMs;

        public SlowEventHandler(long delayMs) {
            this.delayMs = delayMs;
        }

        @Override
        public EventHandlingResult handle(LoanApplicationSubmittedEvent event) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return EventHandlingResult.builder()
                    .eventId(event.getEventId())
                    .successful(true)
                    .processedAt(LocalDateTime.now())
                    .build();
        }
    }
}