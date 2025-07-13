package com.amanahfi.events.publisher;

import com.amanahfi.events.domain.DomainEvent;
import com.amanahfi.events.domain.EventMetadata;
import com.amanahfi.events.domain.IslamicBankingEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Domain Event Publisher
 * 
 * Tests event publishing for Islamic banking compliance:
 * - Customer lifecycle events (KYC, activation, suspension)
 * - Payment events (CBDC settlement, cross-currency)
 * - Murabaha contract events (creation, approval, payment)
 * - Compliance events (AML checks, Sharia validation)
 * - Audit events for regulatory reporting
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Domain Event Publisher Tests")
class DomainEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private EventMetadataEnricher metadataEnricher;

    private DomainEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new DomainEventPublisher(kafkaTemplate, metadataEnricher);
    }

    @Test
    @DisplayName("Should publish customer registration event for Islamic banking onboarding")
    void shouldPublishCustomerRegistrationEventForIslamicBankingOnboarding() {
        // Given
        CustomerRegisteredEvent event = new CustomerRegisteredEvent(
            "CUST-12345678",
            "784-1990-1234567-1", // UAE Emirates ID
            "Ahmed Al-Rashid",
            "ahmed.alrashid@email.ae",
            "+971501234567",
            Instant.now()
        );

        EventMetadata enrichedMetadata = EventMetadata.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("CustomerRegistered")
            .aggregateId("CUST-12345678")
            .aggregateType("Customer")
            .version(1L)
            .timestamp(Instant.now())
            .correlationId(UUID.randomUUID().toString())
            .islamicBankingCompliant(true)
            .regulatoryCompliance("CBUAE,VARA")
            .build();

        when(metadataEnricher.enrich(event)).thenReturn(enrichedMetadata);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        RecordMetadata recordMetadata = new RecordMetadata(
            new TopicPartition("amanahfi.customers", 0), 0, 0, 0, 0L, 0, 0);
        SendResult<String, Object> sendResult = new SendResult<>(
            new ProducerRecord<>("amanahfi.customers", "CUST-12345678", event), recordMetadata);
        future.complete(sendResult);
        
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When
        eventPublisher.publish(event).join();

        // Then
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = 
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        
        ProducerRecord<String, Object> sentRecord = recordCaptor.getValue();
        assertThat(sentRecord.topic()).isEqualTo("amanahfi.customers");
        assertThat(sentRecord.key()).isEqualTo("CUST-12345678");
        assertThat(sentRecord.value()).isInstanceOf(CustomerRegisteredEvent.class);
        
        // Verify Islamic banking headers
        assertThat(sentRecord.headers().lastHeader("islamic-banking")).isNotNull();
        assertThat(new String(sentRecord.headers().lastHeader("islamic-banking").value())).isEqualTo("true");
        assertThat(sentRecord.headers().lastHeader("sharia-compliant")).isNotNull();
        assertThat(new String(sentRecord.headers().lastHeader("sharia-compliant").value())).isEqualTo("true");
    }

    @Test
    @DisplayName("Should publish CBDC payment settlement event with UAE compliance")
    void shouldPublishCbdcPaymentSettlementEventWithUaeCompliance() {
        // Given
        CbdcPaymentSettledEvent event = new CbdcPaymentSettledEvent(
            "PAY-87654321",
            "ACC-12345678", // From account
            "ACC-87654321", // To account
            "1500.00",
            "AED",
            "UAE-CBDC",
            Instant.now(),
            3 // Settlement time in seconds - meets ≤5 second requirement
        );

        EventMetadata enrichedMetadata = EventMetadata.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("CbdcPaymentSettled")
            .aggregateId("PAY-87654321")
            .aggregateType("Payment")
            .version(1L)
            .timestamp(Instant.now())
            .correlationId(UUID.randomUUID().toString())
            .islamicBankingCompliant(true)
            .cbdcCompliant(true)
            .regulatoryCompliance("CBUAE")
            .build();

        when(metadataEnricher.enrich(event)).thenReturn(enrichedMetadata);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When
        eventPublisher.publish(event).join();

        // Then
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = 
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        
        ProducerRecord<String, Object> sentRecord = recordCaptor.getValue();
        assertThat(sentRecord.topic()).isEqualTo("amanahfi.payments");
        assertThat(sentRecord.key()).isEqualTo("PAY-87654321");
        
        // Verify CBDC compliance headers
        assertThat(sentRecord.headers().lastHeader("cbdc-compliant")).isNotNull();
        assertThat(new String(sentRecord.headers().lastHeader("cbdc-compliant").value())).isEqualTo("true");
        assertThat(sentRecord.headers().lastHeader("settlement-time")).isNotNull();
        assertThat(new String(sentRecord.headers().lastHeader("settlement-time").value())).isEqualTo("3");
    }

    @Test
    @DisplayName("Should publish Murabaha contract approval event with Sharia compliance")
    void shouldPublishMurabahaContractApprovalEventWithShariaCompliance() {
        // Given
        MurabahaContractApprovedEvent event = new MurabahaContractApprovedEvent(
            "MUR-11111111",
            "CUST-12345678",
            "150000.00", // Asset cost
            "187500.00", // Total amount (25% profit)
            "AED",
            36, // Term months
            "SCHOLAR-001", // Sharia board member
            "Contract complies with Islamic finance principles",
            Instant.now()
        );

        EventMetadata enrichedMetadata = EventMetadata.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("MurabahaContractApproved")
            .aggregateId("MUR-11111111")
            .aggregateType("MurabahaContract")
            .version(1L)
            .timestamp(Instant.now())
            .correlationId(UUID.randomUUID().toString())
            .islamicBankingCompliant(true)
            .shariahApproved(true)
            .regulatoryCompliance("HSA")
            .build();

        when(metadataEnricher.enrich(event)).thenReturn(enrichedMetadata);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When
        eventPublisher.publish(event).join();

        // Then
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = 
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        
        ProducerRecord<String, Object> sentRecord = recordCaptor.getValue();
        assertThat(sentRecord.topic()).isEqualTo("amanahfi.murabaha");
        assertThat(sentRecord.key()).isEqualTo("MUR-11111111");
        
        // Verify Sharia compliance headers
        assertThat(sentRecord.headers().lastHeader("sharia-approved")).isNotNull();
        assertThat(new String(sentRecord.headers().lastHeader("sharia-approved").value())).isEqualTo("true");
        assertThat(sentRecord.headers().lastHeader("islamic-product")).isNotNull();
        assertThat(new String(sentRecord.headers().lastHeader("islamic-product").value())).isEqualTo("MURABAHA");
    }

    @Test
    @DisplayName("Should publish compliance check event for AML monitoring")
    void shouldPublishComplianceCheckEventForAmlMonitoring() {
        // Given
        ComplianceCheckCreatedEvent event = new ComplianceCheckCreatedEvent(
            "CHK-99999999",
            "CUST-12345678",
            "AML",
            "CUSTOMER_ONBOARDING",
            "Automated KYC verification for new Islamic banking customer",
            Instant.now()
        );

        EventMetadata enrichedMetadata = EventMetadata.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("ComplianceCheckCreated")
            .aggregateId("CHK-99999999")
            .aggregateType("ComplianceCheck")
            .version(1L)
            .timestamp(Instant.now())
            .correlationId(UUID.randomUUID().toString())
            .regulatoryCompliance("CBUAE,VARA")
            .complianceType("AML")
            .build();

        when(metadataEnricher.enrich(event)).thenReturn(enrichedMetadata);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When
        eventPublisher.publish(event).join();

        // Then
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = 
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        
        ProducerRecord<String, Object> sentRecord = recordCaptor.getValue();
        assertThat(sentRecord.topic()).isEqualTo("amanahfi.compliance");
        assertThat(sentRecord.key()).isEqualTo("CHK-99999999");
        
        // Verify compliance headers
        assertThat(sentRecord.headers().lastHeader("compliance-type")).isNotNull();
        assertThat(new String(sentRecord.headers().lastHeader("compliance-type").value())).isEqualTo("AML");
        assertThat(sentRecord.headers().lastHeader("regulatory-compliance")).isNotNull();
    }

    @Test
    @DisplayName("Should handle publishing failure with proper error handling")
    void shouldHandlePublishingFailureWithProperErrorHandling() {
        // Given
        CustomerRegisteredEvent event = new CustomerRegisteredEvent(
            "CUST-12345678", "784-1990-1234567-1", "Ahmed Al-Rashid",
            "ahmed@email.ae", "+971501234567", Instant.now()
        );

        when(metadataEnricher.enrich(event)).thenReturn(mock(EventMetadata.class));
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka broker unavailable"));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When & Then
        assertThatThrownBy(() -> eventPublisher.publish(event).join())
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("Kafka broker unavailable");
    }

    @Test
    @DisplayName("Should batch publish multiple events efficiently")
    void shouldBatchPublishMultipleEventsEfficiently() {
        // Given
        CustomerRegisteredEvent event1 = new CustomerRegisteredEvent(
            "CUST-12345678", "784-1990-1234567-1", "Ahmed Al-Rashid",
            "ahmed@email.ae", "+971501234567", Instant.now()
        );
        
        CbdcPaymentSettledEvent event2 = new CbdcPaymentSettledEvent(
            "PAY-87654321", "ACC-12345678", "ACC-87654321",
            "1500.00", "AED", "UAE-CBDC", Instant.now(), 3
        );

        when(metadataEnricher.enrich(any())).thenReturn(mock(EventMetadata.class));
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When
        eventPublisher.publishBatch(java.util.List.of(event1, event2)).join();

        // Then
        verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));
    }

    @Test
    @DisplayName("Should add audit trail for regulatory compliance")
    void shouldAddAuditTrailForRegulatoryCompliance() {
        // Given
        IslamicBankingEvent event = new CustomerRegisteredEvent(
            "CUST-12345678", "784-1990-1234567-1", "Ahmed Al-Rashid",
            "ahmed@email.ae", "+971501234567", Instant.now()
        );

        EventMetadata enrichedMetadata = EventMetadata.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("CustomerRegistered")
            .aggregateId("CUST-12345678")
            .aggregateType("Customer")
            .version(1L)
            .timestamp(Instant.now())
            .correlationId(UUID.randomUUID().toString())
            .islamicBankingCompliant(true)
            .auditRequired(true)
            .regulatoryCompliance("CBUAE,VARA")
            .build();

        when(metadataEnricher.enrich(event)).thenReturn(enrichedMetadata);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.complete(mock(SendResult.class));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(future);

        // When
        eventPublisher.publish(event).join();

        // Then
        ArgumentCaptor<ProducerRecord<String, Object>> recordCaptor = 
            ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(recordCaptor.capture());
        
        ProducerRecord<String, Object> sentRecord = recordCaptor.getValue();
        
        // Verify audit trail headers
        assertThat(sentRecord.headers().lastHeader("audit-required")).isNotNull();
        assertThat(new String(sentRecord.headers().lastHeader("audit-required").value())).isEqualTo("true");
        assertThat(sentRecord.headers().lastHeader("event-id")).isNotNull();
        assertThat(sentRecord.headers().lastHeader("correlation-id")).isNotNull();
    }

    // Helper event classes for testing
    static class CustomerRegisteredEvent implements IslamicBankingEvent {
        private final String customerId;
        private final String emiratesId;
        private final String fullName;
        private final String email;
        private final String mobileNumber;
        private final Instant timestamp;

        public CustomerRegisteredEvent(String customerId, String emiratesId, String fullName,
                                     String email, String mobileNumber, Instant timestamp) {
            this.customerId = customerId;
            this.emiratesId = emiratesId;
            this.fullName = fullName;
            this.email = email;
            this.mobileNumber = mobileNumber;
            this.timestamp = timestamp;
        }

        @Override
        public String getAggregateId() { return customerId; }
        @Override
        public String getEventType() { return "CustomerRegistered"; }
        @Override
        public Instant getTimestamp() { return timestamp; }
        @Override
        public boolean isIslamicBankingCompliant() { return true; }

        // Getters
        public String getCustomerId() { return customerId; }
        public String getEmiratesId() { return emiratesId; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getMobileNumber() { return mobileNumber; }
    }

    static class CbdcPaymentSettledEvent implements IslamicBankingEvent {
        private final String paymentId;
        private final String fromAccountId;
        private final String toAccountId;
        private final String amount;
        private final String currency;
        private final String cbdcType;
        private final Instant settlementTime;
        private final int settlementSeconds;

        public CbdcPaymentSettledEvent(String paymentId, String fromAccountId, String toAccountId,
                                     String amount, String currency, String cbdcType,
                                     Instant settlementTime, int settlementSeconds) {
            this.paymentId = paymentId;
            this.fromAccountId = fromAccountId;
            this.toAccountId = toAccountId;
            this.amount = amount;
            this.currency = currency;
            this.cbdcType = cbdcType;
            this.settlementTime = settlementTime;
            this.settlementSeconds = settlementSeconds;
        }

        @Override
        public String getAggregateId() { return paymentId; }
        @Override
        public String getEventType() { return "CbdcPaymentSettled"; }
        @Override
        public Instant getTimestamp() { return settlementTime; }
        @Override
        public boolean isIslamicBankingCompliant() { return true; }

        // Getters
        public String getPaymentId() { return paymentId; }
        public String getFromAccountId() { return fromAccountId; }
        public String getToAccountId() { return toAccountId; }
        public String getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getCbdcType() { return cbdcType; }
        public int getSettlementSeconds() { return settlementSeconds; }
    }

    static class MurabahaContractApprovedEvent implements IslamicBankingEvent {
        private final String contractId;
        private final String customerId;
        private final String assetCost;
        private final String totalAmount;
        private final String currency;
        private final int termMonths;
        private final String shariahBoardMemberId;
        private final String approvalNotes;
        private final Instant approvalTime;

        public MurabahaContractApprovedEvent(String contractId, String customerId, String assetCost,
                                           String totalAmount, String currency, int termMonths,
                                           String shariahBoardMemberId, String approvalNotes, Instant approvalTime) {
            this.contractId = contractId;
            this.customerId = customerId;
            this.assetCost = assetCost;
            this.totalAmount = totalAmount;
            this.currency = currency;
            this.termMonths = termMonths;
            this.shariahBoardMemberId = shariahBoardMemberId;
            this.approvalNotes = approvalNotes;
            this.approvalTime = approvalTime;
        }

        @Override
        public String getAggregateId() { return contractId; }
        @Override
        public String getEventType() { return "MurabahaContractApproved"; }
        @Override
        public Instant getTimestamp() { return approvalTime; }
        @Override
        public boolean isIslamicBankingCompliant() { return true; }

        // Getters
        public String getContractId() { return contractId; }
        public String getCustomerId() { return customerId; }
        public String getAssetCost() { return assetCost; }
        public String getTotalAmount() { return totalAmount; }
        public String getCurrency() { return currency; }
        public int getTermMonths() { return termMonths; }
        public String getShariahBoardMemberId() { return shariahBoardMemberId; }
        public String getApprovalNotes() { return approvalNotes; }
    }

    static class ComplianceCheckCreatedEvent implements DomainEvent {
        private final String checkId;
        private final String entityId;
        private final String complianceType;
        private final String checkType;
        private final String reason;
        private final Instant timestamp;

        public ComplianceCheckCreatedEvent(String checkId, String entityId, String complianceType,
                                         String checkType, String reason, Instant timestamp) {
            this.checkId = checkId;
            this.entityId = entityId;
            this.complianceType = complianceType;
            this.checkType = checkType;
            this.reason = reason;
            this.timestamp = timestamp;
        }

        @Override
        public String getAggregateId() { return checkId; }
        @Override
        public String getEventType() { return "ComplianceCheckCreated"; }
        @Override
        public Instant getTimestamp() { return timestamp; }

        // Getters
        public String getCheckId() { return checkId; }
        public String getEntityId() { return entityId; }
        public String getComplianceType() { return complianceType; }
        public String getCheckType() { return checkType; }
        public String getReason() { return reason; }
    }
}