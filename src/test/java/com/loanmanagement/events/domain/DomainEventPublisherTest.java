package com.loanmanagement.events.domain;

import com.loanmanagement.events.domain.model.*;
import com.loanmanagement.events.domain.service.DomainEventPublisher;
import com.loanmanagement.events.domain.service.DomainEventDispatcher;
import com.loanmanagement.events.domain.service.EventStore;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for Domain Event Publisher
 * Comprehensive testing of domain event publishing, handling, and persistence
 */
@DisplayName("Domain Event Publisher Tests")
class DomainEventPublisherTest {

    private DomainEventPublisher domainEventPublisher;
    private DomainEventDispatcher domainEventDispatcher;
    private EventStore eventStore;

    @Mock
    private DomainEventHandler<LoanApplicationSubmittedEvent> loanApplicationHandler;
    
    @Mock
    private DomainEventHandler<LoanApprovedEvent> loanApprovedHandler;
    
    @Mock
    private DomainEventHandler<PaymentProcessedEvent> paymentProcessedHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        domainEventPublisher = new DomainEventPublisher();
        domainEventDispatcher = new DomainEventDispatcher();
        eventStore = new EventStore();
    }

    @Nested
    @DisplayName("Event Publishing Tests")
    class EventPublishingTests {

        @Test
        @DisplayName("Should publish single domain event successfully")
        void shouldPublishSingleDomainEventSuccessfully() {
            // Given
            LoanApplicationSubmittedEvent event = LoanApplicationSubmittedEvent.builder()
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

            // When
            EventPublishingResult result = domainEventPublisher.publish(event);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals(event.getEventId(), result.getEventId());
            assertNotNull(result.getPublishedAt());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should publish multiple domain events in batch")
        void shouldPublishMultipleDomainEventsInBatch() {
            // Given
            List<DomainEvent> events = List.of(
                    createLoanApplicationEvent(),
                    createLoanApprovedEvent(),
                    createPaymentProcessedEvent()
            );

            // When
            BatchEventPublishingResult batchResult = domainEventPublisher.publishBatch(events);

            // Then
            assertNotNull(batchResult);
            assertTrue(batchResult.isOverallSuccessful());
            assertEquals(3, batchResult.getTotalEvents());
            assertEquals(3, batchResult.getSuccessfulEvents());
            assertEquals(0, batchResult.getFailedEvents());
            assertNotNull(batchResult.getPublishingResults());
            assertEquals(3, batchResult.getPublishingResults().size());
        }

        @Test
        @DisplayName("Should handle event publishing failure gracefully")
        void shouldHandleEventPublishingFailureGracefully() {
            // Given
            DomainEvent invalidEvent = createInvalidEvent();

            // When
            EventPublishingResult result = domainEventPublisher.publish(invalidEvent);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertNotNull(result.getErrorMessage());
            assertNotNull(result.getFailureReason());
            assertEquals(PublishingFailureReason.VALIDATION_ERROR, result.getFailureReason());
        }

        @Test
        @DisplayName("Should publish events with correct ordering")
        void shouldPublishEventsWithCorrectOrdering() {
            // Given
            List<DomainEvent> orderedEvents = createOrderedEvents();

            // When
            OrderedEventPublishingResult result = domainEventPublisher.publishOrdered(orderedEvents);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals(orderedEvents.size(), result.getProcessedEvents());
            assertTrue(result.isOrderingMaintained());
            assertNotNull(result.getEventSequence());
        }

        @Test
        @DisplayName("Should publish events asynchronously")
        void shouldPublishEventsAsynchronously() {
            // Given
            DomainEvent event = createLoanApplicationEvent();

            // When
            CompletableFuture<EventPublishingResult> futureResult = 
                    domainEventPublisher.publishAsync(event);

            // Then
            assertNotNull(futureResult);
            assertFalse(futureResult.isDone()); // Should be processing asynchronously
            
            // Wait for completion
            EventPublishingResult result = futureResult.join();
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals(PublishingMode.ASYNC, result.getPublishingMode());
        }
    }

    @Nested
    @DisplayName("Event Dispatching Tests")
    class EventDispatchingTests {

        @Test
        @DisplayName("Should dispatch events to registered handlers")
        void shouldDispatchEventsToRegisteredHandlers() {
            // Given
            domainEventDispatcher.registerHandler(LoanApplicationSubmittedEvent.class, loanApplicationHandler);
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();

            // When
            EventDispatchingResult result = domainEventDispatcher.dispatch(event);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals(1, result.getHandlersExecuted());
            verify(loanApplicationHandler, times(1)).handle(event);
        }

        @Test
        @DisplayName("Should dispatch events to multiple handlers")
        void shouldDispatchEventsToMultipleHandlers() {
            // Given
            DomainEventHandler<LoanApplicationSubmittedEvent> secondHandler = mock(DomainEventHandler.class);
            domainEventDispatcher.registerHandler(LoanApplicationSubmittedEvent.class, loanApplicationHandler);
            domainEventDispatcher.registerHandler(LoanApplicationSubmittedEvent.class, secondHandler);
            
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();

            // When
            EventDispatchingResult result = domainEventDispatcher.dispatch(event);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals(2, result.getHandlersExecuted());
            verify(loanApplicationHandler, times(1)).handle(event);
            verify(secondHandler, times(1)).handle(event);
        }

        @Test
        @DisplayName("Should handle event handler failures")
        void shouldHandleEventHandlerFailures() {
            // Given
            domainEventDispatcher.registerHandler(LoanApplicationSubmittedEvent.class, loanApplicationHandler);
            doThrow(new RuntimeException("Handler failed")).when(loanApplicationHandler).handle(any());
            
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();

            // When
            EventDispatchingResult result = domainEventDispatcher.dispatch(event);

            // Then
            assertNotNull(result);
            assertFalse(result.isSuccessful());
            assertEquals(1, result.getFailedHandlers());
            assertNotNull(result.getHandlerFailures());
            assertFalse(result.getHandlerFailures().isEmpty());
        }

        @Test
        @DisplayName("Should support conditional event dispatching")
        void shouldSupportConditionalEventDispatching() {
            // Given
            EventDispatchingCondition condition = event -> 
                    event instanceof LoanApplicationSubmittedEvent loanEvent &&
                    loanEvent.getLoanAmount().getAmount().compareTo(new BigDecimal("10000")) > 0;
            
            domainEventDispatcher.registerConditionalHandler(
                    LoanApplicationSubmittedEvent.class, loanApplicationHandler, condition);
            
            LoanApplicationSubmittedEvent highValueEvent = createHighValueLoanApplicationEvent();
            LoanApplicationSubmittedEvent lowValueEvent = createLowValueLoanApplicationEvent();

            // When
            EventDispatchingResult highValueResult = domainEventDispatcher.dispatch(highValueEvent);
            EventDispatchingResult lowValueResult = domainEventDispatcher.dispatch(lowValueEvent);

            // Then
            assertTrue(highValueResult.isSuccessful());
            assertEquals(1, highValueResult.getHandlersExecuted());
            
            assertTrue(lowValueResult.isSuccessful());
            assertEquals(0, lowValueResult.getHandlersExecuted()); // Condition not met
        }
    }

    @Nested
    @DisplayName("Event Store Tests")
    class EventStoreTests {

        @Test
        @DisplayName("Should store domain events successfully")
        void shouldStoreDomainEventsSuccessfully() {
            // Given
            DomainEvent event = createLoanApplicationEvent();

            // When
            EventStoreResult storeResult = eventStore.store(event);

            // Then
            assertNotNull(storeResult);
            assertTrue(storeResult.isSuccessful());
            assertNotNull(storeResult.getStoredEventId());
            assertNotNull(storeResult.getStoredAt());
            assertEquals(event.getEventId(), storeResult.getOriginalEventId());
        }

        @Test
        @DisplayName("Should retrieve events by aggregate ID")
        void shouldRetrieveEventsByAggregateId() {
            // Given
            String aggregateId = LoanId.generate().toString();
            List<DomainEvent> events = createEventsForAggregate(aggregateId);
            events.forEach(eventStore::store);

            // When
            List<DomainEvent> retrievedEvents = eventStore.getEventsByAggregateId(aggregateId);

            // Then
            assertNotNull(retrievedEvents);
            assertEquals(events.size(), retrievedEvents.size());
            assertTrue(retrievedEvents.stream().allMatch(event -> 
                    event.getAggregateId().equals(aggregateId)));
        }

        @Test
        @DisplayName("Should retrieve events by type")
        void shouldRetrieveEventsByType() {
            // Given
            eventStore.store(createLoanApplicationEvent());
            eventStore.store(createLoanApprovedEvent());
            eventStore.store(createPaymentProcessedEvent());

            // When
            List<DomainEvent> loanEvents = eventStore.getEventsByType(LoanApplicationSubmittedEvent.class);

            // Then
            assertNotNull(loanEvents);
            assertFalse(loanEvents.isEmpty());
            assertTrue(loanEvents.stream().allMatch(event -> 
                    event instanceof LoanApplicationSubmittedEvent));
        }

        @Test
        @DisplayName("Should retrieve events within date range")
        void shouldRetrieveEventsWithinDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusHours(1);
            LocalDateTime endDate = LocalDateTime.now().plusHours(1);
            
            DomainEvent event = createLoanApplicationEvent();
            eventStore.store(event);

            // When
            List<DomainEvent> events = eventStore.getEventsByDateRange(startDate, endDate);

            // Then
            assertNotNull(events);
            assertFalse(events.isEmpty());
            assertTrue(events.stream().allMatch(e -> 
                    e.getOccurredOn().isAfter(startDate) && e.getOccurredOn().isBefore(endDate)));
        }

        @Test
        @DisplayName("Should support event stream pagination")
        void shouldSupportEventStreamPagination() {
            // Given
            for (int i = 0; i < 15; i++) {
                eventStore.store(createLoanApplicationEvent());
            }

            // When
            EventStreamPage firstPage = eventStore.getEventStream(0, 10);
            EventStreamPage secondPage = eventStore.getEventStream(10, 10);

            // Then
            assertNotNull(firstPage);
            assertEquals(10, firstPage.getEvents().size());
            assertFalse(firstPage.isLastPage());
            
            assertNotNull(secondPage);
            assertEquals(5, secondPage.getEvents().size());
            assertTrue(secondPage.isLastPage());
        }
    }

    @Nested
    @DisplayName("Event Replay Tests")
    class EventReplayTests {

        @Test
        @DisplayName("Should replay events for aggregate reconstruction")
        void shouldReplayEventsForAggregateReconstruction() {
            // Given
            String aggregateId = LoanId.generate().toString();
            List<DomainEvent> events = createEventsForAggregate(aggregateId);
            events.forEach(eventStore::store);

            // When
            EventReplayResult replayResult = eventStore.replayEvents(aggregateId);

            // Then
            assertNotNull(replayResult);
            assertTrue(replayResult.isSuccessful());
            assertEquals(events.size(), replayResult.getReplayedEventCount());
            assertNotNull(replayResult.getReconstructedState());
        }

        @Test
        @DisplayName("Should replay events from specific version")
        void shouldReplayEventsFromSpecificVersion() {
            // Given
            String aggregateId = LoanId.generate().toString();
            List<DomainEvent> events = createEventsForAggregate(aggregateId);
            events.forEach(eventStore::store);
            
            long fromVersion = 2L;

            // When
            EventReplayResult replayResult = eventStore.replayEventsFromVersion(aggregateId, fromVersion);

            // Then
            assertNotNull(replayResult);
            assertTrue(replayResult.isSuccessful());
            assertTrue(replayResult.getReplayedEventCount() < events.size());
            assertTrue(replayResult.getReplayedEvents().stream()
                    .allMatch(event -> event.getVersion() >= fromVersion));
        }

        @Test
        @DisplayName("Should handle event replay failures")
        void shouldHandleEventReplayFailures() {
            // Given
            String nonExistentAggregateId = "NON_EXISTENT";

            // When
            EventReplayResult replayResult = eventStore.replayEvents(nonExistentAggregateId);

            // Then
            assertNotNull(replayResult);
            assertFalse(replayResult.isSuccessful());
            assertEquals(0, replayResult.getReplayedEventCount());
            assertNotNull(replayResult.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Event Saga Tests")
    class EventSagaTests {

        @Test
        @DisplayName("Should initiate saga from domain event")
        void shouldInitiateSagaFromDomainEvent() {
            // Given
            LoanApplicationSubmittedEvent event = createLoanApplicationEvent();
            LoanProcessingSaga saga = new LoanProcessingSaga();

            // When
            SagaInitiationResult result = saga.initiateFrom(event);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertNotNull(result.getSagaId());
            assertEquals(SagaStatus.STARTED, result.getSagaStatus());
            assertNotNull(result.getInitiatingEvent());
        }

        @Test
        @DisplayName("Should handle saga step execution")
        void shouldHandleSagaStepExecution() {
            // Given
            LoanProcessingSaga saga = new LoanProcessingSaga();
            saga.initiateFrom(createLoanApplicationEvent());
            
            LoanApprovedEvent approvalEvent = createLoanApprovedEvent();

            // When
            SagaStepResult stepResult = saga.handleEvent(approvalEvent);

            // Then
            assertNotNull(stepResult);
            assertTrue(stepResult.isSuccessful());
            assertNotNull(stepResult.getExecutedStep());
            assertNotNull(stepResult.getNextExpectedEvents());
        }

        @Test
        @DisplayName("Should handle saga completion")
        void shouldHandleSagaCompletion() {
            // Given
            LoanProcessingSaga saga = new LoanProcessingSaga();
            saga.initiateFrom(createLoanApplicationEvent());
            saga.handleEvent(createLoanApprovedEvent());
            
            PaymentProcessedEvent finalEvent = createPaymentProcessedEvent();

            // When
            SagaCompletionResult result = saga.complete(finalEvent);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals(SagaStatus.COMPLETED, result.getFinalStatus());
            assertNotNull(result.getCompletionTime());
            assertNotNull(result.getSagaOutcome());
        }

        @Test
        @DisplayName("Should handle saga compensation")
        void shouldHandleSagaCompensation() {
            // Given
            LoanProcessingSaga saga = new LoanProcessingSaga();
            saga.initiateFrom(createLoanApplicationEvent());
            
            LoanRejectedEvent rejectionEvent = createLoanRejectedEvent();

            // When
            SagaCompensationResult result = saga.compensate(rejectionEvent);

            // Then
            assertNotNull(result);
            assertTrue(result.isSuccessful());
            assertEquals(SagaStatus.COMPENSATED, result.getFinalStatus());
            assertNotNull(result.getCompensationActions());
            assertFalse(result.getCompensationActions().isEmpty());
        }
    }

    // Helper methods to create test events
    
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

    private DomainEvent createInvalidEvent() {
        return new InvalidDomainEvent(); // Event with validation errors
    }

    private List<DomainEvent> createOrderedEvents() {
        return List.of(
                createLoanApplicationEvent(),
                createLoanApprovedEvent(),
                createPaymentProcessedEvent()
        );
    }

    private LoanApplicationSubmittedEvent createHighValueLoanApplicationEvent() {
        return LoanApplicationSubmittedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(LoanId.generate().toString())
                .loanId(LoanId.generate())
                .customerId("CUST-123")
                .loanAmount(Money.of("USD", new BigDecimal("100000.00"))) // High value
                .loanType("PERSONAL")
                .applicationDate(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .version(1L)
                .build();
    }

    private LoanApplicationSubmittedEvent createLowValueLoanApplicationEvent() {
        return LoanApplicationSubmittedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(LoanId.generate().toString())
                .loanId(LoanId.generate())
                .customerId("CUST-123")
                .loanAmount(Money.of("USD", new BigDecimal("5000.00"))) // Low value
                .loanType("PERSONAL")
                .applicationDate(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .version(1L)
                .build();
    }

    private List<DomainEvent> createEventsForAggregate(String aggregateId) {
        return List.of(
                LoanApplicationSubmittedEvent.builder()
                        .eventId(EventId.generate())
                        .aggregateId(aggregateId)
                        .loanId(LoanId.of(aggregateId))
                        .version(1L)
                        .occurredOn(LocalDateTime.now())
                        .build(),
                LoanApprovedEvent.builder()
                        .eventId(EventId.generate())
                        .aggregateId(aggregateId)
                        .loanId(LoanId.of(aggregateId))
                        .version(2L)
                        .occurredOn(LocalDateTime.now())
                        .build()
        );
    }

    private LoanRejectedEvent createLoanRejectedEvent() {
        return LoanRejectedEvent.builder()
                .eventId(EventId.generate())
                .aggregateId(LoanId.generate().toString())
                .loanId(LoanId.generate())
                .rejectionReason("Insufficient credit score")
                .rejectionDate(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .version(2L)
                .build();
    }

    // Inner class for invalid event
    private static class InvalidDomainEvent implements DomainEvent {
        @Override
        public EventId getEventId() { return null; } // Invalid - null ID
        
        @Override
        public String getAggregateId() { return ""; } // Invalid - empty ID
        
        @Override
        public LocalDateTime getOccurredOn() { return null; } // Invalid - null timestamp
        
        @Override
        public Long getVersion() { return -1L; } // Invalid - negative version
    }
}