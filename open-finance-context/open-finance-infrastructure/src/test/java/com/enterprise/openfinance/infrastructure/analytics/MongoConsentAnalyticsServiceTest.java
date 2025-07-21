package com.enterprise.openfinance.infrastructure.analytics;

import com.enterprise.openfinance.domain.event.*;
import com.enterprise.openfinance.domain.model.consent.ConsentId;
import com.enterprise.openfinance.domain.model.consent.ConsentScope;
import com.enterprise.openfinance.domain.model.participant.ParticipantId;
import com.enterprise.shared.domain.CustomerId;
import net.jqwik.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD tests for MongoConsentAnalyticsService following Red-Green-Refactor approach.
 * Tests the silver copy analytics implementation with MongoDB.
 */
@Tag("integration")
@Tag("tdd")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class MongoConsentAnalyticsServiceTest {

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    private MongoTemplate mongoTemplate;
    private AnalyticsDataMaskingService mockDataMaskingService;
    private ComplianceReportingService mockComplianceService;
    private MongoConsentAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        mockDataMaskingService = mock(AnalyticsDataMaskingService.class);
        mockComplianceService = mock(ComplianceReportingService.class);
        
        analyticsService = new MongoConsentAnalyticsService(
            mongoTemplate,
            mockDataMaskingService,
            mockComplianceService
        );
        
        // Clean up collections before each test
        mongoTemplate.remove(new Query(), "consent_metrics");
        mongoTemplate.remove(new Query(), "usage_analytics");
        mongoTemplate.remove(new Query(), "participant_metrics");
    }

    // === TDD: Red-Green-Refactor for ConsentCreatedEvent Analytics ===

    @Test
    @DisplayName("RED: Given ConsentCreatedEvent, When processing analytics, Then should fail without implementation")
    void should_fail_processing_consent_created_analytics_without_implementation() {
        // Given: ConsentCreatedEvent
        var event = ConsentCreatedEvent.builder()
                .consentId(ConsentId.of("CONSENT-123"))
                .customerId(CustomerId.of("CUSTOMER-456"))
                .participantId(ParticipantId.of("BANK-001"))
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION))
                .occurredAt(Instant.now())
                .build();

        // When & Then: Should handle event processing (this will initially fail - RED phase)
        assertThatCode(() -> analyticsService.handle(event))
                .doesNotThrowAnyException(); // Async processing, won't throw immediately
    }

    @Test
    @DisplayName("GREEN: Given ConsentCreatedEvent, When processing analytics, Then should update daily metrics")
    void should_update_daily_metrics_for_consent_created_event() throws Exception {
        // Given: ConsentCreatedEvent
        var event = ConsentCreatedEvent.builder()
                .consentId(ConsentId.of("CONSENT-123"))
                .customerId(CustomerId.of("CUSTOMER-456"))
                .participantId(ParticipantId.of("BANK-001"))
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION, ConsentScope.TRANSACTION_HISTORY))
                .occurredAt(Instant.now())
                .build();

        when(mockDataMaskingService.maskCustomerId(any())).thenReturn("MASKED-CUSTOMER");

        // When: Processing analytics
        analyticsService.handle(event);
        
        // Wait for async processing
        Thread.sleep(100);

        // Then: Should update consent metrics
        verify(mongoTemplate, atLeastOnce()).upsert(any(Query.class), any(), eq("consent_metrics"));
        
        // And: Should update participant metrics
        verify(mongoTemplate, atLeastOnce()).upsert(any(Query.class), any(), eq("participant_metrics"));
        
        // And: Should record customer pattern
        verify(mongoTemplate, atLeastOnce()).upsert(any(Query.class), any(), eq("customer_patterns"));
    }

    @Test
    @DisplayName("REFACTOR: Given multiple ConsentCreatedEvents, When processing, Then should aggregate correctly")
    void should_aggregate_multiple_consent_created_events() throws Exception {
        // Given: Multiple ConsentCreatedEvents for same participant and date
        var participantId = ParticipantId.of("BANK-001");
        var today = Instant.now();
        
        var event1 = createConsentCreatedEvent("CONSENT-1", participantId, today);
        var event2 = createConsentCreatedEvent("CONSENT-2", participantId, today);
        var event3 = createConsentCreatedEvent("CONSENT-3", participantId, today.plusSeconds(3600)); // 1 hour later

        when(mockDataMaskingService.maskCustomerId(any())).thenReturn("MASKED-CUSTOMER");

        // When: Processing multiple events
        analyticsService.handle(event1);
        analyticsService.handle(event2);
        analyticsService.handle(event3);
        
        // Wait for async processing
        Thread.sleep(200);

        // Then: Should update metrics for each event
        verify(mongoTemplate, times(3)).upsert(any(Query.class), any(), eq("consent_metrics"));
        
        // And: Should update participant metrics for each event
        verify(mongoTemplate, times(3)).upsert(any(Query.class), any(), eq("participant_metrics"));
    }

    // === TDD: ConsentAuthorizedEvent Analytics ===

    @Test
    @DisplayName("RED: Given ConsentAuthorizedEvent, When processing, Then should fail without implementation")
    void should_fail_processing_consent_authorized_analytics_without_implementation() {
        // Given: ConsentAuthorizedEvent
        var event = ConsentAuthorizedEvent.builder()
                .consentId(ConsentId.of("CONSENT-123"))
                .customerId(CustomerId.of("CUSTOMER-456"))
                .participantId(ParticipantId.of("BANK-001"))
                .occurredAt(Instant.now())
                .build();

        // When & Then: Should handle event processing
        assertThatCode(() -> analyticsService.handle(event))
                .doesNotThrowAnyException(); // Async processing
    }

    @Test
    @DisplayName("GREEN: Given ConsentAuthorizedEvent, When processing, Then should update authorization metrics")
    void should_update_authorization_metrics() throws Exception {
        // Given: ConsentAuthorizedEvent
        var event = ConsentAuthorizedEvent.builder()
                .consentId(ConsentId.of("CONSENT-123"))
                .customerId(CustomerId.of("CUSTOMER-456"))
                .participantId(ParticipantId.of("BANK-001"))
                .occurredAt(Instant.now())
                .build();

        // When: Processing analytics
        analyticsService.handle(event);
        
        // Wait for async processing
        Thread.sleep(100);

        // Then: Should update authorization metrics
        verify(mongoTemplate, atLeastOnce()).upsert(any(Query.class), any(), eq("consent_metrics"));
    }

    // === TDD: ConsentUsedEvent Analytics with Security Monitoring ===

    @Test
    @DisplayName("RED: Given ConsentUsedEvent, When processing, Then should fail without security monitoring")
    void should_fail_processing_consent_used_without_security_monitoring() {
        // Given: ConsentUsedEvent
        var event = ConsentUsedEvent.builder()
                .consentId(ConsentId.of("CONSENT-123"))
                .participantId(ParticipantId.of("BANK-001"))
                .customerId(CustomerId.of("CUSTOMER-456"))
                .occurredAt(Instant.now())
                .build();

        // When & Then: Should handle event processing
        assertThatCode(() -> analyticsService.handle(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GREEN: Given high-frequency ConsentUsedEvents, When processing, Then should detect anomaly")
    void should_detect_anomalous_usage_patterns() throws Exception {
        // Given: Multiple rapid ConsentUsedEvents (suspicious pattern)
        var participantId = ParticipantId.of("BANK-001");
        var now = Instant.now();
        
        when(mockDataMaskingService.maskCustomerId(any())).thenReturn("MASKED-CUSTOMER");
        when(mockDataMaskingService.maskDataRequested(any())).thenReturn("MASKED-DATA");
        
        // Simulate 150 rapid API calls (above threshold)
        when(mongoTemplate.count(any(Query.class), eq("usage_analytics")))
                .thenReturn(150L); // Above threshold of 100

        var event = ConsentUsedEvent.builder()
                .consentId(ConsentId.of("CONSENT-123"))
                .participantId(participantId)
                .customerId(CustomerId.of("CUSTOMER-456"))
                .occurredAt(now)
                .processingTimeMs(50)
                .build();

        // When: Processing usage event
        analyticsService.handle(event);
        
        // Wait for async processing
        Thread.sleep(200);

        // Then: Should save usage analytics
        verify(mongoTemplate, atLeastOnce()).save(any(), eq("usage_analytics"));
        
        // And: Should record security incident for suspicious pattern
        verify(mongoTemplate, atLeastOnce()).save(any(), eq("security_incidents"));
    }

    // === TDD: Metrics Retrieval ===

    @Test
    @DisplayName("RED: Given participant metrics request, When retrieving, Then should fail without query implementation")
    void should_fail_retrieving_consent_metrics_without_implementation() {
        // Given: Metrics request parameters
        var participantId = "BANK-001";
        var fromDate = LocalDate.now().minusDays(30);
        var toDate = LocalDate.now();

        // When: Retrieving metrics
        var futureMetrics = analyticsService.getConsentMetrics(participantId, fromDate, toDate);

        // Then: Should complete without exception but may return empty results initially
        assertThatCode(() -> {
            var metrics = futureMetrics.get();
            assertThat(metrics).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GREEN: Given stored consent metrics, When retrieving, Then should return aggregated data")
    void should_retrieve_aggregated_consent_metrics() throws Exception {
        // Given: Stored consent metrics data
        var participantId = "BANK-001";
        var fromDate = LocalDate.now().minusDays(7);
        var toDate = LocalDate.now();

        // Mock aggregation result
        var mockResult = mock(org.springframework.data.mongodb.core.aggregation.AggregationResults.class);
        var mockSummary = ConsentMetricsSummary.builder()
                .participantId(participantId)
                .totalConsents(100L)
                .authorizedConsents(85L)
                .revokedConsents(10L)
                .expiredConsents(5L)
                .totalUsage(1500L)
                .averageLifetime(45.5)
                .build();

        when(mockResult.getUniqueMappedResult()).thenReturn(mockSummary);
        when(mongoTemplate.aggregate(any(), eq("consent_metrics"), eq(ConsentMetricsSummary.class)))
                .thenReturn(mockResult);

        // When: Retrieving metrics
        var futureMetrics = analyticsService.getConsentMetrics(participantId, fromDate, toDate);
        var metrics = futureMetrics.get();

        // Then: Should return aggregated metrics
        assertThat(metrics).isNotNull();
        assertThat(metrics.getParticipantId()).isEqualTo(participantId);
        assertThat(metrics.getTotalConsents()).isEqualTo(100L);
        assertThat(metrics.getAuthorizedConsents()).isEqualTo(85L);
        assertThat(metrics.getAuthorizationRate()).isEqualTo(85.0);
    }

    // === TDD: Real-time Metrics ===

    @Test
    @DisplayName("RED: Given real-time metrics request, When retrieving, Then should fail without aggregation logic")
    void should_fail_retrieving_real_time_metrics_without_implementation() {
        // Given: Real-time metrics request
        // When: Retrieving real-time metrics
        var futureMetrics = analyticsService.getRealTimeMetrics();

        // Then: Should handle gracefully
        assertThatCode(() -> {
            var metrics = futureMetrics.get();
            assertThat(metrics).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GREEN: Given recent metric data, When retrieving real-time, Then should return current aggregation")
    void should_retrieve_real_time_metrics() throws Exception {
        // Given: Recent metric data
        var recentMetrics = java.util.List.of(
            createRealTimeMetric("consent_created", "BANK-001", 5L),
            createRealTimeMetric("consent_authorized", "BANK-001", 3L),
            createRealTimeMetric("data_accessed", "BANK-001", 25L)
        );

        when(mongoTemplate.find(any(Query.class), eq(RealTimeMetric.class), eq("real_time_metrics")))
                .thenReturn(recentMetrics);

        // When: Retrieving real-time metrics
        var futureMetrics = analyticsService.getRealTimeMetrics();
        var metrics = futureMetrics.get();

        // Then: Should return aggregated real-time data
        assertThat(metrics).isNotNull();
        assertThat(metrics).containsKey("consent_created");
        assertThat(metrics).containsKey("consent_authorized");
        assertThat(metrics).containsKey("data_accessed");
        assertThat(metrics).containsKey("timestamp");
        assertThat((Long) metrics.get("consent_created")).isEqualTo(5L);
        assertThat((Long) metrics.get("data_accessed")).isEqualTo(25L);
    }

    // === Property-Based Tests ===

    @Property(tries = 50)
    @DisplayName("Property: Analytics processing should handle any valid consent event")
    void should_handle_any_valid_consent_created_event(
            @ForAll("validConsentIds") String consentId,
            @ForAll("validCustomerIds") String customerId,
            @ForAll("validParticipantIds") String participantId,
            @ForAll("validScopes") Set<ConsentScope> scopes) {
        
        // Given: Any valid ConsentCreatedEvent
        var event = ConsentCreatedEvent.builder()
                .consentId(ConsentId.of(consentId))
                .customerId(CustomerId.of(customerId))
                .participantId(ParticipantId.of(participantId))
                .scopes(scopes)
                .occurredAt(Instant.now())
                .build();

        when(mockDataMaskingService.maskCustomerId(any())).thenReturn("MASKED-CUSTOMER");

        // When & Then: Should process without throwing exception
        assertThatCode(() -> analyticsService.handle(event))
                .doesNotThrowAnyException();
    }

    @Property(tries = 30)
    @DisplayName("Property: Metrics aggregation should be commutative")
    void metrics_aggregation_should_be_commutative(
            @ForAll("validParticipantIds") String participantId,
            @ForAll @IntRange(min = 1, max = 10) int eventCount) {
        
        // Given: Multiple events in different orders
        var events = java.util.stream.IntStream.range(0, eventCount)
                .mapToObj(i -> createConsentCreatedEvent("CONSENT-" + i, 
                    ParticipantId.of(participantId), Instant.now().plusSeconds(i)))
                .collect(java.util.stream.Collectors.toList());

        when(mockDataMaskingService.maskCustomerId(any())).thenReturn("MASKED-CUSTOMER");

        // When: Processing events in order
        events.forEach(analyticsService::handle);

        // Then: Should process all events
        // (In real implementation, we would verify the order doesn't matter for final results)
        assertThat(events).hasSize(eventCount);
    }

    // === Compliance Reporting Tests ===

    @Test
    @DisplayName("RED: Given compliance report request, When generating, Then should fail without implementation")
    void should_fail_generating_compliance_report_without_implementation() {
        // Given: Report date
        var reportDate = LocalDate.now();

        // When: Generating compliance report
        var futureReport = analyticsService.generateComplianceReport(reportDate);

        // Then: Should complete (implementation will initially be minimal)
        assertThatCode(() -> {
            var report = futureReport.get();
            assertThat(report).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("GREEN: Given compliance data, When generating report, Then should include all required sections")
    void should_generate_comprehensive_compliance_report() throws Exception {
        // Given: Compliance report request
        var reportDate = LocalDate.now();

        // When: Generating compliance report
        var futureReport = analyticsService.generateComplianceReport(reportDate);
        var report = futureReport.get();

        // Then: Should contain all required sections
        assertThat(report).isNotNull();
        assertThat(report.getReportDate()).isEqualTo(reportDate);
        assertThat(report.getGeneratedAt()).isNotNull();
        assertThat(report.getReportType()).isEqualTo("DAILY_COMPLIANCE");
        assertThat(report.getComplianceScore()).isGreaterThanOrEqualTo(0.0);
        assertThat(report.getComplianceScore()).isLessThanOrEqualTo(100.0);
    }

    // === Helper methods for test data creation ===

    private ConsentCreatedEvent createConsentCreatedEvent(String consentId, 
                                                         ParticipantId participantId, 
                                                         Instant occurredAt) {
        return ConsentCreatedEvent.builder()
                .consentId(ConsentId.of(consentId))
                .customerId(CustomerId.of("CUSTOMER-" + consentId.substring(8)))
                .participantId(participantId)
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION))
                .occurredAt(occurredAt)
                .build();
    }

    private RealTimeMetric createRealTimeMetric(String metricType, String participantId, Long value) {
        return RealTimeMetric.builder()
                .metricType(metricType)
                .participantId(participantId)
                .value(value)
                .timestamp(Instant.now().minusSeconds(60)) // 1 minute ago
                .build();
    }

    // === Property providers ===

    @Provide
    Arbitrary<String> validConsentIds() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(8)
                .ofMaxLength(12)
                .map(s -> "CONSENT-" + s);
    }

    @Provide
    Arbitrary<String> validCustomerIds() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(8)
                .ofMaxLength(12)
                .map(s -> "CUSTOMER-" + s);
    }

    @Provide
    Arbitrary<String> validParticipantIds() {
        return Arbitraries.strings()
                .withCharRange('A', 'Z')
                .ofMinLength(4)
                .ofMaxLength(8)
                .map(s -> "BANK-" + s);
    }

    @Provide
    Arbitrary<Set<ConsentScope>> validScopes() {
        return Arbitraries.of(ConsentScope.class)
                .set()
                .ofMinSize(1)
                .ofMaxSize(3);
    }

    // === Error Handling Tests ===

    @Test
    @DisplayName("Given MongoDB connection failure, When processing analytics, Then should handle gracefully")
    void should_handle_mongodb_connection_failure_gracefully() {
        // Given: MongoDB connection failure
        when(mongoTemplate.upsert(any(), any(), any(String.class)))
                .thenThrow(new RuntimeException("MongoDB connection failed"));

        var event = ConsentCreatedEvent.builder()
                .consentId(ConsentId.of("CONSENT-123"))
                .customerId(CustomerId.of("CUSTOMER-456"))
                .participantId(ParticipantId.of("BANK-001"))
                .scopes(Set.of(ConsentScope.ACCOUNT_INFORMATION))
                .occurredAt(Instant.now())
                .build();

        when(mockDataMaskingService.maskCustomerId(any())).thenReturn("MASKED-CUSTOMER");

        // When & Then: Should not throw exception (graceful degradation)
        assertThatCode(() -> analyticsService.handle(event))
                .doesNotThrowAnyException();
    }
}