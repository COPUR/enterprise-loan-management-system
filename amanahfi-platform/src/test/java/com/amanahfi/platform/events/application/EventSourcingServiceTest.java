package com.amanahfi.platform.events.application;

import com.amanahfi.platform.events.domain.*;
import com.amanahfi.platform.events.port.in.*;
import com.amanahfi.platform.events.port.out.EventStoreRepository;
import com.amanahfi.platform.events.port.out.EventStreamPublisher;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventSourcingService")
class EventSourcingServiceTest {
    
    @Mock
    private EventStoreRepository eventStoreRepository;
    
    @Mock
    private EventStreamPublisher eventStreamPublisher;
    
    @InjectMocks
    private EventSourcingService eventSourcingService;
    
    private TestDomainEvent testEvent;
    private EventStore eventStore;
    private final String aggregateId = "test-aggregate-123";
    private final String aggregateType = "TestAggregate";
    
    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent(aggregateId, aggregateType);
        eventStore = EventStore.createForAggregate(aggregateId, aggregateType);
    }
    
    @Test
    @DisplayName("Should store events successfully for new aggregate")
    void shouldStoreEventsForNewAggregate() {
        // Given
        List<DomainEvent> events = List.of(testEvent);
        StoreEventsCommand command = StoreEventsCommand.builder()
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .events(events)
            .expectedVersion(0L)
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.empty());
        when(eventStoreRepository.save(any(EventStore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        eventSourcingService.storeEvents(command);
        
        // Then
        ArgumentCaptor<EventStore> storeCaptor = ArgumentCaptor.forClass(EventStore.class);
        verify(eventStoreRepository).save(storeCaptor.capture());
        
        EventStore savedStore = storeCaptor.getValue();
        assertThat(savedStore.getAggregateId()).isEqualTo(aggregateId);
        assertThat(savedStore.getAggregateType()).isEqualTo(aggregateType);
        assertThat(savedStore.getStoredEvents()).hasSize(1);
        assertThat(savedStore.getVersion()).isEqualTo(1L);
        
        verify(eventStreamPublisher).publish(eq("amanahfi.general"), eq(events));
    }
    
    @Test
    @DisplayName("Should store events successfully for existing aggregate")
    void shouldStoreEventsForExistingAggregate() {
        // Given
        List<DomainEvent> events = List.of(testEvent);
        StoreEventsCommand command = StoreEventsCommand.builder()
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .events(events)
            .expectedVersion(0L)
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(eventStore));
        when(eventStoreRepository.save(any(EventStore.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        eventSourcingService.storeEvents(command);
        
        // Then
        ArgumentCaptor<EventStore> storeCaptor = ArgumentCaptor.forClass(EventStore.class);
        verify(eventStoreRepository).save(storeCaptor.capture());
        
        EventStore savedStore = storeCaptor.getValue();
        assertThat(savedStore.getStoredEvents()).hasSize(1);
        assertThat(savedStore.getVersion()).isEqualTo(1L);
        
        verify(eventStreamPublisher).publish(eq("amanahfi.general"), eq(events));
    }
    
    @Test
    @DisplayName("Should throw OptimisticLockException when version mismatch")
    void shouldThrowOptimisticLockExceptionOnVersionMismatch() {
        // Given
        EventStore existingStore = EventStore.createForAggregate(aggregateId, aggregateType);
        existingStore.appendEvents(List.of(testEvent), 0L); // Version becomes 1
        
        List<DomainEvent> events = List.of(testEvent);
        StoreEventsCommand command = StoreEventsCommand.builder()
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .events(events)
            .expectedVersion(0L) // Expecting version 0 but actual is 1
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(existingStore));
        
        // When & Then
        assertThatThrownBy(() -> eventSourcingService.storeEvents(command))
            .isInstanceOf(OptimisticLockException.class)
            .hasMessageContaining("Version mismatch");
        
        verify(eventStoreRepository, never()).save(any());
        verify(eventStreamPublisher, never()).publish(anyString(), any(List.class));
    }
    
    @Test
    @DisplayName("Should get events from specific version")
    void shouldGetEventsFromSpecificVersion() {
        // Given
        EventStore storeWithEvents = EventStore.createForAggregate(aggregateId, aggregateType);
        storeWithEvents.appendEvents(List.of(testEvent), 0L);
        storeWithEvents.appendEvents(List.of(testEvent), 1L);
        
        GetEventsQuery query = GetEventsQuery.builder()
            .aggregateId(aggregateId)
            .fromVersion(1L)
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(storeWithEvents));
        
        // When
        List<StoredEvent> result = eventSourcingService.getEvents(query);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVersion()).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("Should return empty list when no events found")
    void shouldReturnEmptyListWhenNoEventsFound() {
        // Given
        GetEventsQuery query = GetEventsQuery.builder()
            .aggregateId("non-existent-aggregate")
            .fromVersion(0L)
            .build();
        
        when(eventStoreRepository.findByAggregateId("non-existent-aggregate")).thenReturn(Optional.empty());
        
        // When
        List<StoredEvent> result = eventSourcingService.getEvents(query);
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("Should get all events for aggregate")
    void shouldGetAllEventsForAggregate() {
        // Given
        EventStore storeWithEvents = EventStore.createForAggregate(aggregateId, aggregateType);
        storeWithEvents.appendEvents(List.of(testEvent), 0L);
        storeWithEvents.appendEvents(List.of(testEvent), 1L);
        
        GetAllEventsQuery query = GetAllEventsQuery.builder()
            .aggregateId(aggregateId)
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(storeWithEvents));
        
        // When
        List<StoredEvent> result = eventSourcingService.getAllEvents(query);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(1L);
        assertThat(result.get(1).getVersion()).isEqualTo(2L);
    }
    
    @Test
    @DisplayName("Should get snapshot for aggregate")
    void shouldGetSnapshotForAggregate() {
        // Given
        EventStore storeWithEvents = EventStore.createForAggregate(aggregateId, aggregateType);
        storeWithEvents.appendEvents(List.of(testEvent), 0L);
        
        GetSnapshotQuery query = GetSnapshotQuery.builder()
            .aggregateId(aggregateId)
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(storeWithEvents));
        
        // When
        Optional<EventStoreSnapshot> result = eventSourcingService.getSnapshot(query);
        
        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAggregateId()).isEqualTo(aggregateId);
        assertThat(result.get().getVersion()).isEqualTo(1L);
        assertThat(result.get().getEventCount()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should replay events from specific version")
    void shouldReplayEventsFromSpecificVersion() {
        // Given
        EventStore storeWithEvents = EventStore.createForAggregate(aggregateId, aggregateType);
        storeWithEvents.appendEvents(List.of(testEvent), 0L);
        storeWithEvents.appendEvents(List.of(testEvent), 1L);
        
        ReplayEventsCommand command = ReplayEventsCommand.builder()
            .aggregateId(aggregateId)
            .fromVersion(1L)
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(storeWithEvents));
        
        // When
        eventSourcingService.replayEvents(command);
        
        // Then
        verify(eventStreamPublisher).publish(eq("amanahfi.general"), any(List.class));
    }
    
    @Test
    @DisplayName("Should check if aggregate exists")
    void shouldCheckIfAggregateExists() {
        // Given
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(eventStore));
        when(eventStoreRepository.findByAggregateId("non-existent")).thenReturn(Optional.empty());
        
        // When & Then
        assertThat(eventSourcingService.aggregateExists(aggregateId)).isTrue();
        assertThat(eventSourcingService.aggregateExists("non-existent")).isFalse();
    }
    
    @Test
    @DisplayName("Should get current version for aggregate")
    void shouldGetCurrentVersionForAggregate() {
        // Given
        EventStore storeWithEvents = EventStore.createForAggregate(aggregateId, aggregateType);
        storeWithEvents.appendEvents(List.of(testEvent), 0L);
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(storeWithEvents));
        when(eventStoreRepository.findByAggregateId("non-existent")).thenReturn(Optional.empty());
        
        // When & Then
        assertThat(eventSourcingService.getCurrentVersion(aggregateId)).isEqualTo(1L);
        assertThat(eventSourcingService.getCurrentVersion("non-existent")).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("Should delete event store when confirmed")
    void shouldDeleteEventStoreWhenConfirmed() {
        // Given
        DeleteEventStoreCommand command = DeleteEventStoreCommand.builder()
            .aggregateId(aggregateId)
            .confirmed(true)
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(eventStore));
        
        // When
        eventSourcingService.deleteEventStore(command);
        
        // Then
        verify(eventStoreRepository).delete(eventStore);
    }
    
    @Test
    @DisplayName("Should not delete event store when not confirmed")
    void shouldNotDeleteEventStoreWhenNotConfirmed() {
        // Given
        DeleteEventStoreCommand command = DeleteEventStoreCommand.builder()
            .aggregateId(aggregateId)
            .confirmed(false)
            .build();
        
        // When & Then
        assertThatThrownBy(() -> eventSourcingService.deleteEventStore(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Deletion must be confirmed");
        
        verify(eventStoreRepository, never()).delete(any());
    }
    
    @Test
    @DisplayName("Should get events by category")
    void shouldGetEventsByCategory() {
        // Given
        TestIslamicFinanceEvent islamicEvent = new TestIslamicFinanceEvent(aggregateId, aggregateType);
        TestPaymentEvent paymentEvent = new TestPaymentEvent(aggregateId, aggregateType);
        
        EventStore storeWithEvents = EventStore.createForAggregate(aggregateId, aggregateType);
        storeWithEvents.appendEvents(List.of(islamicEvent, paymentEvent), 0L);
        
        GetEventsByCategoryQuery query = GetEventsByCategoryQuery.builder()
            .aggregateId(aggregateId)
            .eventCategory(EventCategory.ISLAMIC_FINANCE)
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(storeWithEvents));
        
        // When
        List<StoredEvent> result = eventSourcingService.getEventsByCategory(query);
        
        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEventType()).isEqualTo("TestIslamicFinanceEvent");
    }
    
    @Test
    @DisplayName("Should get events by time range")
    void shouldGetEventsByTimeRange() {
        // Given
        Instant now = Instant.now();
        Instant oneHourAgo = now.minusSeconds(3600);
        Instant twoHoursAgo = now.minusSeconds(7200);
        
        EventStore storeWithEvents = EventStore.createForAggregate(aggregateId, aggregateType);
        storeWithEvents.appendEvents(List.of(testEvent), 0L);
        
        GetEventsByTimeRangeQuery query = GetEventsByTimeRangeQuery.builder()
            .aggregateId(aggregateId)
            .fromTime(oneHourAgo)
            .toTime(now)
            .build();
        
        when(eventStoreRepository.findByAggregateId(aggregateId)).thenReturn(Optional.of(storeWithEvents));
        
        // When
        List<StoredEvent> result = eventSourcingService.getEventsByTimeRange(query);
        
        // Then
        assertThat(result).hasSize(1);
    }
    
    // Test helper classes
    
    private static class TestDomainEvent extends DomainEvent {
        public TestDomainEvent(String aggregateId, String aggregateType) {
            super(aggregateId, aggregateType, EventMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .causationId(UUID.randomUUID().toString())
                .userId("test-user")
                .build());
        }
    }
    
    private static class TestIslamicFinanceEvent extends DomainEvent {
        public TestIslamicFinanceEvent(String aggregateId, String aggregateType) {
            super(aggregateId, aggregateType, EventMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .causationId(UUID.randomUUID().toString())
                .userId("test-user")
                .build());
        }
    }
    
    private static class TestPaymentEvent extends DomainEvent {
        public TestPaymentEvent(String aggregateId, String aggregateType) {
            super(aggregateId, aggregateType, EventMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .causationId(UUID.randomUUID().toString())
                .userId("test-user")
                .build());
        }
    }
}