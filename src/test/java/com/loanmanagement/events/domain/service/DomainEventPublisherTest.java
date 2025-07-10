package com.loanmanagement.events.domain.service;

import com.loanmanagement.events.domain.model.AbstractDomainEvent;
import com.loanmanagement.events.domain.model.DomainEvent;
import com.loanmanagement.events.domain.model.EventId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * TDD Tests for DomainEventPublisher
 * Testing missing classes that cause compilation errors
 */
@ExtendWith(MockitoExtension.class)
class DomainEventPublisherTest {

    @Mock
    private EventPublishingMetrics mockMetrics;
    
    @Mock
    private EventValidationService mockValidationService;
    
    private DomainEventPublisher publisher;
    private TestDomainEvent testEvent;

    @BeforeEach
    void setUp() {
        publisher = new DomainEventPublisher(mockMetrics, mockValidationService);
        testEvent = new TestDomainEvent("test-aggregate-123");
    }

    @Test
    void shouldPublishSingleEventSuccessfully() {
        // Given
        when(mockValidationService.validate(testEvent)).thenReturn(true);
        
        // When
        EventPublishingResult result = publisher.publish(testEvent);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        verify(mockMetrics).recordEventPublished(testEvent);
        verify(mockValidationService).validate(testEvent);
    }

    @Test
    void shouldFailToPublishInvalidEvent() {
        // Given
        when(mockValidationService.validate(testEvent)).thenReturn(false);
        
        // When
        EventPublishingResult result = publisher.publish(testEvent);
        
        // Then
        assertNotNull(result);
        assertFalse(result.isSuccessful());
        verify(mockMetrics).recordEventPublishingError(eq(testEvent), any());
    }

    @Test
    void shouldPublishBatchEventsSuccessfully() {
        // Given
        List<DomainEvent> events = List.of(
            new TestDomainEvent("aggregate-1"),
            new TestDomainEvent("aggregate-2")
        );
        when(mockValidationService.validate(any(DomainEvent.class))).thenReturn(true);
        
        // When
        BatchEventPublishingResult result = publisher.publishBatch(events);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(2, result.getSuccessfulCount());
        assertEquals(0, result.getFailedCount());
    }

    @Test
    void shouldPublishOrderedEventsSuccessfully() {
        // Given
        List<DomainEvent> events = List.of(
            new TestDomainEvent("aggregate-1"),
            new TestDomainEvent("aggregate-1")
        );
        when(mockValidationService.validate(any(DomainEvent.class))).thenReturn(true);
        
        // When
        OrderedEventPublishingResult result = publisher.publishOrdered(events);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
        assertEquals(2, result.getProcessedCount());
    }

    @Test
    void shouldPublishBatchAsynchronously() {
        // Given
        List<DomainEvent> events = List.of(new TestDomainEvent("aggregate-1"));
        when(mockValidationService.validate(any(DomainEvent.class))).thenReturn(true);
        
        // When
        CompletableFuture<BatchEventPublishingResult> future = publisher.publishBatchAsync(events);
        
        // Then
        assertNotNull(future);
        assertFalse(future.isDone()); // Should be async
    }

    @Test
    void shouldRetryPublishingWithRetryPolicy() {
        // Given
        RetryPolicy retryPolicy = new RetryPolicy(3, 1000);
        when(mockValidationService.validate(testEvent)).thenReturn(true);
        
        // When
        EventPublishingResult result = publisher.retryPublishing(testEvent, retryPolicy);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isSuccessful());
    }

    @Test
    void shouldValidateEventBeforePublishing() {
        // Given
        when(mockValidationService.validate(testEvent)).thenReturn(false);
        
        // When
        boolean isValid = publisher.validateEvent(testEvent);
        
        // Then
        assertFalse(isValid);
        verify(mockValidationService).validate(testEvent);
    }

    // Test implementation of DomainEvent
    private static class TestDomainEvent extends AbstractDomainEvent {
        private final String aggregateId;

        public TestDomainEvent(String aggregateId) {
            super();
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }
}