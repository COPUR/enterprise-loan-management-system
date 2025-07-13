package com.amanahfi.events.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for AmanahFi Islamic Banking Event Streaming
 * 
 * Configures:
 * - Producer settings for domain event publishing
 * - Consumer settings for event processing
 * - Topic configuration for Islamic banking contexts
 * - Serialization for compliance and audit
 */
@Configuration
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${amanahfi.kafka.replication-factor:1}")
    private int replicationFactor;

    @Value("${amanahfi.kafka.partitions:3}")
    private int partitions;

    /**
     * Producer configuration for publishing domain events
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Performance and reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5); // Small delay for batching
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        // Idempotence for exactly-once semantics (important for financial transactions)
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        // Compression for efficiency
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // Timeout settings
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        // Islamic banking compliance - add producer metadata
        configProps.put("client.id", "amanahfi-producer");
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for event publishing
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        
        // Set default topic for fallback
        template.setDefaultTopic("amanahfi.general");
        
        return template;
    }

    /**
     * Consumer configuration for event processing
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic configuration
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Consumer group settings
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "amanahfi-events");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Performance settings
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        // Reliability settings
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for financial accuracy
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        
        // JSON deserialization settings
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.amanahfi.*");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    // Topic Definitions for Islamic Banking Contexts

    /**
     * Customer events topic
     */
    @Bean
    public NewTopic customersTopic() {
        return TopicBuilder.name("amanahfi.customers")
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "604800000") // 7 days retention
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .build();
    }

    /**
     * Account events topic
     */
    @Bean
    public NewTopic accountsTopic() {
        return TopicBuilder.name("amanahfi.accounts")
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "2592000000") // 30 days retention
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .build();
    }

    /**
     * Payment events topic (including CBDC)
     */
    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name("amanahfi.payments")
            .partitions(partitions * 2) // More partitions for high throughput
            .replicas(replicationFactor)
            .config("retention.ms", "7776000000") // 90 days retention (regulatory requirement)
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .config("min.insync.replicas", "1") // Ensure durability for financial transactions
            .build();
    }

    /**
     * Murabaha contract events topic
     */
    @Bean
    public NewTopic murabahaTopic() {
        return TopicBuilder.name("amanahfi.murabaha")
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "31536000000") // 1 year retention (contract lifecycle)
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .build();
    }

    /**
     * Compliance events topic
     */
    @Bean
    public NewTopic complianceTopic() {
        return TopicBuilder.name("amanahfi.compliance")
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "157680000000") // 5 years retention (regulatory requirement)
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .config("min.insync.replicas", "1") // Ensure durability for compliance records
            .build();
    }

    /**
     * General events topic (fallback)
     */
    @Bean
    public NewTopic generalTopic() {
        return TopicBuilder.name("amanahfi.general")
            .partitions(partitions)
            .replicas(replicationFactor)
            .config("retention.ms", "604800000") // 7 days retention
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .build();
    }

    /**
     * Audit events topic for regulatory compliance
     */
    @Bean
    public NewTopic auditTopic() {
        return TopicBuilder.name("amanahfi.audit")
            .partitions(partitions)
            .replicas(Math.max(replicationFactor, 2)) // Higher replication for audit trails
            .config("retention.ms", "315360000000") // 10 years retention (regulatory requirement)
            .config("cleanup.policy", "delete")
            .config("compression.type", "snappy")
            .config("min.insync.replicas", "2") // Ensure high durability for audit records
            .build();
    }
}