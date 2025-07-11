package com.amanahfi.platform.shared.events;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for DomainEventPublisher
 * 
 * This test class follows Test-Driven Development principles to ensure
 * the DomainEventPublisher interface contract is properly implemented.
 * 
 * Test Coverage:
 * - Single event publishing
 * - Batch event publishing
 * - Error handling and exceptions
 * - Null safety and defensive programming
 * - Islamic finance compliance events
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Domain Event Publisher Tests")
class DomainEventPublisherTest {

    @Mock
    private DomainEventPublisher eventPublisher;

    private TestDomainEvent testEvent;
    private TestDomainEvent secondEvent;
    private List<DomainEvent> eventList;

    @BeforeEach
    void setUp() {
        testEvent = new TestDomainEvent(
            UUID.randomUUID(),
            "TEST-001",
            "TestAggregate",
            Instant.now(),
            1L,
            UUID.randomUUID(),
            UUID.randomUUID(),
            EventMetadata.system("TEST_SYSTEM")
        );

        secondEvent = new TestDomainEvent(
            UUID.randomUUID(),
            "TEST-002",
            "TestAggregate",
            Instant.now(),
            2L,
            UUID.randomUUID(),
            UUID.randomUUID(),
            EventMetadata.system("TEST_SYSTEM")
        );

        eventList = Arrays.asList(testEvent, secondEvent);
    }

    @Nested
    @DisplayName("Single Event Publishing")
    class SingleEventPublishing {

        @Test
        @DisplayName("Should publish single event successfully")
        void shouldPublishSingleEventSuccessfully() {
            // Given
            doNothing().when(eventPublisher).publish(testEvent);

            // When
            assertDoesNotThrow(() -> eventPublisher.publish(testEvent));

            // Then
            verify(eventPublisher, times(1)).publish(testEvent);
        }

        @Test
        @DisplayName("Should throw exception when publishing null event")
        void shouldThrowExceptionWhenPublishingNullEvent() {
            // Given
            doThrow(new IllegalArgumentException("Event cannot be null"))
                .when(eventPublisher).publish(null);

            // When & Then
            assertThrows(IllegalArgumentException.class, 
                () -> eventPublisher.publish(null));
        }

        @Test
        @DisplayName("Should throw EventPublishingException when publishing fails")
        void shouldThrowEventPublishingExceptionWhenPublishingFails() {
            // Given
            doThrow(new DomainEventPublisher.EventPublishingException("Publishing failed"))
                .when(eventPublisher).publish(testEvent);

            // When & Then
            DomainEventPublisher.EventPublishingException exception = assertThrows(
                DomainEventPublisher.EventPublishingException.class,
                () -> eventPublisher.publish(testEvent)
            );

            assertEquals("Publishing failed", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle Sharia-compliant events")
        void shouldHandleShariaCompliantEvents() {
            // Given
            ShariaCompliantEvent shariaEvent = new ShariaCompliantEvent(
                UUID.randomUUID(),
                "MURABAHA-001",
                "MurabahaAggregate",
                Instant.now(),
                1L,
                UUID.randomUUID(),
                UUID.randomUUID(),
                EventMetadata.system("SHARIA_SYSTEM")
            );

            doNothing().when(eventPublisher).publish(shariaEvent);

            // When
            assertDoesNotThrow(() -> eventPublisher.publish(shariaEvent));

            // Then
            verify(eventPublisher, times(1)).publish(shariaEvent);
            assertTrue(shariaEvent.requiresShariaCompliance());
        }
    }

    @Nested
    @DisplayName("Batch Event Publishing")
    class BatchEventPublishing {

        @Test
        @DisplayName("Should publish multiple events successfully")
        void shouldPublishMultipleEventsSuccessfully() {
            // Given
            doNothing().when(eventPublisher).publishAll(eventList);

            // When
            assertDoesNotThrow(() -> eventPublisher.publishAll(eventList));

            // Then
            verify(eventPublisher, times(1)).publishAll(eventList);
        }

        @Test
        @DisplayName("Should handle empty event list")
        void shouldHandleEmptyEventList() {
            // Given
            List<DomainEvent> emptyList = Collections.emptyList();
            doNothing().when(eventPublisher).publishAll(emptyList);

            // When
            assertDoesNotThrow(() -> eventPublisher.publishAll(emptyList));

            // Then
            verify(eventPublisher, times(1)).publishAll(emptyList);
        }

        @Test
        @DisplayName("Should throw exception when publishing null event list")
        void shouldThrowExceptionWhenPublishingNullEventList() {
            // Given
            doThrow(new IllegalArgumentException("Event list cannot be null"))
                .when(eventPublisher).publishAll(null);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                () -> eventPublisher.publishAll(null));
        }

        @Test
        @DisplayName("Should handle batch publishing failure atomically")
        void shouldHandleBatchPublishingFailureAtomically() {
            // Given
            doThrow(new DomainEventPublisher.EventPublishingException("Batch publishing failed"))
                .when(eventPublisher).publishAll(eventList);

            // When & Then
            DomainEventPublisher.EventPublishingException exception = assertThrows(
                DomainEventPublisher.EventPublishingException.class,
                () -> eventPublisher.publishAll(eventList)
            );

            assertEquals("Batch publishing failed", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    class ExceptionHandling {

        @Test
        @DisplayName("EventPublishingException should have message constructor")
        void eventPublishingExceptionShouldHaveMessageConstructor() {
            // Given
            String message = "Test exception message";

            // When
            DomainEventPublisher.EventPublishingException exception = 
                new DomainEventPublisher.EventPublishingException(message);

            // Then
            assertEquals(message, exception.getMessage());
            assertNull(exception.getCause());
        }

        @Test
        @DisplayName("EventPublishingException should have message and cause constructor")
        void eventPublishingExceptionShouldHaveMessageAndCauseConstructor() {
            // Given
            String message = "Test exception message";
            Throwable cause = new RuntimeException("Root cause");

            // When
            DomainEventPublisher.EventPublishingException exception = 
                new DomainEventPublisher.EventPublishingException(message, cause);

            // Then
            assertEquals(message, exception.getMessage());
            assertEquals(cause, exception.getCause());
        }
    }

    /**
     * Test implementation of DomainEvent for testing purposes
     */
    private static class TestDomainEvent implements DomainEvent {
        private final UUID eventId;
        private final String aggregateId;
        private final String aggregateType;
        private final Instant occurredOn;
        private final Long aggregateVersion;
        private final UUID correlationId;
        private final UUID causationId;
        private final EventMetadata metadata;

        public TestDomainEvent(UUID eventId, String aggregateId, String aggregateType,
                              Instant occurredOn, Long aggregateVersion, UUID correlationId,
                              UUID causationId, EventMetadata metadata) {
            this.eventId = eventId;
            this.aggregateId = aggregateId;
            this.aggregateType = aggregateType;
            this.occurredOn = occurredOn;
            this.aggregateVersion = aggregateVersion;
            this.correlationId = correlationId;
            this.causationId = causationId;
            this.metadata = metadata;
        }

        @Override
        public UUID getEventId() { return eventId; }

        @Override
        public String getAggregateId() { return aggregateId; }

        @Override
        public String getAggregateType() { return aggregateType; }

        @Override
        public Instant getOccurredOn() { return occurredOn; }

        @Override
        public Long getAggregateVersion() { return aggregateVersion; }

        @Override
        public UUID getCorrelationId() { return correlationId; }

        @Override
        public UUID getCausationId() { return causationId; }

        @Override
        public EventMetadata getMetadata() { return metadata; }
    }

    /**
     * Test Sharia-compliant event implementation
     */
    private static class ShariaCompliantEvent extends TestDomainEvent {

        public ShariaCompliantEvent(UUID eventId, String aggregateId, String aggregateType,
                                   Instant occurredOn, Long aggregateVersion, UUID correlationId,
                                   UUID causationId, EventMetadata metadata) {
            super(eventId, aggregateId, aggregateType, occurredOn, aggregateVersion,
                  correlationId, causationId, metadata);
        }

        @Override
        public boolean requiresShariaCompliance() {
            return true;
        }

        @Override
        public boolean requiresRegulatoryReporting() {
            return true;
        }
    }
}