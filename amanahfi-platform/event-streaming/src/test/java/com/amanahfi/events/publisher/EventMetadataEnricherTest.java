package com.amanahfi.events.publisher;

import com.amanahfi.events.domain.DomainEvent;
import com.amanahfi.events.domain.EventMetadata;
import com.amanahfi.events.domain.IslamicBankingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Test Suite for Event Metadata Enricher
 * 
 * Tests metadata enrichment for Islamic banking compliance
 */
@DisplayName("Event Metadata Enricher Tests")
class EventMetadataEnricherTest {

    private EventMetadataEnricher enricher;

    @BeforeEach
    void setUp() {
        enricher = new EventMetadataEnricher();
    }

    @Test
    @DisplayName("Should enrich customer event with Islamic banking metadata")
    void shouldEnrichCustomerEventWithIslamicBankingMetadata() {
        // Given
        TestCustomerRegisteredEvent event = new TestCustomerRegisteredEvent(
            "CUST-12345678", "CustomerRegistered", Instant.now()
        );

        // When
        EventMetadata metadata = enricher.enrich(event);

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.getEventId()).isNotNull().startsWith("EVT-");
        assertThat(metadata.getEventType()).isEqualTo("CustomerRegistered");
        assertThat(metadata.getAggregateId()).isEqualTo("CUST-12345678");
        assertThat(metadata.getAggregateType()).isEqualTo("Customer");
        assertThat(metadata.getVersion()).isEqualTo(1L);
        assertThat(metadata.getTimestamp()).isNotNull();
        assertThat(metadata.getCorrelationId()).isNotNull().startsWith("CORR-");
        
        // Islamic banking compliance
        assertThat(metadata.isIslamicBankingCompliant()).isTrue();
        assertThat(metadata.getRegulatoryCompliance()).isEqualTo("CBUAE,VARA,HSA");
        assertThat(metadata.isAuditRequired()).isTrue();
        assertThat(metadata.getAuditLevel()).isEqualTo("MEDIUM");
        assertThat(metadata.getComplianceType()).isEqualTo("GENERAL");
    }

    @Test
    @DisplayName("Should enrich payment event with CBDC compliance metadata")
    void shouldEnrichPaymentEventWithCbdcComplianceMetadata() {
        // Given
        TestPaymentEvent event = new TestPaymentEvent(
            "PAY-87654321", "CbdcPaymentSettled", Instant.now()
        );

        // When
        EventMetadata metadata = enricher.enrich(event);

        // Then
        assertThat(metadata.getAggregateType()).isEqualTo("Payment");
        assertThat(metadata.isCbdcCompliant()).isTrue();
        assertThat(metadata.getComplianceType()).isEqualTo("CBDC");
        assertThat(metadata.getAuditLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should enrich Murabaha event with Sharia compliance metadata")
    void shouldEnrichMurabahaEventWithShariaComplianceMetadata() {
        // Given
        TestMurabahaEvent event = new TestMurabahaEvent(
            "MUR-11111111", "MurabahaContractApproved", Instant.now()
        );

        // When
        EventMetadata metadata = enricher.enrich(event);

        // Then
        assertThat(metadata.getAggregateType()).isEqualTo("MurabahaContract");
        assertThat(metadata.isShariahApproved()).isTrue();
        assertThat(metadata.getComplianceType()).isEqualTo("SHARIA");
        assertThat(metadata.getAuditLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should enrich compliance event with AML metadata")
    void shouldEnrichComplianceEventWithAmlMetadata() {
        // Given
        TestComplianceEvent event = new TestComplianceEvent(
            "CHK-99999999", "ComplianceCheckCreated", Instant.now()
        );

        // When
        EventMetadata metadata = enricher.enrich(event);

        // Then
        assertThat(metadata.getAggregateType()).isEqualTo("ComplianceCheck");
        assertThat(metadata.getComplianceType()).isEqualTo("AML");
        assertThat(metadata.getAuditLevel()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Should handle non-Islamic banking events")
    void shouldHandleNonIslamicBankingEvents() {
        // Given
        TestGeneralEvent event = new TestGeneralEvent(
            "GEN-12345678", "GeneralEvent", Instant.now()
        );

        // When
        EventMetadata metadata = enricher.enrich(event);

        // Then
        assertThat(metadata.isIslamicBankingCompliant()).isFalse();
        assertThat(metadata.isAuditRequired()).isFalse();
        assertThat(metadata.getAuditLevel()).isEqualTo("LOW");
    }

    // Test event classes
    static class TestCustomerRegisteredEvent implements IslamicBankingEvent {
        private final String aggregateId;
        private final String eventType;
        private final Instant timestamp;

        public TestCustomerRegisteredEvent(String aggregateId, String eventType, Instant timestamp) {
            this.aggregateId = aggregateId;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }

        @Override
        public String getAggregateId() { return aggregateId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public Instant getTimestamp() { return timestamp; }
    }

    static class TestPaymentEvent implements IslamicBankingEvent {
        private final String aggregateId;
        private final String eventType;
        private final Instant timestamp;

        public TestPaymentEvent(String aggregateId, String eventType, Instant timestamp) {
            this.aggregateId = aggregateId;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }

        @Override
        public String getAggregateId() { return aggregateId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public Instant getTimestamp() { return timestamp; }
    }

    static class TestMurabahaEvent implements IslamicBankingEvent {
        private final String aggregateId;
        private final String eventType;
        private final Instant timestamp;

        public TestMurabahaEvent(String aggregateId, String eventType, Instant timestamp) {
            this.aggregateId = aggregateId;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }

        @Override
        public String getAggregateId() { return aggregateId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public Instant getTimestamp() { return timestamp; }
    }

    static class TestComplianceEvent implements IslamicBankingEvent {
        private final String aggregateId;
        private final String eventType;
        private final Instant timestamp;

        public TestComplianceEvent(String aggregateId, String eventType, Instant timestamp) {
            this.aggregateId = aggregateId;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }

        @Override
        public String getAggregateId() { return aggregateId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public Instant getTimestamp() { return timestamp; }
    }

    static class TestGeneralEvent implements DomainEvent {
        private final String aggregateId;
        private final String eventType;
        private final Instant timestamp;

        public TestGeneralEvent(String aggregateId, String eventType, Instant timestamp) {
            this.aggregateId = aggregateId;
            this.eventType = eventType;
            this.timestamp = timestamp;
        }

        @Override
        public String getAggregateId() { return aggregateId; }
        @Override
        public String getEventType() { return eventType; }
        @Override
        public Instant getTimestamp() { return timestamp; }
    }
}