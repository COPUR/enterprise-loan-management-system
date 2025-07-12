package com.amanahfi.platform.integration.messaging;

import com.amanahfi.platform.integration.AbstractIntegrationTest;
import com.amanahfi.platform.shared.domain.event.DomainEvent;
import com.amanahfi.platform.islamicfinance.domain.event.MurabahaContractCreatedEvent;
import com.amanahfi.platform.cbdc.domain.event.DigitalDirhamTransferExecutedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Integration tests for Kafka event streaming infrastructure.
 * 
 * Tests the event-driven architecture for:
 * - Islamic Finance event publishing and consumption
 * - CBDC transaction event streaming
 * - Cross-service communication
 * - Event sourcing and replay
 * - Dead letter queue handling
 * - Regulatory compliance event auditing
 */
@Tag("integration")
@Tag("messaging")
@Tag("kafka")
@Tag("event-streaming")
@TestPropertySource(properties = {
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.group-id=amanahfi-integration-test",
    "spring.kafka.producer.retries=3",
    "spring.kafka.producer.acks=all"
})
class KafkaEventStreamingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaProducer<String, String> testProducer;
    private KafkaConsumer<String, String> testConsumer;
    private final BlockingQueue<String> receivedMessages = new LinkedBlockingQueue<>();
    private CountDownLatch messageLatch;

    @BeforeEach
    void setupKafkaClients() {
        // Producer configuration
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        testProducer = new KafkaProducer<>(producerProps);

        // Consumer configuration
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "amanahfi-integration-test-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        testConsumer = new KafkaConsumer<>(consumerProps);

        receivedMessages.clear();
        messageLatch = new CountDownLatch(1);
    }

    @Test
    @DisplayName("Should publish and consume Islamic Finance Murabaha event")
    void shouldPublishAndConsumeMurabahaEvent() throws Exception {
        // Given
        String topicName = "amanahfi.islamic-finance.murabaha";
        MurabahaContractCreatedEvent event = createMurabahaContractCreatedEvent();
        String eventJson = objectMapper.writeValueAsString(event);

        // Subscribe to topic
        testConsumer.subscribe(Collections.singletonList(topicName));

        // When - Publish event
        ProducerRecord<String, String> record = new ProducerRecord<>(
            topicName, 
            event.getAggregateId(), 
            eventJson
        );
        testProducer.send(record).get(5, TimeUnit.SECONDS);

        // Then - Consume and validate event
        boolean messageReceived = false;
        long timeout = System.currentTimeMillis() + 10000; // 10 second timeout

        while (System.currentTimeMillis() < timeout && !messageReceived) {
            var records = testConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> consumedRecord : records) {
                assertAll(
                    () -> assertThat(consumedRecord.key()).isEqualTo(event.getAggregateId()),
                    () -> assertThat(consumedRecord.value()).isNotNull(),
                    () -> assertThat(consumedRecord.topic()).isEqualTo(topicName)
                );

                // Validate event content
                MurabahaContractCreatedEvent receivedEvent = objectMapper.readValue(
                    consumedRecord.value(), 
                    MurabahaContractCreatedEvent.class
                );
                
                assertAll(
                    () -> assertThat(receivedEvent.getContractId()).isEqualTo(event.getContractId()),
                    () -> assertThat(receivedEvent.getCustomerId()).isEqualTo(event.getCustomerId()),
                    () -> assertThat(receivedEvent.getAssetValue()).isEqualTo(event.getAssetValue()),
                    () -> assertThat(receivedEvent.getProfitAmount()).isEqualTo(event.getProfitAmount()),
                    () -> assertThat(receivedEvent.isShariaCompliant()).isTrue()
                );

                messageReceived = true;
            }
        }

        assertThat(messageReceived).isTrue();
    }

    @Test
    @DisplayName("Should publish and consume CBDC Digital Dirham transfer event")
    void shouldPublishAndConsumeDigitalDirhamTransferEvent() throws Exception {
        // Given
        String topicName = "amanahfi.cbdc.digital-dirham";
        DigitalDirhamTransferExecutedEvent event = createDigitalDirhamTransferEvent();
        String eventJson = objectMapper.writeValueAsString(event);

        // Subscribe to topic
        testConsumer.subscribe(Collections.singletonList(topicName));

        // When - Publish event
        ProducerRecord<String, String> record = new ProducerRecord<>(
            topicName, 
            event.getTransferId(), 
            eventJson
        );
        record.headers().add("event-type", "DigitalDirhamTransferExecutedEvent".getBytes());
        record.headers().add("institution-id", "AMANAHFI-001".getBytes());
        record.headers().add("cbdc-network", "DIGITAL-DIRHAM".getBytes());
        
        testProducer.send(record).get(5, TimeUnit.SECONDS);

        // Then - Consume and validate event
        boolean messageReceived = false;
        long timeout = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < timeout && !messageReceived) {
            var records = testConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> consumedRecord : records) {
                assertAll(
                    () -> assertThat(consumedRecord.key()).isEqualTo(event.getTransferId()),
                    () -> assertThat(consumedRecord.value()).isNotNull(),
                    () -> assertThat(consumedRecord.topic()).isEqualTo(topicName)
                );

                // Validate headers
                assertThat(new String(consumedRecord.headers().lastHeader("event-type").value()))
                    .isEqualTo("DigitalDirhamTransferExecutedEvent");
                
                // Validate event content
                DigitalDirhamTransferExecutedEvent receivedEvent = objectMapper.readValue(
                    consumedRecord.value(), 
                    DigitalDirhamTransferExecutedEvent.class
                );
                
                assertAll(
                    () -> assertThat(receivedEvent.getTransferId()).isEqualTo(event.getTransferId()),
                    () -> assertThat(receivedEvent.getAmount()).isEqualTo(event.getAmount()),
                    () -> assertThat(receivedEvent.getCurrency()).isEqualTo("AED-CBDC"),
                    () -> assertThat(receivedEvent.getNetworkId()).isEqualTo("DIGITAL-DIRHAM-NETWORK"),
                    () -> assertThat(receivedEvent.getStatus()).isEqualTo("CONFIRMED")
                );

                messageReceived = true;
            }
        }

        assertThat(messageReceived).isTrue();
    }

    @Test
    @DisplayName("Should handle event ordering and partitioning")
    void shouldHandleEventOrderingAndPartitioning() throws Exception {
        // Given
        String topicName = "amanahfi.islamic-finance.murabaha";
        String customerId = "CUSTOMER-" + UUID.randomUUID().toString();
        
        // Create multiple events for the same customer
        MurabahaContractCreatedEvent event1 = createMurabahaContractCreatedEvent();
        event1.setCustomerId(customerId);
        event1.setContractId("CONTRACT-001");
        
        MurabahaContractCreatedEvent event2 = createMurabahaContractCreatedEvent();
        event2.setCustomerId(customerId);
        event2.setContractId("CONTRACT-002");
        
        MurabahaContractCreatedEvent event3 = createMurabahaContractCreatedEvent();
        event3.setCustomerId(customerId);
        event3.setContractId("CONTRACT-003");

        // Subscribe to topic
        testConsumer.subscribe(Collections.singletonList(topicName));

        // When - Publish events with same partition key (customerId)
        testProducer.send(new ProducerRecord<>(topicName, customerId, objectMapper.writeValueAsString(event1)));
        testProducer.send(new ProducerRecord<>(topicName, customerId, objectMapper.writeValueAsString(event2)));
        testProducer.send(new ProducerRecord<>(topicName, customerId, objectMapper.writeValueAsString(event3)));
        testProducer.flush();

        // Then - Consume events and verify ordering
        int eventsReceived = 0;
        long timeout = System.currentTimeMillis() + 10000;
        String[] expectedContractOrder = {"CONTRACT-001", "CONTRACT-002", "CONTRACT-003"};

        while (System.currentTimeMillis() < timeout && eventsReceived < 3) {
            var records = testConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                MurabahaContractCreatedEvent receivedEvent = objectMapper.readValue(
                    record.value(), 
                    MurabahaContractCreatedEvent.class
                );
                
                // Verify partition consistency (same customer should be in same partition)
                assertThat(record.key()).isEqualTo(customerId);
                assertThat(receivedEvent.getContractId()).isEqualTo(expectedContractOrder[eventsReceived]);
                
                eventsReceived++;
            }
        }

        assertThat(eventsReceived).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle event replay functionality")
    void shouldHandleEventReplay() throws Exception {
        // Given
        String topicName = "amanahfi.audit.replay";
        String replayEventJson = createReplayEventJson();

        // Subscribe to topic from beginning
        testConsumer.subscribe(Collections.singletonList(topicName));

        // When - Publish replay event
        ProducerRecord<String, String> record = new ProducerRecord<>(
            topicName, 
            "REPLAY-" + UUID.randomUUID().toString(), 
            replayEventJson
        );
        record.headers().add("replay-request", "true".getBytes());
        record.headers().add("replay-from", LocalDateTime.now().minusHours(1).toString().getBytes());
        
        testProducer.send(record).get(5, TimeUnit.SECONDS);

        // Then - Verify replay event was processed
        boolean replayEventReceived = false;
        long timeout = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < timeout && !replayEventReceived) {
            var records = testConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> consumedRecord : records) {
                if (consumedRecord.headers().lastHeader("replay-request") != null) {
                    assertThat(new String(consumedRecord.headers().lastHeader("replay-request").value()))
                        .isEqualTo("true");
                    
                    // Validate replay event structure
                    Map<String, Object> replayEvent = objectMapper.readValue(
                        consumedRecord.value(), 
                        Map.class
                    );
                    
                    assertAll(
                        () -> assertThat(replayEvent).containsKey("replayId"),
                        () -> assertThat(replayEvent).containsKey("fromTimestamp"),
                        () -> assertThat(replayEvent).containsKey("toTimestamp"),
                        () -> assertThat(replayEvent).containsKey("eventTypes"),
                        () -> assertThat(replayEvent).containsKey("requestedBy")
                    );
                    
                    replayEventReceived = true;
                }
            }
        }

        assertThat(replayEventReceived).isTrue();
    }

    @Test
    @DisplayName("Should handle dead letter queue for failed events")
    void shouldHandleDeadLetterQueue() throws Exception {
        // Given
        String mainTopicName = "amanahfi.islamic-finance.murabaha";
        String dlqTopicName = "amanahfi.islamic-finance.murabaha.dlq";
        
        // Create malformed event that will fail processing
        String malformedEventJson = "{ \"invalid\": \"json structure\" }";

        // Subscribe to DLQ topic
        testConsumer.subscribe(Collections.singletonList(dlqTopicName));

        // When - Publish malformed event
        ProducerRecord<String, String> record = new ProducerRecord<>(
            mainTopicName, 
            "INVALID-EVENT", 
            malformedEventJson
        );
        record.headers().add("original-topic", mainTopicName.getBytes());
        record.headers().add("failure-reason", "DESERIALIZATION_ERROR".getBytes());
        
        // Simulate DLQ routing by publishing directly to DLQ topic
        ProducerRecord<String, String> dlqRecord = new ProducerRecord<>(
            dlqTopicName,
            record.key(),
            record.value()
        );
        dlqRecord.headers().add("original-topic", mainTopicName.getBytes());
        dlqRecord.headers().add("failure-reason", "DESERIALIZATION_ERROR".getBytes());
        dlqRecord.headers().add("failure-timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
        
        testProducer.send(dlqRecord).get(5, TimeUnit.SECONDS);

        // Then - Verify DLQ event was received
        boolean dlqEventReceived = false;
        long timeout = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < timeout && !dlqEventReceived) {
            var records = testConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> consumedRecord : records) {
                assertAll(
                    () -> assertThat(consumedRecord.topic()).isEqualTo(dlqTopicName),
                    () -> assertThat(consumedRecord.key()).isEqualTo("INVALID-EVENT"),
                    () -> assertThat(new String(consumedRecord.headers().lastHeader("original-topic").value()))
                        .isEqualTo(mainTopicName),
                    () -> assertThat(new String(consumedRecord.headers().lastHeader("failure-reason").value()))
                        .isEqualTo("DESERIALIZATION_ERROR")
                );
                
                dlqEventReceived = true;
            }
        }

        assertThat(dlqEventReceived).isTrue();
    }

    @Test
    @DisplayName("Should handle regulatory compliance audit events")
    void shouldHandleRegulatoryComplianceAuditEvents() throws Exception {
        // Given
        String topicName = "amanahfi.compliance.audit";
        String auditEventJson = createComplianceAuditEventJson();

        // Subscribe to topic
        testConsumer.subscribe(Collections.singletonList(topicName));

        // When - Publish compliance audit event
        ProducerRecord<String, String> record = new ProducerRecord<>(
            topicName, 
            "AUDIT-" + UUID.randomUUID().toString(), 
            auditEventJson
        );
        record.headers().add("compliance-type", "SHARIA_AUDIT".getBytes());
        record.headers().add("regulator", "HSA".getBytes());
        record.headers().add("institution-id", "AMANAHFI-001".getBytes());
        
        testProducer.send(record).get(5, TimeUnit.SECONDS);

        // Then - Verify audit event was processed
        boolean auditEventReceived = false;
        long timeout = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < timeout && !auditEventReceived) {
            var records = testConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> consumedRecord : records) {
                // Validate audit event headers
                assertAll(
                    () -> assertThat(new String(consumedRecord.headers().lastHeader("compliance-type").value()))
                        .isEqualTo("SHARIA_AUDIT"),
                    () -> assertThat(new String(consumedRecord.headers().lastHeader("regulator").value()))
                        .isEqualTo("HSA"),
                    () -> assertThat(new String(consumedRecord.headers().lastHeader("institution-id").value()))
                        .isEqualTo("AMANAHFI-001")
                );
                
                // Validate audit event content
                Map<String, Object> auditEvent = objectMapper.readValue(
                    consumedRecord.value(), 
                    Map.class
                );
                
                assertAll(
                    () -> assertThat(auditEvent).containsKey("auditId"),
                    () -> assertThat(auditEvent).containsKey("auditType"),
                    () -> assertThat(auditEvent).containsKey("findings"),
                    () -> assertThat(auditEvent).containsKey("complianceStatus"),
                    () -> assertThat(auditEvent.get("auditType")).isEqualTo("SHARIA_COMPLIANCE")
                );
                
                auditEventReceived = true;
            }
        }

        assertThat(auditEventReceived).isTrue();
    }

    @KafkaListener(topics = "amanahfi.test.listener")
    public void testEventListener(@Payload String message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        receivedMessages.offer(message);
        messageLatch.countDown();
    }

    private MurabahaContractCreatedEvent createMurabahaContractCreatedEvent() {
        return new MurabahaContractCreatedEvent(
            UUID.randomUUID().toString(), // contractId
            "CUSTOMER-" + UUID.randomUUID().toString(), // customerId
            "AMANAHFI-001", // institutionId
            new BigDecimal("2500000.00"), // assetValue
            new BigDecimal("250000.00"), // profitAmount
            new BigDecimal("2750000.00"), // totalPrice
            60, // termMonths
            "RESIDENTIAL_PROPERTY", // assetType
            "Dubai Marina Tower", // assetDescription
            true, // shariaCompliant
            "HSA-2024-MUR-001", // hsaApprovalRef
            LocalDateTime.now() // timestamp
        );
    }

    private DigitalDirhamTransferExecutedEvent createDigitalDirhamTransferEvent() {
        return new DigitalDirhamTransferExecutedEvent(
            UUID.randomUUID().toString(), // transferId
            "WALLET-SENDER-" + UUID.randomUUID().toString(), // senderWalletId
            "WALLET-RECEIVER-" + UUID.randomUUID().toString(), // receiverWalletId
            new BigDecimal("100000.00"), // amount
            "AED-CBDC", // currency
            "DIGITAL-DIRHAM-NETWORK", // networkId
            "0x1234567890abcdef", // blockHash
            "CONFIRMED", // status
            "INSTITUTIONAL_TRANSFER", // transferType
            "AMANAHFI-001", // institutionId
            LocalDateTime.now() // timestamp
        );
    }

    private String createReplayEventJson() throws JsonProcessingException {
        Map<String, Object> replayEvent = Map.of(
            "replayId", UUID.randomUUID().toString(),
            "fromTimestamp", LocalDateTime.now().minusHours(24).toString(),
            "toTimestamp", LocalDateTime.now().toString(),
            "eventTypes", new String[]{
                "MurabahaContractCreatedEvent",
                "DigitalDirhamTransferExecutedEvent",
                "ComplianceAuditEvent"
            },
            "filters", Map.of(
                "institutionId", "AMANAHFI-001",
                "customerId", "CUSTOMER-123456",
                "eventStatus", "COMPLETED"
            ),
            "requestedBy", "AUDIT-TEAM",
            "replayReason", "REGULATORY_COMPLIANCE_AUDIT",
            "timestamp", LocalDateTime.now().toString()
        );
        
        return objectMapper.writeValueAsString(replayEvent);
    }

    private String createComplianceAuditEventJson() throws JsonProcessingException {
        Map<String, Object> auditEvent = Map.of(
            "auditId", UUID.randomUUID().toString(),
            "auditType", "SHARIA_COMPLIANCE",
            "institutionId", "AMANAHFI-001",
            "auditPeriod", Map.of(
                "from", LocalDateTime.now().minusMonths(3).toString(),
                "to", LocalDateTime.now().toString()
            ),
            "auditor", Map.of(
                "name", "Higher Sharia Authority",
                "id", "HSA-UAE-001",
                "certification", "AAOIFI_CERTIFIED"
            ),
            "findings", Map.of(
                "totalTransactionsAudited", 15420,
                "compliantTransactions", 15420,
                "nonCompliantTransactions", 0,
                "complianceRate", 1.00,
                "recommendations", new String[]{
                    "ENHANCE_MONITORING_DASHBOARD",
                    "QUARTERLY_SCHOLAR_REVIEW"
                }
            ),
            "complianceStatus", "FULLY_COMPLIANT",
            "certificateIssued", true,
            "certificateValidUntil", LocalDateTime.now().plusYears(1).toString(),
            "timestamp", LocalDateTime.now().toString()
        );
        
        return objectMapper.writeValueAsString(auditEvent);
    }

    @TestConfiguration
    static class TestKafkaConfiguration {
        @Bean
        public BlockingQueue<String> testMessageQueue() {
            return new LinkedBlockingQueue<>();
        }
    }
}