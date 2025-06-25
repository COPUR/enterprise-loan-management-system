package com.bank.loanmanagement.sharedkernel.infrastructure.eventstore;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for JpaEventStore
 * Tests event persistence, concurrency control, and query operations
 * Ensures 85%+ test coverage for event sourcing infrastructure
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaEventStore Tests")
class JpaEventStoreTest {

    @Mock
    private JpaEventStore.EventStoreRepository eventStoreRepository;
    
    @Mock
    private JpaEventStore.SnapshotRepository snapshotRepository;
    
    @Mock
    private ObjectMapper objectMapper;

    private JpaEventStore eventStore;
    private TestDomainEvent testEvent;
    private String aggregateId;
    
    @BeforeEach
    void setUp() {
        eventStore = new JpaEventStore(eventStoreRepository, snapshotRepository, objectMapper);
        aggregateId = "TEST-AGGREGATE-123";
        testEvent = new TestDomainEvent(aggregateId, 1L, "Test event data");
    }

    @Nested
    @DisplayName("Save Events Tests")
    class SaveEventsTests {

        @Test
        @DisplayName("Should save events successfully with correct expected version")
        void shouldSaveEventsSuccessfullyWithCorrectExpectedVersion() throws Exception {
            // Given
            List<DomainEvent> events = List.of(testEvent);
            long expectedVersion = 0L;
            
            when(eventStoreRepository.findMaxVersionByAggregateId(aggregateId))
                .thenReturn(Optional.of(expectedVersion));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"data\":\"test\"}");

            // When
            eventStore.saveEvents(aggregateId, events, expectedVersion);

            // Then
            verify(eventStoreRepository).saveAll(anyList());
            verify(eventStoreRepository).findMaxVersionByAggregateId(aggregateId);
        }

        @Test
        @DisplayName("Should save events when no previous version exists")
        void shouldSaveEventsWhenNoPreviousVersionExists() throws Exception {
            // Given
            List<DomainEvent> events = List.of(testEvent);
            long expectedVersion = 0L;
            
            when(eventStoreRepository.findMaxVersionByAggregateId(aggregateId))
                .thenReturn(Optional.empty());
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"data\":\"test\"}");

            // When
            eventStore.saveEvents(aggregateId, events, expectedVersion);

            // Then
            verify(eventStoreRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("Should throw OptimisticLockingFailureException when version mismatch")
        void shouldThrowOptimisticLockingFailureExceptionWhenVersionMismatch() {
            // Given
            List<DomainEvent> events = List.of(testEvent);
            long expectedVersion = 1L;
            long currentVersion = 2L;
            
            when(eventStoreRepository.findMaxVersionByAggregateId(aggregateId))
                .thenReturn(Optional.of(currentVersion));

            // When & Then
            assertThatThrownBy(() -> eventStore.saveEvents(aggregateId, events, expectedVersion))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageContaining("Expected version 1 but current version is 2");
        }

        @Test
        @DisplayName("Should handle serialization failure gracefully")
        void shouldHandleSerializationFailureGracefully() throws Exception {
            // Given
            List<DomainEvent> events = List.of(testEvent);
            long expectedVersion = 0L;
            
            when(eventStoreRepository.findMaxVersionByAggregateId(aggregateId))
                .thenReturn(Optional.of(expectedVersion));
            when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

            // When & Then
            assertThatThrownBy(() -> eventStore.saveEvents(aggregateId, events, expectedVersion))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to serialize event data");
        }
    }

    @Nested
    @DisplayName("Retrieve Events Tests")
    class RetrieveEventsTests {

        @Test
        @DisplayName("Should retrieve all events for aggregate ordered by version")
        void shouldRetrieveAllEventsForAggregateOrderedByVersion() throws Exception {
            // Given
            List<JpaEventStore.EventStoreEntry> entries = createTestEntries();
            when(eventStoreRepository.findByAggregateIdOrderByVersionAsc(aggregateId))
                .thenReturn(entries);
            when(objectMapper.readValue(anyString(), eq(TestDomainEvent.class)))
                .thenReturn(testEvent);

            // When
            List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId);

            // Then
            assertThat(events).hasSize(2);
            verify(eventStoreRepository).findByAggregateIdOrderByVersionAsc(aggregateId);
        }

        @Test
        @DisplayName("Should retrieve events from specific version")
        void shouldRetrieveEventsFromSpecificVersion() throws Exception {
            // Given
            long fromVersion = 1L;
            List<JpaEventStore.EventStoreEntry> entries = createTestEntries();
            when(eventStoreRepository.findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, fromVersion))
                .thenReturn(entries);
            when(objectMapper.readValue(anyString(), eq(TestDomainEvent.class)))
                .thenReturn(testEvent);

            // When
            List<DomainEvent> events = eventStore.getEventsForAggregateFromVersion(aggregateId, fromVersion);

            // Then
            assertThat(events).hasSize(2);
            verify(eventStoreRepository).findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, fromVersion);
        }

        @Test
        @DisplayName("Should retrieve events by type")
        void shouldRetrieveEventsByType() throws Exception {
            // Given
            String eventType = "TestDomainEvent";
            List<JpaEventStore.EventStoreEntry> entries = createTestEntries();
            when(eventStoreRepository.findByEventTypeOrderByOccurredOnAsc(eventType))
                .thenReturn(entries);
            when(objectMapper.readValue(anyString(), eq(TestDomainEvent.class)))
                .thenReturn(testEvent);

            // When
            List<DomainEvent> events = eventStore.getEventsByType(eventType);

            // Then
            assertThat(events).hasSize(2);
            verify(eventStoreRepository).findByEventTypeOrderByOccurredOnAsc(eventType);
        }

        @Test
        @DisplayName("Should retrieve events by service domain")
        void shouldRetrieveEventsByServiceDomain() throws Exception {
            // Given
            String serviceDomain = "ConsumerLoan";
            List<JpaEventStore.EventStoreEntry> entries = createTestEntries();
            when(eventStoreRepository.findByServiceDomainOrderByOccurredOnAsc(serviceDomain))
                .thenReturn(entries);
            when(objectMapper.readValue(anyString(), eq(TestDomainEvent.class)))
                .thenReturn(testEvent);

            // When
            List<DomainEvent> events = eventStore.getEventsByServiceDomain(serviceDomain);

            // Then
            assertThat(events).hasSize(2);
            verify(eventStoreRepository).findByServiceDomainOrderByOccurredOnAsc(serviceDomain);
        }

        @Test
        @DisplayName("Should retrieve events after timestamp")
        void shouldRetrieveEventsAfterTimestamp() throws Exception {
            // Given
            OffsetDateTime timestamp = OffsetDateTime.now().minusHours(1);
            List<JpaEventStore.EventStoreEntry> entries = createTestEntries();
            when(eventStoreRepository.findByOccurredOnAfterOrderByOccurredOnAsc(timestamp))
                .thenReturn(entries);
            when(objectMapper.readValue(anyString(), eq(TestDomainEvent.class)))
                .thenReturn(testEvent);

            // When
            List<DomainEvent> events = eventStore.getEventsAfter(timestamp);

            // Then
            assertThat(events).hasSize(2);
            verify(eventStoreRepository).findByOccurredOnAfterOrderByOccurredOnAsc(timestamp);
        }

        @Test
        @DisplayName("Should handle deserialization failure gracefully")
        void shouldHandleDeserializationFailureGracefully() throws Exception {
            // Given
            List<JpaEventStore.EventStoreEntry> entries = createTestEntries();
            when(eventStoreRepository.findByAggregateIdOrderByVersionAsc(aggregateId))
                .thenReturn(entries);
            when(objectMapper.readValue(anyString(), any(Class.class)))
                .thenThrow(new RuntimeException("Deserialization failed"));

            // When & Then
            assertThatThrownBy(() -> eventStore.getEventsForAggregate(aggregateId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize event");
        }
    }

    @Nested
    @DisplayName("Version Management Tests")
    class VersionManagementTests {

        @Test
        @DisplayName("Should return latest aggregate version")
        void shouldReturnLatestAggregateVersion() {
            // Given
            long expectedVersion = 5L;
            when(eventStoreRepository.findMaxVersionByAggregateId(aggregateId))
                .thenReturn(Optional.of(expectedVersion));

            // When
            Optional<Long> version = eventStore.getLatestAggregateVersion(aggregateId);

            // Then
            assertThat(version).isPresent();
            assertThat(version.get()).isEqualTo(expectedVersion);
        }

        @Test
        @DisplayName("Should return empty when no version exists")
        void shouldReturnEmptyWhenNoVersionExists() {
            // Given
            when(eventStoreRepository.findMaxVersionByAggregateId(aggregateId))
                .thenReturn(Optional.empty());

            // When
            Optional<Long> version = eventStore.getLatestAggregateVersion(aggregateId);

            // Then
            assertThat(version).isEmpty();
        }
    }

    @Nested
    @DisplayName("Snapshot Management Tests")
    class SnapshotManagementTests {

        @Test
        @DisplayName("Should save snapshot successfully")
        void shouldSaveSnapshotSuccessfully() throws Exception {
            // Given
            Object snapshot = new TestAggregate("test-data");
            long version = 10L;
            
            when(objectMapper.writeValueAsString(snapshot)).thenReturn("{\"data\":\"test-data\"}");

            // When
            eventStore.saveSnapshot(aggregateId, snapshot, version);

            // Then
            verify(snapshotRepository).save(any(JpaEventStore.SnapshotEntry.class));
        }

        @Test
        @DisplayName("Should handle snapshot serialization failure")
        void shouldHandleSnapshotSerializationFailure() throws Exception {
            // Given
            Object snapshot = new TestAggregate("test-data");
            long version = 10L;
            
            when(objectMapper.writeValueAsString(snapshot))
                .thenThrow(new RuntimeException("Serialization failed"));

            // When & Then
            assertThatThrownBy(() -> eventStore.saveSnapshot(aggregateId, snapshot, version))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to serialize snapshot");
        }

        @Test
        @DisplayName("Should retrieve latest snapshot")
        void shouldRetrieveLatestSnapshot() throws Exception {
            // Given
            JpaEventStore.SnapshotEntry snapshotEntry = createTestSnapshotEntry();
            when(snapshotRepository.findTopByAggregateIdOrderByVersionDesc(aggregateId))
                .thenReturn(Optional.of(snapshotEntry));
            when(objectMapper.readValue(anyString(), eq(Object.class)))
                .thenReturn(new TestAggregate("test-data"));

            // When
            Optional<EventStore.AggregateSnapshot> snapshot = eventStore.getLatestSnapshot(aggregateId);

            // Then
            assertThat(snapshot).isPresent();
            assertThat(snapshot.get().aggregateId()).isEqualTo(aggregateId);
            assertThat(snapshot.get().version()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should return empty when no snapshot exists")
        void shouldReturnEmptyWhenNoSnapshotExists() {
            // Given
            when(snapshotRepository.findTopByAggregateIdOrderByVersionDesc(aggregateId))
                .thenReturn(Optional.empty());

            // When
            Optional<EventStore.AggregateSnapshot> snapshot = eventStore.getLatestSnapshot(aggregateId);

            // Then
            assertThat(snapshot).isEmpty();
        }

        @Test
        @DisplayName("Should handle snapshot deserialization failure")
        void shouldHandleSnapshotDeserializationFailure() throws Exception {
            // Given
            JpaEventStore.SnapshotEntry snapshotEntry = createTestSnapshotEntry();
            when(snapshotRepository.findTopByAggregateIdOrderByVersionDesc(aggregateId))
                .thenReturn(Optional.of(snapshotEntry));
            when(objectMapper.readValue(anyString(), eq(Object.class)))
                .thenThrow(new RuntimeException("Deserialization failed"));

            // When & Then
            assertThatThrownBy(() -> eventStore.getLatestSnapshot(aggregateId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize snapshot");
        }
    }

    @Nested
    @DisplayName("Health and Statistics Tests")
    class HealthAndStatisticsTests {

        @Test
        @DisplayName("Should return healthy when repository is accessible")
        void shouldReturnHealthyWhenRepositoryIsAccessible() {
            // Given
            when(eventStoreRepository.count()).thenReturn(100L);

            // When
            boolean isHealthy = eventStore.isHealthy();

            // Then
            assertThat(isHealthy).isTrue();
        }

        @Test
        @DisplayName("Should return unhealthy when repository throws exception")
        void shouldReturnUnhealthyWhenRepositoryThrowsException() {
            // Given
            when(eventStoreRepository.count()).thenThrow(new RuntimeException("Database error"));

            // When
            boolean isHealthy = eventStore.isHealthy();

            // Then
            assertThat(isHealthy).isFalse();
        }

        @Test
        @DisplayName("Should return accurate statistics")
        void shouldReturnAccurateStatistics() {
            // Given
            long totalEvents = 1000L;
            long totalAggregates = 100L;
            long totalSnapshots = 50L;
            OffsetDateTime lastEventTime = OffsetDateTime.now();
            
            when(eventStoreRepository.count()).thenReturn(totalEvents);
            when(eventStoreRepository.countDistinctAggregates()).thenReturn(totalAggregates);
            when(snapshotRepository.count()).thenReturn(totalSnapshots);
            when(eventStoreRepository.findLastEventTime()).thenReturn(Optional.of(lastEventTime));

            // When
            EventStore.EventStoreStatistics statistics = eventStore.getStatistics();

            // Then
            assertThat(statistics.totalEvents()).isEqualTo(totalEvents);
            assertThat(statistics.totalAggregates()).isEqualTo(totalAggregates);
            assertThat(statistics.totalSnapshots()).isEqualTo(totalSnapshots);
            assertThat(statistics.averageEventsPerAggregate()).isEqualTo(10.0);
            assertThat(statistics.lastEventTime()).isEqualTo(lastEventTime);
        }

        @Test
        @DisplayName("Should handle zero aggregates in statistics calculation")
        void shouldHandleZeroAggregatesInStatisticsCalculation() {
            // Given
            when(eventStoreRepository.count()).thenReturn(0L);
            when(eventStoreRepository.countDistinctAggregates()).thenReturn(0L);
            when(snapshotRepository.count()).thenReturn(0L);
            when(eventStoreRepository.findLastEventTime()).thenReturn(Optional.empty());

            // When
            EventStore.EventStoreStatistics statistics = eventStore.getStatistics();

            // Then
            assertThat(statistics.totalEvents()).isEqualTo(0L);
            assertThat(statistics.totalAggregates()).isEqualTo(0L);
            assertThat(statistics.averageEventsPerAggregate()).isEqualTo(0.0);
        }
    }

    // Helper methods and test classes

    private List<JpaEventStore.EventStoreEntry> createTestEntries() {
        return List.of(
            JpaEventStore.EventStoreEntry.builder()
                .eventId("EVENT-1")
                .aggregateId(aggregateId)
                .aggregateType("TestAggregate")
                .eventType("TestDomainEvent")
                .eventData("{\"data\":\"test1\"}")
                .version(1L)
                .occurredOn(OffsetDateTime.now().minusMinutes(2))
                .serviceDomain("TestDomain")
                .behaviorQualifier("TEST")
                .build(),
            JpaEventStore.EventStoreEntry.builder()
                .eventId("EVENT-2")
                .aggregateId(aggregateId)
                .aggregateType("TestAggregate")
                .eventType("TestDomainEvent")
                .eventData("{\"data\":\"test2\"}")
                .version(2L)
                .occurredOn(OffsetDateTime.now().minusMinutes(1))
                .serviceDomain("TestDomain")
                .behaviorQualifier("TEST")
                .build()
        );
    }

    private JpaEventStore.SnapshotEntry createTestSnapshotEntry() {
        return JpaEventStore.SnapshotEntry.builder()
                .id(1L)
                .aggregateId(aggregateId)
                .aggregateType("TestAggregate")
                .snapshotData("{\"data\":\"test-snapshot\"}")
                .version(10L)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    // Test domain event implementation
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

    private record TestAggregate(String data) {}
}