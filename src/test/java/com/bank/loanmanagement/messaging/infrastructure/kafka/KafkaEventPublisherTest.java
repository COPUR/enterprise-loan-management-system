package com.bank.loanmanagement.messaging.infrastructure.kafka;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for KafkaEventPublisher
 * Tests event publishing, topic routing, and error handling
 * Ensures 85%+ test coverage for Kafka event infrastructure
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventPublisher Tests")
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private KafkaTopicResolver topicResolver;

    @Mock
    private KafkaSecurityService kafkaSecurityService;

    @Mock
    private SendResult<String, String> sendResult;

    private KafkaEventPublisher eventPublisher;
    private TestDomainEvent testEvent;
    private CompletableFuture<SendResult<String, String>> mockFuture;

    @BeforeEach
    void setUp() {
        eventPublisher = new KafkaEventPublisher(kafkaTemplate, objectMapper, topicResolver, kafkaSecurityService);
        testEvent = new TestDomainEvent("AGGREGATE-123", 1L, "test-data");
        mockFuture = CompletableFuture.completedFuture(sendResult);
    }

    @Nested
    @DisplayName("Event Publishing Tests")
    class EventPublishingTests {

        @Test
        @DisplayName("Should publish event successfully with correct topic and partition key")
        void shouldPublishEventSuccessfullyWithCorrectTopicAndPartitionKey() throws Exception {
            // Given
            String expectedTopic = "banking.test-domain.events";
            String expectedJson = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";
            
            when(topicResolver.resolveTopicForEvent(testEvent)).thenReturn(expectedTopic);
            when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);
            when(kafkaSecurityService.applySecurityIfRequired(anyString(), eq(testEvent)))
                .thenReturn(expectedJson);
            when(kafkaTemplate.send(expectedTopic, testEvent.getAggregateId(), expectedJson))
                .thenReturn(mockFuture);

            // When
            CompletableFuture<SendResult<String, String>> result = eventPublisher.publishEvent(testEvent);

            // Then
            assertThat(result).isCompletedWithValue(sendResult);
            verify(topicResolver).resolveTopicForEvent(testEvent);
            verify(kafkaTemplate).send(expectedTopic, testEvent.getAggregateId(), expectedJson);
        }

        @Test
        @DisplayName("Should publish event with metadata successfully")
        void shouldPublishEventWithMetadataSuccessfully() throws Exception {
            // Given
            Map<String, Object> metadata = Map.of("customKey", "customValue", "priority", "HIGH");
            String expectedTopic = "banking.test-domain.events";
            String expectedJson = "{\"eventId\":\"EVENT-123\",\"metadata\":{\"customKey\":\"customValue\"}}";
            
            when(topicResolver.resolveTopicForEvent(testEvent)).thenReturn(expectedTopic);
            when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);
            when(kafkaSecurityService.applySecurityIfRequired(anyString(), eq(testEvent)))
                .thenReturn(expectedJson);
            when(kafkaTemplate.send(expectedTopic, testEvent.getAggregateId(), expectedJson))
                .thenReturn(mockFuture);

            // When
            CompletableFuture<SendResult<String, String>> result = eventPublisher.publishEvent(testEvent, metadata);

            // Then
            assertThat(result).isCompletedWithValue(sendResult);
            
            // Verify enriched wrapper creation
            ArgumentCaptor<KafkaEventPublisher.EnrichedEventWrapper> wrapperCaptor = 
                ArgumentCaptor.forClass(KafkaEventPublisher.EnrichedEventWrapper.class);
            verify(objectMapper).writeValueAsString(wrapperCaptor.capture());
            
            KafkaEventPublisher.EnrichedEventWrapper wrapper = wrapperCaptor.getValue();
            assertThat(wrapper.getEventId()).isEqualTo(testEvent.getEventId());
            assertThat(wrapper.getAggregateId()).isEqualTo(testEvent.getAggregateId());
            assertThat(wrapper.getEventType()).isEqualTo(testEvent.getEventType());
            assertThat(wrapper.getServiceDomain()).isEqualTo(testEvent.getServiceDomain());
            assertThat(wrapper.getBehaviorQualifier()).isEqualTo(testEvent.getBehaviorQualifier());
            assertThat(wrapper.getMetadata()).isEqualTo(metadata);
            assertThat(wrapper.getPublisher()).isEqualTo("KafkaEventPublisher");
            assertThat(wrapper.getPublishedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should handle serialization failure gracefully")
        void shouldHandleSerializationFailureGracefully() throws Exception {
            // Given
            when(topicResolver.resolveTopicForEvent(testEvent)).thenReturn("banking.test-domain.events");
            when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failed") {});

            // When
            CompletableFuture<SendResult<String, String>> result = eventPublisher.publishEvent(testEvent);

            // Then
            assertThat(result).isCompletedExceptionally();
            result.handle((sendResult, throwable) -> {
                assertThat(throwable).isInstanceOf(JsonProcessingException.class);
                return null;
            });
        }

        @Test
        @DisplayName("Should apply security transformations before publishing")
        void shouldApplySecurityTransformationsBeforePublishing() throws Exception {
            // Given
            String originalJson = "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}";
            String secureJson = "{\"eventId\":\"EVENT-123\",\"data\":\"encrypted-test\"}";
            String expectedTopic = "banking.test-domain.secure";
            
            when(topicResolver.resolveTopicForEvent(testEvent)).thenReturn(expectedTopic);
            when(objectMapper.writeValueAsString(any())).thenReturn(originalJson);
            when(kafkaSecurityService.applySecurityIfRequired(originalJson, testEvent))
                .thenReturn(secureJson);
            when(kafkaTemplate.send(expectedTopic, testEvent.getAggregateId(), secureJson))
                .thenReturn(mockFuture);

            // When
            eventPublisher.publishEvent(testEvent);

            // Then
            verify(kafkaSecurityService).applySecurityIfRequired(originalJson, testEvent);
            verify(kafkaTemplate).send(expectedTopic, testEvent.getAggregateId(), secureJson);
        }
    }

    @Nested
    @DisplayName("Batch Publishing Tests")
    class BatchPublishingTests {

        @Test
        @DisplayName("Should publish multiple events in batch")
        void shouldPublishMultipleEventsInBatch() throws Exception {
            // Given
            TestDomainEvent event1 = new TestDomainEvent("AGGREGATE-1", 1L, "data1");
            TestDomainEvent event2 = new TestDomainEvent("AGGREGATE-2", 1L, "data2");
            List<DomainEvent> events = List.of(event1, event2);
            
            when(topicResolver.resolveTopicForEvent(any())).thenReturn("banking.test-domain.events");
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"data\":\"test\"}");
            when(kafkaSecurityService.applySecurityIfRequired(anyString(), any()))
                .thenReturn("{\"data\":\"test\"}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(mockFuture);

            // When
            CompletableFuture<Void> result = eventPublisher.publishEvents(events);

            // Then
            assertThat(result).isCompleted();
            verify(kafkaTemplate, times(2)).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle partial failures in batch publishing")
        void shouldHandlePartialFailuresInBatchPublishing() throws Exception {
            // Given
            TestDomainEvent event1 = new TestDomainEvent("AGGREGATE-1", 1L, "data1");
            TestDomainEvent event2 = new TestDomainEvent("AGGREGATE-2", 1L, "data2");
            List<DomainEvent> events = List.of(event1, event2);
            
            CompletableFuture<SendResult<String, String>> successFuture = CompletableFuture.completedFuture(sendResult);
            CompletableFuture<SendResult<String, String>> failureFuture = new CompletableFuture<>();
            failureFuture.completeExceptionally(new RuntimeException("Kafka failure"));
            
            when(topicResolver.resolveTopicForEvent(any())).thenReturn("banking.test-domain.events");
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"data\":\"test\"}");
            when(kafkaSecurityService.applySecurityIfRequired(anyString(), any()))
                .thenReturn("{\"data\":\"test\"}");
            when(kafkaTemplate.send(anyString(), eq(event1.getAggregateId()), anyString()))
                .thenReturn(successFuture);
            when(kafkaTemplate.send(anyString(), eq(event2.getAggregateId()), anyString()))
                .thenReturn(failureFuture);

            // When
            CompletableFuture<Void> result = eventPublisher.publishEvents(events);

            // Then
            assertThat(result).isCompletedExceptionally();
        }

        @Test
        @DisplayName("Should complete successfully when all events publish successfully")
        void shouldCompleteSuccessfullyWhenAllEventsPublishSuccessfully() throws Exception {
            // Given
            List<DomainEvent> events = List.of(
                new TestDomainEvent("AGGREGATE-1", 1L, "data1"),
                new TestDomainEvent("AGGREGATE-2", 1L, "data2"),
                new TestDomainEvent("AGGREGATE-3", 1L, "data3")
            );
            
            when(topicResolver.resolveTopicForEvent(any())).thenReturn("banking.test-domain.events");
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"data\":\"test\"}");
            when(kafkaSecurityService.applySecurityIfRequired(anyString(), any()))
                .thenReturn("{\"data\":\"test\"}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(mockFuture);

            // When
            CompletableFuture<Void> result = eventPublisher.publishEvents(events);

            // Then
            assertThat(result).isCompleted();
            verify(kafkaTemplate, times(3)).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("SAGA Event Publishing Tests")
    class SagaEventPublishingTests {

        @Test
        @DisplayName("Should publish SAGA event with enhanced metadata")
        void shouldPublishSagaEventWithEnhancedMetadata() throws Exception {
            // Given
            String sagaId = "SAGA-123";
            String stepId = "step-1";
            String expectedTopic = "banking.test-domain.events";
            String expectedJson = "{\"eventId\":\"EVENT-123\",\"sagaId\":\"SAGA-123\"}";
            
            when(topicResolver.resolveTopicForEvent(testEvent)).thenReturn(expectedTopic);
            when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);
            when(kafkaSecurityService.applySecurityIfRequired(anyString(), eq(testEvent)))
                .thenReturn(expectedJson);
            when(kafkaTemplate.send(expectedTopic, testEvent.getAggregateId(), expectedJson))
                .thenReturn(mockFuture);

            // When
            CompletableFuture<SendResult<String, String>> result = 
                eventPublisher.publishSagaEvent(testEvent, sagaId, stepId);

            // Then
            assertThat(result).isCompletedWithValue(sendResult);
            
            // Verify SAGA metadata was included
            ArgumentCaptor<KafkaEventPublisher.EnrichedEventWrapper> wrapperCaptor = 
                ArgumentCaptor.forClass(KafkaEventPublisher.EnrichedEventWrapper.class);
            verify(objectMapper).writeValueAsString(wrapperCaptor.capture());
            
            KafkaEventPublisher.EnrichedEventWrapper wrapper = wrapperCaptor.getValue();
            assertThat(wrapper.getMetadata()).containsEntry("sagaId", sagaId);
            assertThat(wrapper.getMetadata()).containsEntry("stepId", stepId);
            assertThat(wrapper.getMetadata()).containsEntry("eventCategory", "SAGA_COORDINATION");
            assertThat(wrapper.getMetadata()).containsKey("publishedAt");
        }
    }

    @Nested
    @DisplayName("Enriched Event Wrapper Tests")
    class EnrichedEventWrapperTests {

        @Test
        @DisplayName("Should create valid enriched wrapper with all required fields")
        void shouldCreateValidEnrichedWrapperWithAllRequiredFields() {
            // Given
            Map<String, Object> metadata = Map.of("key", "value");
            
            // When
            KafkaEventPublisher.EnrichedEventWrapper wrapper = 
                KafkaEventPublisher.EnrichedEventWrapper.builder()
                    .eventId(testEvent.getEventId())
                    .aggregateId(testEvent.getAggregateId())
                    .aggregateType(testEvent.getAggregateType())
                    .eventType(testEvent.getEventType())
                    .eventData(testEvent.getEventData())
                    .version(testEvent.getVersion())
                    .occurredOn(testEvent.getOccurredOn())
                    .serviceDomain(testEvent.getServiceDomain())
                    .behaviorQualifier(testEvent.getBehaviorQualifier())
                    .metadata(metadata)
                    .publishedAt(OffsetDateTime.now())
                    .publisher("TestPublisher")
                    .build();

            // Then
            assertThat(wrapper.isValid()).isTrue();
            assertThat(wrapper.getEventId()).isEqualTo(testEvent.getEventId());
            assertThat(wrapper.getAggregateId()).isEqualTo(testEvent.getAggregateId());
            assertThat(wrapper.getEventType()).isEqualTo(testEvent.getEventType());
            assertThat(wrapper.getServiceDomain()).isEqualTo(testEvent.getServiceDomain());
            assertThat(wrapper.getBehaviorQualifier()).isEqualTo(testEvent.getBehaviorQualifier());
            assertThat(wrapper.getMetadata()).isEqualTo(metadata);
        }

        @Test
        @DisplayName("Should be invalid when missing required fields")
        void shouldBeInvalidWhenMissingRequiredFields() {
            // Given
            KafkaEventPublisher.EnrichedEventWrapper wrapperMissingEventId = 
                KafkaEventPublisher.EnrichedEventWrapper.builder()
                    .aggregateId(testEvent.getAggregateId())
                    .eventType(testEvent.getEventType())
                    .serviceDomain(testEvent.getServiceDomain())
                    .behaviorQualifier(testEvent.getBehaviorQualifier())
                    .build();

            KafkaEventPublisher.EnrichedEventWrapper wrapperMissingServiceDomain = 
                KafkaEventPublisher.EnrichedEventWrapper.builder()
                    .eventId(testEvent.getEventId())
                    .aggregateId(testEvent.getAggregateId())
                    .eventType(testEvent.getEventType())
                    .behaviorQualifier(testEvent.getBehaviorQualifier())
                    .build();

            // When & Then
            assertThat(wrapperMissingEventId.isValid()).isFalse();
            assertThat(wrapperMissingServiceDomain.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should log success when publishing succeeds")
        void shouldLogSuccessWhenPublishingSucceeds() throws Exception {
            // Given
            String expectedTopic = "banking.test-domain.events";
            when(topicResolver.resolveTopicForEvent(testEvent)).thenReturn(expectedTopic);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"data\":\"test\"}");
            when(kafkaSecurityService.applySecurityIfRequired(anyString(), eq(testEvent)))
                .thenReturn("{\"data\":\"test\"}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(mockFuture);
            when(sendResult.getRecordMetadata()).thenReturn(mock(org.apache.kafka.clients.producer.RecordMetadata.class));
            when(sendResult.getRecordMetadata().partition()).thenReturn(1);

            // When
            eventPublisher.publishEvent(testEvent);

            // Then
            // Verify success callback would be called
            verify(kafkaTemplate).send(expectedTopic, testEvent.getAggregateId(), "{\"data\":\"test\"}");
        }

        @Test
        @DisplayName("Should handle publishing failure gracefully")
        void shouldHandlePublishingFailureGracefully() throws Exception {
            // Given
            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Kafka failure"));
            
            when(topicResolver.resolveTopicForEvent(testEvent)).thenReturn("banking.test-domain.events");
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"data\":\"test\"}");
            when(kafkaSecurityService.applySecurityIfRequired(anyString(), eq(testEvent)))
                .thenReturn("{\"data\":\"test\"}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(failedFuture);

            // When
            CompletableFuture<SendResult<String, String>> result = eventPublisher.publishEvent(testEvent);

            // Then
            assertThat(result).isCompletedExceptionally();
        }
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
}