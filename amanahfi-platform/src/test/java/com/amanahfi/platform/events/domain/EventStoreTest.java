package com.amanahfi.platform.events.domain;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EventStore")
class EventStoreTest {
    
    private EventStore eventStore;
    private TestDomainEvent testEvent;
    private final String aggregateId = "test-aggregate-123";
    private final String aggregateType = "TestAggregate";
    
    @BeforeEach
    void setUp() {
        eventStore = EventStore.createForAggregate(aggregateId, aggregateType);
        testEvent = new TestDomainEvent(aggregateId, aggregateType);
    }
    
    @Test
    @DisplayName("Should create event store for aggregate")
    void shouldCreateEventStoreForAggregate() {
        // When
        EventStore newStore = EventStore.createForAggregate(aggregateId, aggregateType);
        
        // Then
        assertThat(newStore.getId()).isNotNull();
        assertThat(newStore.getAggregateId()).isEqualTo(aggregateId);
        assertThat(newStore.getAggregateType()).isEqualTo(aggregateType);
        assertThat(newStore.getVersion()).isEqualTo(0L);
        assertThat(newStore.getStoredEvents()).isEmpty();
        assertThat(newStore.getCreatedAt()).isNotNull();
        assertThat(newStore.getLastUpdated()).isNotNull();
    }
    
    @Test
    @DisplayName("Should append events with correct versioning")
    void shouldAppendEventsWithCorrectVersioning() {
        // Given
        List<DomainEvent> events = List.of(testEvent);
        
        // When
        EventStore updatedStore = eventStore.appendEvents(events, 0L);
        
        // Then
        assertThat(updatedStore.getVersion()).isEqualTo(1L);
        assertThat(updatedStore.getStoredEvents()).hasSize(1);
        assertThat(updatedStore.getStoredEvents().get(0).getVersion()).isEqualTo(1L);
        assertThat(updatedStore.getStoredEvents().get(0).getAggregateId()).isEqualTo(aggregateId);
        assertThat(updatedStore.getStoredEvents().get(0).getEventType()).isEqualTo("TestDomainEvent");
    }
    
    @Test
    @DisplayName("Should append multiple events incrementally")
    void shouldAppendMultipleEventsIncrementally() {
        // Given
        List<DomainEvent> firstBatch = List.of(testEvent);
        List<DomainEvent> secondBatch = List.of(new TestDomainEvent(aggregateId, aggregateType));
        
        // When
        EventStore firstUpdate = eventStore.appendEvents(firstBatch, 0L);
        EventStore secondUpdate = firstUpdate.appendEvents(secondBatch, 1L);
        
        // Then
        assertThat(secondUpdate.getVersion()).isEqualTo(2L);
        assertThat(secondUpdate.getStoredEvents()).hasSize(2);
        assertThat(secondUpdate.getStoredEvents().get(0).getVersion()).isEqualTo(1L);
        assertThat(secondUpdate.getStoredEvents().get(1).getVersion()).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("Should throw OptimisticLockException on version mismatch")
    void shouldThrowOptimisticLockExceptionOnVersionMismatch() {
        // Given
        eventStore.appendEvents(List.of(testEvent), 0L); // Version becomes 1
        List<DomainEvent> events = List.of(new TestDomainEvent(aggregateId, aggregateType));
        
        // When & Then
        assertThatThrownBy(() -> eventStore.appendEvents(events, 0L)) // Expected 0 but actual is 1
            .isInstanceOf(OptimisticLockException.class)
            .hasMessageContaining("Version mismatch");
    }
    
    @Test
    @DisplayName("Should get events from specific version")
    void shouldGetEventsFromSpecificVersion() {
        // Given
        eventStore.appendEvents(List.of(testEvent), 0L);
        eventStore.appendEvents(List.of(new TestDomainEvent(aggregateId, aggregateType)), 1L);
        eventStore.appendEvents(List.of(new TestDomainEvent(aggregateId, aggregateType)), 2L);
        
        // When
        List<StoredEvent> eventsFromVersion2 = eventStore.getEventsFromVersion(2L);
        
        // Then
        assertThat(eventsFromVersion2).hasSize(1);
        assertThat(eventsFromVersion2.get(0).getVersion()).isEqualTo(3L);
    }
    
    @Test
    @DisplayName("Should get all events")
    void shouldGetAllEvents() {
        // Given
        eventStore.appendEvents(List.of(testEvent), 0L);
        eventStore.appendEvents(List.of(new TestDomainEvent(aggregateId, aggregateType)), 1L);
        
        // When
        List<StoredEvent> allEvents = eventStore.getAllEvents();
        
        // Then
        assertThat(allEvents).hasSize(2);
        assertThat(allEvents.get(0).getVersion()).isEqualTo(1L);
        assertThat(allEvents.get(1).getVersion()).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("Should create event store snapshot")
    void shouldCreateEventStoreSnapshot() {
        // Given
        eventStore.appendEvents(List.of(testEvent), 0L);
        eventStore.appendEvents(List.of(new TestDomainEvent(aggregateId, aggregateType)), 1L);
        
        // When
        EventStoreSnapshot snapshot = eventStore.getSnapshot();
        
        // Then
        assertThat(snapshot.getEventStoreId()).isEqualTo(eventStore.getId());
        assertThat(snapshot.getAggregateId()).isEqualTo(aggregateId);
        assertThat(snapshot.getAggregateType()).isEqualTo(aggregateType);
        assertThat(snapshot.getVersion()).isEqualTo(2L);
        assertThat(snapshot.getEventCount()).isEqualTo(2);
        assertThat(snapshot.getCreatedAt()).isNotNull();
        assertThat(snapshot.getLastUpdated()).isNotNull();
    }
    
    @Test
    @DisplayName("Should update last updated timestamp when appending events")
    void shouldUpdateLastUpdatedTimestampWhenAppendingEvents() {
        // Given
        Instant originalTimestamp = eventStore.getLastUpdated();
        
        // When
        EventStore updatedStore = eventStore.appendEvents(List.of(testEvent), 0L);
        
        // Then
        assertThat(updatedStore.getLastUpdated()).isAfter(originalTimestamp);
    }
    
    @Test
    @DisplayName("Should maintain event ordering")
    void shouldMaintainEventOrdering() {
        // Given
        TestDomainEvent event1 = new TestDomainEvent(aggregateId, aggregateType);
        TestDomainEvent event2 = new TestDomainEvent(aggregateId, aggregateType);
        TestDomainEvent event3 = new TestDomainEvent(aggregateId, aggregateType);
        
        // When
        eventStore.appendEvents(List.of(event1), 0L);
        eventStore.appendEvents(List.of(event2), 1L);
        eventStore.appendEvents(List.of(event3), 2L);
        
        List<StoredEvent> allEvents = eventStore.getAllEvents();
        
        // Then
        assertThat(allEvents).hasSize(3);
        assertThat(allEvents.get(0).getVersion()).isEqualTo(1L);
        assertThat(allEvents.get(1).getVersion()).isEqualTo(2L);
        assertThat(allEvents.get(2).getVersion()).isEqualTo(3L);
        
        // Verify chronological ordering
        assertThat(allEvents.get(0).getTimestamp()).isBeforeOrEqualTo(allEvents.get(1).getTimestamp());
        assertThat(allEvents.get(1).getTimestamp()).isBeforeOrEqualTo(allEvents.get(2).getTimestamp());
    }
    
    @Test
    @DisplayName("Should handle empty event list gracefully")
    void shouldHandleEmptyEventListGracefully() {
        // Given
        List<DomainEvent> emptyEvents = List.of();
        
        // When
        EventStore updatedStore = eventStore.appendEvents(emptyEvents, 0L);
        
        // Then
        assertThat(updatedStore.getVersion()).isEqualTo(0L);
        assertThat(updatedStore.getStoredEvents()).isEmpty();
    }
    
    @Test
    @DisplayName("Should validate aggregate consistency")
    void shouldValidateAggregateConsistency() {
        // Given
        TestDomainEvent eventWithDifferentAggregateId = new TestDomainEvent("different-id", aggregateType);
        List<DomainEvent> inconsistentEvents = List.of(eventWithDifferentAggregateId);
        
        // When & Then
        assertThatThrownBy(() -> eventStore.appendEvents(inconsistentEvents, 0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Event aggregate ID");
    }
    
    // Test helper class
    
    private static class TestDomainEvent extends DomainEvent {
        public TestDomainEvent(String aggregateId, String aggregateType) {
            super(aggregateId, aggregateType, EventMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .causationId(UUID.randomUUID().toString())
                .userId("test-user")
                .build());
        }
    }
}