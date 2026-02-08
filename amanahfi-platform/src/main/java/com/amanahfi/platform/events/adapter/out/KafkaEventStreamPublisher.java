package com.amanahfi.platform.events.adapter.out;

import com.amanahfi.platform.events.port.out.EventStreamPublisher;
import com.amanahfi.platform.events.port.out.StreamStats;
import com.amanahfi.platform.shared.domain.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka implementation of event stream publisher
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventStreamPublisher implements EventStreamPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    // Statistics tracking
    private final ConcurrentHashMap<String, AtomicLong> eventCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> failedCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastEventTimes = new ConcurrentHashMap<>();
    
    @Override
    public void publish(String streamName, DomainEvent event) {
        try {
            log.debug("Publishing event to stream: {} - Event: {}", streamName, event.getClass().getSimpleName());
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(streamName, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Event published successfully to stream: {} - Partition: {} - Offset: {}", 
                        streamName, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    updateSuccessStats(streamName);
                } else {
                    log.error("Failed to publish event to stream: {} - Error: {}", streamName, ex.getMessage());
                    updateFailureStats(streamName);
                }
            });
            
        } catch (Exception e) {
            log.error("Failed to serialize event for stream: {} - Error: {}", streamName, e.getMessage());
            updateFailureStats(streamName);
        }
    }
    
    @Override
    public void publish(String streamName, List<DomainEvent> events) {
        log.debug("Publishing {} events to stream: {}", events.size(), streamName);
        
        for (DomainEvent event : events) {
            publish(streamName, event);
        }
    }
    
    @Override
    public void publishWithPartitionKey(String streamName, DomainEvent event, String partitionKey) {
        try {
            log.debug("Publishing event to stream: {} with partition key: {} - Event: {}", 
                streamName, partitionKey, event.getClass().getSimpleName());
            
            String eventJson = objectMapper.writeValueAsString(event);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(streamName, partitionKey, eventJson);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Event published successfully to stream: {} - Partition: {} - Offset: {}", 
                        streamName, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    updateSuccessStats(streamName);
                } else {
                    log.error("Failed to publish event to stream: {} - Error: {}", streamName, ex.getMessage());
                    updateFailureStats(streamName);
                }
            });
            
        } catch (Exception e) {
            log.error("Failed to serialize event for stream: {} - Error: {}", streamName, e.getMessage());
            updateFailureStats(streamName);
        }
    }
    
    @Override
    public void publishWithPartitionKey(String streamName, List<DomainEvent> events, String partitionKey) {
        log.debug("Publishing {} events to stream: {} with partition key: {}", events.size(), streamName, partitionKey);
        
        for (DomainEvent event : events) {
            publishWithPartitionKey(streamName, event, partitionKey);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Simple health check by trying to get metadata
            kafkaTemplate.getProducerFactory().createProducer().partitionsFor("amanahfi.health-check");
            return true;
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public StreamStats getStreamStats(String streamName) {
        AtomicLong totalCount = eventCounts.getOrDefault(streamName, new AtomicLong(0));
        AtomicLong failedCount = failedCounts.getOrDefault(streamName, new AtomicLong(0));
        Instant lastEventTime = lastEventTimes.getOrDefault(streamName, Instant.EPOCH);
        
        return StreamStats.builder()
            .streamName(streamName)
            .totalEvents(totalCount.get())
            .eventsSentToday(totalCount.get()) // Simplified for now
            .failedEvents(failedCount.get())
            .avgLatencyMs(0.0) // Would need more sophisticated tracking
            .lastEventTime(lastEventTime)
            .isHealthy(isHealthy())
            .build();
    }
    
    private void updateSuccessStats(String streamName) {
        eventCounts.computeIfAbsent(streamName, k -> new AtomicLong(0)).incrementAndGet();
        lastEventTimes.put(streamName, Instant.now());
    }
    
    private void updateFailureStats(String streamName) {
        failedCounts.computeIfAbsent(streamName, k -> new AtomicLong(0)).incrementAndGet();
    }
}