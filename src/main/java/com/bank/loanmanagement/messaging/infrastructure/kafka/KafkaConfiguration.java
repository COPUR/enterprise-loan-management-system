package com.bank.loanmanagement.messaging.infrastructure.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration for Event-Driven Architecture
 * Configures Kafka producers, consumers, and topics for BIAN-compliant event processing
 * Includes security configurations for FAPI compliance and Berlin Group requirements
 * Provides separate configurations for different event types and security levels
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:banking-loan-management}")
    private String defaultGroupId;

    @Value("${spring.kafka.security.protocol:PLAINTEXT}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism:}")
    private String saslMechanism;

    @Value("${spring.kafka.properties.sasl.jaas.config:}")
    private String saslJaasConfig;

    /**
     * Kafka Producer Configuration for Event Publishing
     * Optimized for reliability and performance with FAPI security
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic connection configuration
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Reliability configuration for financial data
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Wait for all replicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 2 minutes
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
        
        // Performance optimization
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
        
        // Idempotence for exactly-once semantics
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        
        // Security configuration
        if (!"PLAINTEXT".equals(securityProtocol)) {
            configProps.put("security.protocol", securityProtocol);
            if (saslMechanism != null && !saslMechanism.isEmpty()) {
                configProps.put("sasl.mechanism", saslMechanism);
                configProps.put("sasl.jaas.config", saslJaasConfig);
            }
        }
        
        log.info("Configured Kafka producer with security protocol: {}", securityProtocol);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory());
        
        // Enable producer transaction support for SAGA consistency
        template.setTransactionIdPrefix("banking-tx-");
        
        return template;
    }

    /**
     * Default Kafka Consumer Configuration
     * Used for standard event processing with retry handling
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic connection configuration
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Error handling configuration
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        
        // Consumer behavior configuration
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual acknowledgment
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
        
        // Performance configuration
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        // Security configuration
        if (!"PLAINTEXT".equals(securityProtocol)) {
            configProps.put("security.protocol", securityProtocol);
            if (saslMechanism != null && !saslMechanism.isEmpty()) {
                configProps.put("sasl.mechanism", saslMechanism);
                configProps.put("sasl.jaas.config", saslJaasConfig);
            }
        }
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // Number of consumer threads
        
        // Acknowledgment configuration for manual offset management
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Error handling configuration
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
            new org.springframework.util.backoff.FixedBackOff(1000L, 3L) // 1 second delay, 3 retries
        ));
        
        log.info("Configured default Kafka listener container factory with concurrency: 3");
        return factory;
    }

    /**
     * Secure Kafka Consumer Configuration for sensitive events
     * Enhanced security settings for payment and customer data
     */
    @Bean
    public ConsumerFactory<String, String> secureConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>(consumerFactory().getConfigurationProperties());
        
        // Enhanced security settings
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId + "-secure");
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 20000); // Shorter timeout for security
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50); // Smaller batches for secure processing
        
        // Isolation level for transactional messages
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> secureKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(secureConsumerFactory());
        factory.setConcurrency(2); // Reduced concurrency for secure processing
        
        // Manual acknowledgment for secure events
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Conservative error handling for secure events
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
            new org.springframework.util.backoff.ExponentialBackOff(1000L, 2.0) // Exponential backoff
        ));
        
        log.info("Configured secure Kafka listener container factory with concurrency: 2");
        return factory;
    }

    /**
     * SAGA-specific Kafka Consumer Configuration
     * Optimized for SAGA coordination and state management
     */
    @Bean
    public ConsumerFactory<String, String> sagaConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>(consumerFactory().getConfigurationProperties());
        
        // SAGA-specific settings
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId + "-saga");
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); // Small batches for SAGA coordination
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000); // Faster failure detection
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 5000);
        
        // Enable compacted topic consumption for SAGA state
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> sagaKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(sagaConsumerFactory());
        factory.setConcurrency(1); // Single thread for SAGA coordination to maintain order
        
        // Immediate acknowledgment for SAGA events
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // No retries for SAGA coordination - handle failures explicitly
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
            new org.springframework.util.backoff.FixedBackOff(0L, 0L) // No retries
        ));
        
        log.info("Configured SAGA Kafka listener container factory with concurrency: 1");
        return factory;
    }

    /**
     * Kafka Admin Configuration for topic management
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        if (!"PLAINTEXT".equals(securityProtocol)) {
            configs.put("security.protocol", securityProtocol);
            if (saslMechanism != null && !saslMechanism.isEmpty()) {
                configs.put("sasl.mechanism", saslMechanism);
                configs.put("sasl.jaas.config", saslJaasConfig);
            }
        }
        
        return new KafkaAdmin(configs);
    }

    /**
     * BIAN Service Domain Topic Definitions
     * Creates topics for each service domain with appropriate configurations
     */
    @Bean
    public NewTopic consumerLoanCommandsTopic() {
        return TopicBuilder.name("banking.consumer-loan.commands")
                .partitions(KafkaTopicResolver.TopicConfiguration.DEFAULT_PARTITIONS)
                .replicas(KafkaTopicResolver.TopicConfiguration.DEFAULT_REPLICATION_FACTOR)
                .configs(KafkaTopicResolver.TopicConfiguration.DEFAULT_TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic consumerLoanEventsTopic() {
        return TopicBuilder.name("banking.consumer-loan.events")
                .partitions(KafkaTopicResolver.TopicConfiguration.DEFAULT_PARTITIONS)
                .replicas(KafkaTopicResolver.TopicConfiguration.DEFAULT_REPLICATION_FACTOR)
                .configs(KafkaTopicResolver.TopicConfiguration.DEFAULT_TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic paymentInitiationCommandsTopic() {
        return TopicBuilder.name("banking.payment-initiation.commands")
                .partitions(KafkaTopicResolver.TopicConfiguration.DEFAULT_PARTITIONS)
                .replicas(KafkaTopicResolver.TopicConfiguration.DEFAULT_REPLICATION_FACTOR)
                .configs(KafkaTopicResolver.TopicConfiguration.DEFAULT_TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic paymentInitiationSecureTopic() {
        return TopicBuilder.name("banking.payment-initiation.secure")
                .partitions(KafkaTopicResolver.TopicConfiguration.DEFAULT_PARTITIONS)
                .replicas(KafkaTopicResolver.TopicConfiguration.DEFAULT_REPLICATION_FACTOR)
                .configs(KafkaTopicResolver.TopicConfiguration.SECURE_TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic customerManagementSecureTopic() {
        return TopicBuilder.name("banking.customer-management.secure")
                .partitions(KafkaTopicResolver.TopicConfiguration.DEFAULT_PARTITIONS)
                .replicas(KafkaTopicResolver.TopicConfiguration.DEFAULT_REPLICATION_FACTOR)
                .configs(KafkaTopicResolver.TopicConfiguration.SECURE_TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic loanOriginationSagaTopic() {
        return TopicBuilder.name("banking.loan-origination.saga.loanoriginationsaga")
                .partitions(6) // Smaller partition count for SAGA coordination
                .replicas(KafkaTopicResolver.TopicConfiguration.DEFAULT_REPLICATION_FACTOR)
                .configs(KafkaTopicResolver.TopicConfiguration.SAGA_TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic creditRiskAssessmentTopic() {
        return TopicBuilder.name("banking.credit-risk-assessment.events")
                .partitions(KafkaTopicResolver.TopicConfiguration.DEFAULT_PARTITIONS)
                .replicas(KafkaTopicResolver.TopicConfiguration.DEFAULT_REPLICATION_FACTOR)
                .configs(KafkaTopicResolver.TopicConfiguration.DEFAULT_TOPIC_CONFIG)
                .build();
    }

    @Bean
    public NewTopic accountInformationTopic() {
        return TopicBuilder.name("banking.account-information.events")
                .partitions(KafkaTopicResolver.TopicConfiguration.DEFAULT_PARTITIONS)
                .replicas(KafkaTopicResolver.TopicConfiguration.DEFAULT_REPLICATION_FACTOR)
                .configs(KafkaTopicResolver.TopicConfiguration.DEFAULT_TOPIC_CONFIG)
                .build();
    }
}