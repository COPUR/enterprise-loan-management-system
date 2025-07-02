package com.bank.loanmanagement.loan.messaging.infrastructure.kafka;

import com.bank.loanmanagement.loan.sharedkernel.domain.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for KafkaEventConsumer
 * Tests event consumption, error handling, retry logic, and dead letter queues
 * Ensures 85%+ test coverage for Kafka consumer infrastructure
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaEventConsumer Tests")
class KafkaEventConsumerTest {

    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private EventProcessor eventProcessor;
    
    @Mock
    private KafkaTemplate<String, String> retryTemplate;
    
    @Mock
    private KafkaTopicResolver topicResolver;
    
    @Mock
    private DeadLetterPublishingRecoverer deadLetterRecoverer;
    
    @Mock
    private Acknowledgment acknowledgment;

    private KafkaEventConsumer eventConsumer;
    private ConsumerRecord<String, String> validRecord;
    private TestDomainEvent testEvent;

    @BeforeEach
    void setUp() {
        eventConsumer = new KafkaEventConsumer(
            objectMapper, eventProcessor, retryTemplate, topicResolver, deadLetterRecoverer);
        
        testEvent = new TestDomainEvent("AGGREGATE-123", 1L, "test-data");
        validRecord = new ConsumerRecord<>(
            "banking.consumer-loan.commands", 
            0, 
            100L, 
            "AGGREGATE-123", 
            "{\"eventId\":\"EVENT-123\",\"data\":\"test\"}"
        );
    }

    @Nested
    @DisplayName("Event Consumption Tests")
    class EventConsumptionTests {

        @Test
        @DisplayName("Should consume and process valid event successfully")
        void shouldConsumeAndProcessValidEventSuccessfully() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\",\"eventType\":\"TestEvent\"}";
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            
            when(objectMapper.readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            eventConsumer.consumeEvent(eventJson, "banking.consumer-loan.commands", 0, 100L, acknowledgment);

            // Then
            verify(objectMapper).readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class);
            verify(eventProcessor).processEvent(any(DomainEvent.class), eq("banking.consumer-loan.commands"));
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should handle batch consumption successfully")
        void shouldHandleBatchConsumptionSuccessfully() throws Exception {
            // Given
            List<ConsumerRecord<String, String>> records = List.of(
                createRecord("EVENT-1", "AGGREGATE-1"),
                createRecord("EVENT-2", "AGGREGATE-2"),
                createRecord("EVENT-3", "AGGREGATE-3")
            );
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            
            when(objectMapper.readValue(anyString(), eq(KafkaEventConsumer.EnrichedEventWrapper.class)))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            eventConsumer.consumeEventBatch(records, acknowledgment);

            // Then
            verify(objectMapper, times(3)).readValue(anyString(), eq(KafkaEventConsumer.EnrichedEventWrapper.class));
            verify(eventProcessor, times(3)).processEvent(any(DomainEvent.class), anyString());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should handle SAGA coordination events")
        void shouldHandleSagaCoordinationEvents() throws Exception {
            // Given
            String sagaTopic = "banking.consumer-loan.saga.loanoriginationsaga";
            String eventJson = "{\"eventId\":\"SAGA-EVENT-123\",\"sagaId\":\"SAGA-123\"}";
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createSagaWrapper();
            
            when(objectMapper.readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class))
                .thenReturn(wrapper);
            when(eventProcessor.processSagaEvent(any(DomainEvent.class), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            eventConsumer.consumeSagaEvent(eventJson, sagaTopic, 0, 100L, acknowledgment);

            // Then
            verify(eventProcessor).processSagaEvent(any(DomainEvent.class), eq(sagaTopic), anyString());
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should handle secure topic events with enhanced validation")
        void shouldHandleSecureTopicEventsWithEnhancedValidation() throws Exception {
            // Given
            String secureTopic = "banking.payment-initiation.commands.secure";
            String eventJson = "{\"eventId\":\"SECURE-EVENT-123\",\"encrypted\":true}";
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createSecureWrapper();
            
            when(objectMapper.readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class))
                .thenReturn(wrapper);
            when(eventProcessor.processSecureEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            eventConsumer.consumeSecureEvent(eventJson, secureTopic, 0, 100L, acknowledgment);

            // Then
            verify(eventProcessor).processSecureEvent(any(DomainEvent.class), eq(secureTopic));
            verify(acknowledgment).acknowledge();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle JSON deserialization failure gracefully")
        void shouldHandleJsonDeserializationFailureGracefully() throws Exception {
            // Given
            String invalidJson = "{invalid-json}";
            when(objectMapper.readValue(invalidJson, KafkaEventConsumer.EnrichedEventWrapper.class))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

            // When & Then
            assertThatThrownBy(() -> eventConsumer.consumeEvent(
                invalidJson, "banking.consumer-loan.commands", 0, 100L, acknowledgment))
                .isInstanceOf(KafkaEventConsumer.EventProcessingException.class)
                .hasMessageContaining("Failed to deserialize event");
            
            verify(acknowledgment, never()).acknowledge();
        }

        @Test
        @DisplayName("Should handle event processing failure with retry")
        void shouldHandleEventProcessingFailureWithRetry() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\"}";
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            
            when(objectMapper.readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing failed")));
            when(topicResolver.getRetryTopic("banking.consumer-loan.commands", 1))
                .thenReturn("banking.consumer-loan.commands.retry.1");

            // When & Then
            assertThatThrownBy(() -> eventConsumer.consumeEvent(
                eventJson, "banking.consumer-loan.commands", 0, 100L, acknowledgment))
                .isInstanceOf(KafkaEventConsumer.EventProcessingException.class)
                .hasMessageContaining("Event processing failed");

            verify(retryTemplate).send(eq("banking.consumer-loan.commands.retry.1"), anyString(), anyString());
            verify(acknowledgment, never()).acknowledge();
        }

        @Test
        @DisplayName("Should send to dead letter queue after max retries")
        void shouldSendToDeadLetterQueueAfterMaxRetries() throws Exception {
            // Given
            String retryTopic = "banking.consumer-loan.commands.retry.3";
            String eventJson = "{\"eventId\":\"EVENT-123\",\"retryCount\":3}";
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            wrapper.getMetadata().put("retryCount", 3);
            
            when(objectMapper.readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Max retries exceeded")));
            when(topicResolver.getDeadLetterTopic("banking.consumer-loan.commands"))
                .thenReturn("banking.consumer-loan.commands.dlq");

            // When & Then
            assertThatThrownBy(() -> eventConsumer.consumeEvent(
                eventJson, retryTopic, 0, 100L, acknowledgment))
                .isInstanceOf(KafkaEventConsumer.EventProcessingException.class)
                .hasMessageContaining("Max retries exceeded");

            verify(deadLetterRecoverer).accept(any(ConsumerRecord.class), any(Exception.class));
            verify(acknowledgment, never()).acknowledge();
        }

        @Test
        @DisplayName("Should handle partial batch failure")
        void shouldHandlePartialBatchFailure() throws Exception {
            // Given
            List<ConsumerRecord<String, String>> records = List.of(
                createRecord("EVENT-1", "AGGREGATE-1"),
                createRecord("EVENT-2", "AGGREGATE-2"),
                createRecord("EVENT-3", "AGGREGATE-3")
            );
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            
            when(objectMapper.readValue(anyString(), eq(KafkaEventConsumer.EnrichedEventWrapper.class)))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing failed")))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When & Then
            assertThatThrownBy(() -> eventConsumer.consumeEventBatch(records, acknowledgment))
                .isInstanceOf(KafkaEventConsumer.BatchProcessingException.class)
                .hasMessageContaining("Batch processing failed");

            verify(eventProcessor, times(3)).processEvent(any(DomainEvent.class), anyString());
            verify(acknowledgment, never()).acknowledge();
        }
    }

    @Nested
    @DisplayName("Retry Logic Tests")
    class RetryLogicTests {

        @Test
        @DisplayName("Should increment retry count on retry")
        void shouldIncrementRetryCountOnRetry() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\",\"retryCount\":1}";
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            wrapper.getMetadata().put("retryCount", 1);
            
            when(objectMapper.readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing failed")));
            when(topicResolver.getRetryTopic("banking.consumer-loan.commands", 2))
                .thenReturn("banking.consumer-loan.commands.retry.2");
            when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"eventId\":\"EVENT-123\",\"retryCount\":2}");

            // When & Then
            assertThatThrownBy(() -> eventConsumer.consumeEvent(
                eventJson, "banking.consumer-loan.commands", 0, 100L, acknowledgment));

            // Verify retry message has incremented retry count
            ArgumentCaptor<String> retryMessageCaptor = ArgumentCaptor.forClass(String.class);
            verify(retryTemplate).send(eq("banking.consumer-loan.commands.retry.2"), anyString(), retryMessageCaptor.capture());
            
            String retryMessage = retryMessageCaptor.getValue();
            assertThat(retryMessage).contains("\"retryCount\":2");
        }

        @Test
        @DisplayName("Should apply exponential backoff delay")
        void shouldApplyExponentialBackoffDelay() throws Exception {
            // Given
            String eventJson = "{\"eventId\":\"EVENT-123\"}";
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            
            when(objectMapper.readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing failed")));

            // When
            long delayLevel1 = eventConsumer.calculateRetryDelay(1);
            long delayLevel2 = eventConsumer.calculateRetryDelay(2);
            long delayLevel3 = eventConsumer.calculateRetryDelay(3);

            // Then
            assertThat(delayLevel1).isEqualTo(1000L); // 1 second
            assertThat(delayLevel2).isEqualTo(2000L); // 2 seconds
            assertThat(delayLevel3).isEqualTo(4000L); // 4 seconds
        }

        @Test
        @DisplayName("Should respect maximum retry limit")
        void shouldRespectMaximumRetryLimit() {
            // When
            boolean shouldRetryLevel1 = eventConsumer.shouldRetry(1);
            boolean shouldRetryLevel2 = eventConsumer.shouldRetry(2);
            boolean shouldRetryLevel3 = eventConsumer.shouldRetry(3);
            boolean shouldRetryLevel4 = eventConsumer.shouldRetry(4);

            // Then
            assertThat(shouldRetryLevel1).isTrue();
            assertThat(shouldRetryLevel2).isTrue();
            assertThat(shouldRetryLevel3).isTrue();
            assertThat(shouldRetryLevel4).isFalse(); // Max retries = 3
        }
    }

    @Nested
    @DisplayName("Dead Letter Queue Tests")
    class DeadLetterQueueTests {

        @Test
        @DisplayName("Should consume and process dead letter queue events")
        void shouldConsumeAndProcessDeadLetterQueueEvents() throws Exception {
            // Given
            String dlqTopic = "banking.consumer-loan.commands.dlq";
            String eventJson = "{\"eventId\":\"DLQ-EVENT-123\"}";
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            
            when(objectMapper.readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class))
                .thenReturn(wrapper);

            // When
            eventConsumer.consumeDeadLetterEvent(eventJson, dlqTopic, 0, 100L, acknowledgment);

            // Then
            verify(objectMapper).readValue(eventJson, KafkaEventConsumer.EnrichedEventWrapper.class);
            // DLQ events are logged but not reprocessed
            verify(acknowledgment).acknowledge();
        }

        @Test
        @DisplayName("Should track dead letter queue statistics")
        void shouldTrackDeadLetterQueueStatistics() throws Exception {
            // Given
            String dlqTopic = "banking.consumer-loan.commands.dlq";
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            
            when(objectMapper.readValue(anyString(), eq(KafkaEventConsumer.EnrichedEventWrapper.class)))
                .thenReturn(wrapper);

            // When
            eventConsumer.consumeDeadLetterEvent("{\"eventId\":\"DLQ-1\"}", dlqTopic, 0, 100L, acknowledgment);
            eventConsumer.consumeDeadLetterEvent("{\"eventId\":\"DLQ-2\"}", dlqTopic, 0, 101L, acknowledgment);

            // Then
            KafkaEventConsumer.ConsumerStatistics stats = eventConsumer.getStatistics();
            assertThat(stats.getTotalDeadLetterEvents()).isEqualTo(2L);
            assertThat(stats.getDeadLetterEventsByTopic()).containsEntry(dlqTopic, 2L);
        }
    }

    @Nested
    @DisplayName("Consumer Statistics Tests")
    class ConsumerStatisticsTests {

        @Test
        @DisplayName("Should track processing statistics accurately")
        void shouldTrackProcessingStatisticsAccurately() throws Exception {
            // Given
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            when(objectMapper.readValue(anyString(), eq(KafkaEventConsumer.EnrichedEventWrapper.class)))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            eventConsumer.consumeEvent("{\"eventId\":\"1\"}", "topic1", 0, 100L, acknowledgment);
            eventConsumer.consumeEvent("{\"eventId\":\"2\"}", "topic2", 0, 101L, acknowledgment);

            // Then
            KafkaEventConsumer.ConsumerStatistics stats = eventConsumer.getStatistics();
            assertThat(stats.getTotalEventsProcessed()).isEqualTo(2L);
            assertThat(stats.getSuccessfulEvents()).isEqualTo(2L);
            assertThat(stats.getFailedEvents()).isEqualTo(0L);
            assertThat(stats.getEventsByTopic()).containsEntry("topic1", 1L);
            assertThat(stats.getEventsByTopic()).containsEntry("topic2", 1L);
        }

        @Test
        @DisplayName("Should track processing errors in statistics")
        void shouldTrackProcessingErrorsInStatistics() throws Exception {
            // Given
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            when(objectMapper.readValue(anyString(), eq(KafkaEventConsumer.EnrichedEventWrapper.class)))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing failed")));

            // When
            try {
                eventConsumer.consumeEvent("{\"eventId\":\"1\"}", "topic1", 0, 100L, acknowledgment);
            } catch (Exception e) {
                // Expected
            }

            // Then
            KafkaEventConsumer.ConsumerStatistics stats = eventConsumer.getStatistics();
            assertThat(stats.getTotalEventsProcessed()).isEqualTo(1L);
            assertThat(stats.getSuccessfulEvents()).isEqualTo(0L);
            assertThat(stats.getFailedEvents()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should calculate accurate processing rates")
        void shouldCalculateAccurateProcessingRates() throws Exception {
            // Given
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            when(objectMapper.readValue(anyString(), eq(KafkaEventConsumer.EnrichedEventWrapper.class)))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            for (int i = 0; i < 10; i++) {
                eventConsumer.consumeEvent("{\"eventId\":\"" + i + "\"}", "topic1", 0, 100L + i, acknowledgment);
            }

            // Then
            KafkaEventConsumer.ConsumerStatistics stats = eventConsumer.getStatistics();
            assertThat(stats.getSuccessRate()).isEqualTo(100.0);
            assertThat(stats.getAverageProcessingTimeMs()).isGreaterThan(0.0);
        }
    }

    @Nested
    @DisplayName("Consumer Health Check Tests")
    class ConsumerHealthCheckTests {

        @Test
        @DisplayName("Should report healthy when processing normally")
        void shouldReportHealthyWhenProcessingNormally() throws Exception {
            // Given
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            when(objectMapper.readValue(anyString(), eq(KafkaEventConsumer.EnrichedEventWrapper.class)))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

            // When
            eventConsumer.consumeEvent("{\"eventId\":\"1\"}", "topic1", 0, 100L, acknowledgment);

            // Then
            KafkaEventConsumer.HealthStatus health = eventConsumer.getHealth();
            assertThat(health.isHealthy()).isTrue();
            assertThat(health.getStatusMessage()).contains("Consumer operating normally");
        }

        @Test
        @DisplayName("Should report unhealthy when error rate is high")
        void shouldReportUnhealthyWhenErrorRateIsHigh() throws Exception {
            // Given
            KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
            when(objectMapper.readValue(anyString(), eq(KafkaEventConsumer.EnrichedEventWrapper.class)))
                .thenReturn(wrapper);
            when(eventProcessor.processEvent(any(DomainEvent.class), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Processing failed")));

            // When - Simulate high error rate
            for (int i = 0; i < 10; i++) {
                try {
                    eventConsumer.consumeEvent("{\"eventId\":\"" + i + "\"}", "topic1", 0, 100L + i, acknowledgment);
                } catch (Exception e) {
                    // Expected
                }
            }

            // Then
            KafkaEventConsumer.HealthStatus health = eventConsumer.getHealth();
            assertThat(health.isHealthy()).isFalse();
            assertThat(health.getStatusMessage()).contains("High error rate detected");
        }
    }

    // Helper methods and test classes

    private ConsumerRecord<String, String> createRecord(String eventId, String aggregateId) {
        return new ConsumerRecord<>(
            "banking.consumer-loan.commands",
            0,
            100L,
            aggregateId,
            "{\"eventId\":\"" + eventId + "\",\"aggregateId\":\"" + aggregateId + "\"}"
        );
    }

    private KafkaEventConsumer.EnrichedEventWrapper createValidWrapper() {
        return KafkaEventConsumer.EnrichedEventWrapper.builder()
            .eventId("EVENT-123")
            .aggregateId("AGGREGATE-123")
            .aggregateType("TestAggregate")
            .eventType("TestDomainEvent")
            .eventData(Map.of("testData", "test"))
            .version(1L)
            .occurredOn(OffsetDateTime.now())
            .serviceDomain("TestDomain")
            .behaviorQualifier("TEST")
            .metadata(Map.of("priority", "HIGH"))
            .publishedAt(OffsetDateTime.now())
            .publisher("TestPublisher")
            .build();
    }

    private KafkaEventConsumer.EnrichedEventWrapper createSagaWrapper() {
        KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
        wrapper.getMetadata().put("sagaId", "SAGA-123");
        wrapper.getMetadata().put("stepId", "step-1");
        wrapper.getMetadata().put("eventCategory", "SAGA_COORDINATION");
        return wrapper;
    }

    private KafkaEventConsumer.EnrichedEventWrapper createSecureWrapper() {
        KafkaEventConsumer.EnrichedEventWrapper wrapper = createValidWrapper();
        wrapper.getMetadata().put("securityLevel", "HIGH");
        wrapper.getMetadata().put("encrypted", true);
        wrapper.getMetadata().put("fapiInteractionId", "FAPI-123");
        return wrapper;
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