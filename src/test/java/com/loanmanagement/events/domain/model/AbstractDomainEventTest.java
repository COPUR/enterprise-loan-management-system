package com.loanmanagement.events.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

/**
 * TDD Tests for AbstractDomainEvent
 * Following Red-Green-Refactor cycle
 */
class AbstractDomainEventTest {

    private TestDomainEvent event;
    private LocalDateTime testTime;

    @BeforeEach
    void setUp() {
        testTime = LocalDateTime.now();
        event = new TestDomainEvent("test-aggregate-123", testTime, 1L);
    }

    @Test
    void shouldGenerateEventIdWhenCreatedWithDefaultConstructor() {
        // Given
        TestDomainEvent defaultEvent = new TestDomainEvent("aggregate-123");
        
        // When & Then
        assertNotNull(defaultEvent.getEventId());
        assertNotNull(defaultEvent.getOccurredOn());
        assertEquals(1L, defaultEvent.getVersion());
        assertEquals("test-aggregate-123", defaultEvent.getAggregateId());
    }

    @Test
    void shouldCreateEventWithProvidedValues() {
        // Given
        EventId eventId = EventId.generate();
        LocalDateTime occurredOn = LocalDateTime.now().minusHours(1);
        Long version = 5L;
        
        // When
        TestDomainEvent customEvent = new TestDomainEvent(eventId, occurredOn, version, "custom-aggregate");
        
        // Then
        assertEquals(eventId, customEvent.getEventId());
        assertEquals(occurredOn, customEvent.getOccurredOn());
        assertEquals(version, customEvent.getVersion());
        assertEquals("custom-aggregate", customEvent.getAggregateId());
    }

    @Test
    void shouldReturnCorrectEventType() {
        // When & Then
        assertEquals("TestDomainEvent", event.getEventType());
    }

    @Test
    void shouldDetermineIfEventIsFirst() {
        // Given
        TestDomainEvent firstEvent = new TestDomainEvent("aggregate", testTime, 1L);
        TestDomainEvent laterEvent = new TestDomainEvent("aggregate", testTime, 2L);
        
        // When & Then
        assertTrue(firstEvent.isFirstEvent());
        assertFalse(laterEvent.isFirstEvent());
    }

    @Test
    void shouldCompareEventTiming() {
        // Given
        LocalDateTime earlier = testTime.minusMinutes(10);
        LocalDateTime later = testTime.plusMinutes(10);
        TestDomainEvent earlierEvent = new TestDomainEvent("aggregate", earlier, 1L);
        TestDomainEvent laterEvent = new TestDomainEvent("aggregate", later, 2L);
        
        // When & Then
        assertTrue(earlierEvent.occurredBefore(laterEvent));
        assertFalse(laterEvent.occurredBefore(earlierEvent));
        assertTrue(laterEvent.occurredAfter(earlierEvent));
        assertFalse(earlierEvent.occurredAfter(laterEvent));
    }

    @Test
    void shouldDetermineIfEventsBelongToSameAggregate() {
        // Given
        TestDomainEvent sameAggregateEvent = new TestDomainEvent("test-aggregate-123", testTime, 2L);
        TestDomainEvent differentAggregateEvent = new TestDomainEvent("different-aggregate", testTime, 1L);
        
        // When & Then
        assertTrue(event.belongsToSameAggregate(sameAggregateEvent));
        assertFalse(event.belongsToSameAggregate(differentAggregateEvent));
    }

    @Test
    void shouldHandleEqualityCorrectly() {
        // Given
        EventId sameEventId = event.getEventId();
        TestDomainEvent sameEvent = new TestDomainEvent(sameEventId, testTime, 1L, "test-aggregate-123");
        TestDomainEvent differentEvent = new TestDomainEvent("different-aggregate");
        
        // When & Then
        assertEquals(event, sameEvent);
        assertNotEquals(event, differentEvent);
        assertEquals(event.hashCode(), sameEvent.hashCode());
    }

    @Test
    void shouldThrowExceptionForNullEventId() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new TestDomainEvent(null, testTime, 1L, "aggregate"));
    }

    @Test
    void shouldThrowExceptionForNullOccurredOn() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new TestDomainEvent(EventId.generate(), null, 1L, "aggregate"));
    }

    @Test
    void shouldThrowExceptionForNullVersion() {
        // When & Then
        assertThrows(NullPointerException.class, () -> 
            new TestDomainEvent(EventId.generate(), testTime, null, "aggregate"));
    }

    // Test implementation of AbstractDomainEvent
    private static class TestDomainEvent extends AbstractDomainEvent {
        private final String aggregateId;

        public TestDomainEvent(String aggregateId) {
            super();
            this.aggregateId = aggregateId;
        }

        public TestDomainEvent(String aggregateId, LocalDateTime occurredOn, Long version) {
            super(version);
            this.aggregateId = aggregateId;
        }

        public TestDomainEvent(EventId eventId, LocalDateTime occurredOn, Long version, String aggregateId) {
            super(eventId, occurredOn, version);
            this.aggregateId = aggregateId;
        }

        @Override
        public String getAggregateId() {
            return aggregateId;
        }
    }
}