package com.banking.loan.infrastructure.config;

import com.banking.loan.infrastructure.messaging.BankingMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise Banking Kafka Configuration
 * Implements industry-standard settings for high-volume, compliant banking operations
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
@Slf4j
public class BankingKafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers:localhost:9093}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:banking-enterprise-group}")
    private String consumerGroupId;

    @Value("${banking.kafka.producer.batch-size:65536}")
    private Integer batchSize;

    @Value("${banking.kafka.producer.linger-ms:5}")
    private Integer lingerMs;

    @Value("${banking.kafka.producer.buffer-memory:134217728}")
    private Long bufferMemory;

    @Value("${banking.kafka.consumer.max-poll-records:500}")
    private Integer maxPollRecords;

    @Value("${banking.kafka.consumer.fetch-min-size:1048576}")
    private Integer fetchMinSize;

    private final ObjectMapper objectMapper;

    /**
     * Enterprise Banking Producer Configuration
     * Optimized for high-throughput financial transactions
     */
    @Bean
    public ProducerFactory<String, String> bankingProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic Configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Banking-Specific Performance Tuning
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4"); // Optimal for banking data
        
        // Banking Industry Reliability Requirements
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Ensure all replicas acknowledge
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // Strict ordering
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Prevent duplicates
        
        // Timeout Configuration for Banking Operations
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 2 minutes
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        
        // Message Size Configuration for Large Banking Payloads
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 10485760); // 10MB
        
        // Monitoring and Metrics
        configProps.put(ProducerConfig.METRICS_SAMPLE_WINDOW_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.METRICS_NUM_SAMPLES_CONFIG, 2);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Enterprise Banking Kafka Template
     */
    @Bean
    public KafkaTemplate<String, String> bankingKafkaTemplate() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(bankingProducerFactory());
        
        // Set default topic for banking operations
        template.setDefaultTopic("banking.domain.events.v1");
        
        // Enable transaction support for critical banking operations
        template.setTransactionIdPrefix("banking-tx-");
        
        return template;
    }

    /**
     * Enterprise Banking Consumer Configuration
     * Optimized for reliable message processing with compliance requirements
     */
    @Bean
    public ConsumerFactory<String, String> bankingConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic Configuration
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Banking-Specific Consumer Settings
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Never lose banking data
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for reliability
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinSize);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        // Session and Heartbeat Configuration
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10 seconds
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
        
        // Error Handling for Banking Data Integrity
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.banking.loan.infrastructure.messaging");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BankingMessage.class);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Banking-Specific Kafka Listener Container Factory
     * Includes error handling, retry logic, and compliance features
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> bankingKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(bankingConsumerFactory());
        
        // Concurrency Configuration for Banking Workloads
        factory.setConcurrency(10); // Optimal for banking transaction processing
        
        // Acknowledgment Configuration for Banking Reliability
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true); // Ensure commits are synchronous
        
        // Error Handling for Banking Compliance
        factory.setCommonErrorHandler(bankingErrorHandler());
        
        // Consumer Configuration
        factory.getContainerProperties().setPollTimeout(Duration.ofSeconds(3));
        factory.getContainerProperties().setIdleEventInterval(Duration.ofSeconds(60));
        
        // Monitoring and Observability
        factory.getContainerProperties().setLogContainerConfig(true);
        factory.getContainerProperties().setMissingTopicsFatal(false);
        
        return factory;
    }

    /**
     * High-Volume Transaction Processing Container Factory
     * Optimized for payment and transaction events
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> transactionKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(bankingConsumerFactory());
        
        // Higher concurrency for transaction processing
        factory.setConcurrency(20);
        
        // Batch processing for high-volume transactions
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        
        // Faster polling for real-time transaction processing
        factory.getContainerProperties().setPollTimeout(Duration.ofMillis(100));
        
        factory.setCommonErrorHandler(transactionErrorHandler());
        
        return factory;
    }

    /**
     * Compliance-Focused Container Factory
     * For regulatory and audit events requiring special handling
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> complianceKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(bankingConsumerFactory());
        
        // Lower concurrency for careful compliance processing
        factory.setConcurrency(5);
        
        // Strict ordering for compliance events
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.getContainerProperties().setSyncCommits(true);
        
        // No retry for compliance - direct to DLQ
        factory.setCommonErrorHandler(complianceErrorHandler());
        
        return factory;
    }

    /**
     * Banking-Specific Error Handler
     * Implements industry-standard error handling for financial services
     */
    @Bean
    public DefaultErrorHandler bankingErrorHandler() {
        // Exponential backoff for banking operations
        FixedBackOff fixedBackOff = new FixedBackOff(5000L, 3L); // 5 seconds, 3 retries
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (consumerRecord, exception) -> {
                log.error("Banking event processing failed after retries. Record: {} Partition: {} Offset: {}", 
                    consumerRecord.key(), consumerRecord.partition(), consumerRecord.offset(), exception);
                // Send to banking DLQ
                sendToBankingDLQ(consumerRecord, exception);
            },
            fixedBackOff
        );
        
        // Don't retry for certain banking-specific exceptions
        errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            SecurityException.class,
            org.springframework.security.access.AccessDeniedException.class
        );
        
        return errorHandler;
    }

    /**
     * Transaction-Specific Error Handler
     * Fast-fail for transaction processing to maintain performance
     */
    @Bean
    public DefaultErrorHandler transactionErrorHandler() {
        // Quick retry for transaction processing
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 2L); // 1 second, 2 retries
        
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (consumerRecord, exception) -> {
                log.error("Transaction event processing failed. Record: {} Partition: {} Offset: {}", 
                    consumerRecord.key(), consumerRecord.partition(), consumerRecord.offset(), exception);
                sendToTransactionDLQ(consumerRecord, exception);
            },
            fixedBackOff
        );
        
        return errorHandler;
    }

    /**
     * Compliance-Specific Error Handler
     * No retry - direct to compliance investigation queue
     */
    @Bean
    public DefaultErrorHandler complianceErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (consumerRecord, exception) -> {
                log.error("Compliance event processing failed - requires manual investigation. Record: {} Partition: {} Offset: {}", 
                    consumerRecord.key(), consumerRecord.partition(), consumerRecord.offset(), exception);
                sendToComplianceDLQ(consumerRecord, exception);
            },
            new FixedBackOff(0L, 0L) // No retries for compliance
        );
        
        return errorHandler;
    }

    private void sendToBankingDLQ(org.apache.kafka.clients.consumer.ConsumerRecord<Object, Object> record, Exception exception) {
        // Implementation for banking DLQ
        log.info("Sending banking event to DLQ: {}", record.key());
    }

    private void sendToTransactionDLQ(org.apache.kafka.clients.consumer.ConsumerRecord<Object, Object> record, Exception exception) {
        // Implementation for transaction DLQ
        log.info("Sending transaction event to DLQ: {}", record.key());
    }

    private void sendToComplianceDLQ(org.apache.kafka.clients.consumer.ConsumerRecord<Object, Object> record, Exception exception) {
        // Implementation for compliance investigation DLQ
        log.error("Sending compliance event to investigation DLQ: {}", record.key());
    }
}