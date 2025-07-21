package com.enterprise.openfinance.infrastructure.monitoring;

import com.enterprise.openfinance.domain.model.consent.ConsentId;
import com.enterprise.openfinance.domain.model.participant.ParticipantId;
import com.enterprise.shared.domain.CustomerId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD tests for PrometheusMetricsCollector.
 * Tests comprehensive monitoring metrics for Open Finance API compliance,
 * security monitoring, and performance tracking.
 */
@Tag("monitoring")
@Tag("tdd")
@Tag("metrics")
@DisplayName("Prometheus Metrics Collector TDD Tests")
class PrometheusMetricsCollectorTest {

    private MeterRegistry meterRegistry;
    private PrometheusMetricsCollector metricsCollector;
    
    // Test data
    private static final ParticipantId TEST_PARTICIPANT = ParticipantId.of("BANK-TEST01");
    private static final ConsentId TEST_CONSENT = ConsentId.of("CONSENT-ABC12345");
    private static final CustomerId TEST_CUSTOMER = CustomerId.of("CUSTOMER-789");

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsCollector = new PrometheusMetricsCollector(meterRegistry);
    }

    // === TDD: Red-Green-Refactor for API Metrics ===

    @Test
    @DisplayName("RED: Given API request timing, When recording metrics, Then should track request duration")
    void should_record_api_request_duration_metrics() {
        // Given: API request timer
        var timer = metricsCollector.startApiRequestTimer("/open-finance/v1/accounts", "GET", TEST_PARTICIPANT);
        
        // Simulate API processing time
        try {
            Thread.sleep(100); // 100ms processing time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When: Recording API request completion
        metricsCollector.recordApiRequest(timer, "/open-finance/v1/accounts", "GET", "200", TEST_PARTICIPANT);
        
        // Then: Should have recorded timing metrics
        var requestTimer = meterRegistry.find("openfinance_api_request_duration")
            .tag("endpoint", "/open-finance/v1/accounts")
            .tag("method", "GET")
            .tag("status", "200")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .timer();
            
        assertThat(requestTimer).isNotNull();
        assertThat(requestTimer.count()).isEqualTo(1);
        assertThat(requestTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(90);
        
        // And: Should have incremented request counter
        var requestCounter = meterRegistry.find("openfinance_api_requests_total")
            .tag("endpoint", "/open-finance/v1/accounts")
            .tag("method", "GET")
            .tag("status", "200")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
            
        assertThat(requestCounter).isNotNull();
        assertThat(requestCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("GREEN: Given API error, When recording error metrics, Then should track error details")
    void should_record_api_error_metrics() {
        // Given: API error scenario
        var endpoint = "/open-finance/v1/loans";
        var method = "GET";
        var errorType = "CONSENT_EXPIRED";
        var errorMessage = "Consent has expired";
        
        // When: Recording API error
        metricsCollector.recordApiError(endpoint, method, errorType, TEST_PARTICIPANT, errorMessage);
        
        // Then: Should have incremented error counter with proper tags
        var errorCounter = meterRegistry.find("openfinance_api_errors_total")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .tag("error_type", errorType)
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
            
        assertThat(errorCounter).isNotNull();
        assertThat(errorCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("REFACTOR: Given multiple API requests, When recording metrics, Then should aggregate properly")
    void should_aggregate_multiple_api_request_metrics() {
        // Given: Multiple API requests to same endpoint
        var endpoint = "/open-finance/v1/accounts";
        
        // When: Recording multiple requests
        for (int i = 0; i < 5; i++) {
            var timer = metricsCollector.startApiRequestTimer(endpoint, "GET", TEST_PARTICIPANT);
            metricsCollector.recordApiRequest(timer, endpoint, "GET", "200", TEST_PARTICIPANT);
        }
        
        // Then: Should aggregate request counts
        var requestCounter = meterRegistry.find("openfinance_api_requests_total")
            .tag("endpoint", endpoint)
            .tag("method", "GET")
            .tag("status", "200")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
            
        assertThat(requestCounter).isNotNull();
        assertThat(requestCounter.count()).isEqualTo(5.0);
    }

    // === TDD: Security Metrics Tests ===

    @Test
    @DisplayName("Given security violation, When recording violation, Then should track security metrics")
    void should_record_security_violation_metrics() {
        // Given: Security violation scenario
        var violationType = "FAPI_VIOLATION";
        var severity = "HIGH";
        var details = "Invalid DPoP token signature";
        
        // When: Recording security violation
        metricsCollector.recordSecurityViolation(violationType, TEST_PARTICIPANT, severity, details);
        
        // Then: Should have incremented security violation counter
        var securityCounter = meterRegistry.find("openfinance_security_violations_total")
            .tag("violation_type", violationType)
            .tag("participant", TEST_PARTICIPANT.getValue())
            .tag("severity", severity)
            .counter();
            
        assertThat(securityCounter).isNotNull();
        assertThat(securityCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Given FAPI security check, When validation passes, Then should record successful check")
    void should_record_successful_fapi_security_check() {
        // Given: Successful FAPI security check
        var checkType = "DPOP_VALIDATION";
        var passed = true;
        
        // When: Recording FAPI security check
        metricsCollector.recordFAPISecurityCheck(checkType, passed, TEST_PARTICIPANT);
        
        // Then: Should record successful check
        var securityCheckCounter = meterRegistry.find("openfinance_fapi_security_checks_total")
            .tag("check_type", checkType)
            .tag("result", "passed")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
            
        assertThat(securityCheckCounter).isNotNull();
        assertThat(securityCheckCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Given FAPI security check failure, When validation fails, Then should record violation and failure")
    void should_record_failed_fapi_security_check_and_violation() {
        // Given: Failed FAPI security check
        var checkType = "REQUEST_SIGNATURE";
        var passed = false;
        
        // When: Recording FAPI security check
        metricsCollector.recordFAPISecurityCheck(checkType, passed, TEST_PARTICIPANT);
        
        // Then: Should record failed check
        var securityCheckCounter = meterRegistry.find("openfinance_fapi_security_checks_total")
            .tag("check_type", checkType)
            .tag("result", "failed")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
            
        assertThat(securityCheckCounter).isNotNull();
        assertThat(securityCheckCounter.count()).isEqualTo(1.0);
        
        // And: Should also record security violation
        var violationCounter = meterRegistry.find("openfinance_security_violations_total")
            .tag("violation_type", "FAPI_VIOLATION")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
            
        assertThat(violationCounter).isNotNull();
        assertThat(violationCounter.count()).isEqualTo(1.0);
    }

    // === TDD: Consent Management Metrics Tests ===

    @Test
    @DisplayName("Given consent creation, When recording consent metrics, Then should track consent lifecycle")
    void should_record_consent_creation_metrics() {
        // Given: Consent creation scenario
        var scopeCount = 3;
        
        // When: Recording consent creation
        metricsCollector.recordConsentCreation(TEST_CONSENT, TEST_PARTICIPANT, TEST_CUSTOMER, scopeCount);
        
        // Then: Should have incremented consent creation counter
        var consentCounter = meterRegistry.find("openfinance_consents_created_total")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .tag("scope_count", String.valueOf(scopeCount))
            .counter();
            
        assertThat(consentCounter).isNotNull();
        assertThat(consentCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Given consent revocation, When recording revocation, Then should track revocation reason")
    void should_record_consent_revocation_metrics() {
        // Given: Consent revocation scenario
        var reason = "CUSTOMER_REQUEST";
        
        // When: Recording consent revocation
        metricsCollector.recordConsentRevocation(TEST_CONSENT, reason, TEST_CUSTOMER);
        
        // Then: Should have incremented revocation counter
        var revocationCounter = meterRegistry.find("openfinance_consents_revoked_total")
            .tag("reason", reason)
            .counter();
            
        assertThat(revocationCounter).isNotNull();
        assertThat(revocationCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Given consent validation, When timing validation, Then should track validation performance")
    void should_record_consent_validation_timing() {
        // Given: Consent validation timing
        var timer = metricsCollector.startConsentValidation(TEST_CONSENT, TEST_PARTICIPANT);
        
        // Simulate validation processing
        try {
            Thread.sleep(50); // 50ms validation time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When: Recording consent validation completion
        metricsCollector.recordConsentValidation(timer, true, TEST_CONSENT, "VALID");
        
        // Then: Should have recorded validation timing
        var validationTimer = meterRegistry.find("openfinance_consent_validation_duration").timer();
        assertThat(validationTimer).isNotNull();
        assertThat(validationTimer.count()).isEqualTo(1);
        assertThat(validationTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(40);
        
        // And: Should have recorded validation result
        var validationCounter = meterRegistry.find("openfinance_consent_validations_total")
            .tag("result", "valid")
            .tag("validation_result", "VALID")
            .counter();
            
        assertThat(validationCounter).isNotNull();
        assertThat(validationCounter.count()).isEqualTo(1.0);
    }

    // === TDD: Data Sharing Metrics Tests ===

    @Test
    @DisplayName("Given data sharing request, When orchestrating saga, Then should track cross-platform metrics")
    void should_record_data_sharing_request_metrics() {
        // Given: Data sharing request
        var requestId = "REQ-123456";
        var platforms = new String[]{"ENTERPRISE_LOANS", "AMANAHFI_PLATFORM", "MASRUFI_FRAMEWORK"};
        
        // When: Starting data sharing request
        var timer = metricsCollector.startDataSharingRequest(requestId, platforms, TEST_PARTICIPANT);
        
        // Then: Should have incremented data sharing request counter
        var requestCounter = meterRegistry.find("openfinance_data_sharing_requests_total")
            .tag("platforms_count", String.valueOf(platforms.length))
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
            
        assertThat(requestCounter).isNotNull();
        assertThat(requestCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Given data sharing completion, When recording success, Then should track data size and latencies")
    void should_record_successful_data_sharing_completion() {
        // Given: Successful data sharing completion
        var requestId = "REQ-789012";
        var platforms = new String[]{"ENTERPRISE_LOANS", "AMANAHFI_PLATFORM"};
        var dataSize = 2048L;
        var timer = metricsCollector.startDataSharingRequest(requestId, platforms, TEST_PARTICIPANT);
        
        // When: Recording data sharing completion
        metricsCollector.recordDataSharingCompletion(timer, requestId, true, dataSize, platforms);
        
        // Then: Should have recorded data sharing timing
        var dataSharingTimer = meterRegistry.find("openfinance_data_sharing_duration").timer();
        assertThat(dataSharingTimer).isNotNull();
        assertThat(dataSharingTimer.count()).isEqualTo(1);
        
        // And: Should have recorded data size
        var dataSizeGauge = meterRegistry.find("openfinance_data_sharing_size_bytes").gauge();
        assertThat(dataSizeGauge).isNotNull();
        assertThat(dataSizeGauge.value()).isEqualTo(dataSize);
        
        // And: Should have recorded platform latencies
        var platformLatencyGauge = meterRegistry.find("openfinance_platform_latency_ms")
            .tag("platform", "ENTERPRISE_LOANS")
            .gauge();
        assertThat(platformLatencyGauge).isNotNull();
    }

    @Test
    @DisplayName("Given data sharing failure, When recording failure, Then should track failure metrics")
    void should_record_data_sharing_failure_metrics() {
        // Given: Failed data sharing scenario
        var requestId = "REQ-FAIL123";
        var platforms = new String[]{"ENTERPRISE_LOANS", "AMANAHFI_PLATFORM"};
        var timer = metricsCollector.startDataSharingRequest(requestId, platforms, TEST_PARTICIPANT);
        
        // When: Recording data sharing failure
        metricsCollector.recordDataSharingCompletion(timer, requestId, false, 0L, platforms);
        
        // Then: Should have incremented failure counter
        var failureCounter = meterRegistry.find("openfinance_data_sharing_failures_total")
            .tag("platforms_count", String.valueOf(platforms.length))
            .counter();
            
        assertThat(failureCounter).isNotNull();
        assertThat(failureCounter.count()).isEqualTo(1.0);
    }

    // === TDD: Compliance Metrics Tests ===

    @Test
    @DisplayName("Given compliance violation, When recording violation, Then should track compliance metrics")
    void should_record_compliance_violation_metrics() {
        // Given: Compliance violation scenario
        var violationType = "DATA_RETENTION_VIOLATION";
        var regulation = "CBUAE_C7_2023";
        var severity = "MEDIUM";
        var description = "Data retained beyond consent expiration";
        
        // When: Recording compliance violation
        metricsCollector.recordComplianceViolation(violationType, regulation, severity, description);
        
        // Then: Should have incremented compliance violation counter
        var complianceCounter = meterRegistry.find("openfinance_compliance_violations_total")
            .tag("violation_type", violationType)
            .tag("regulation", regulation)
            .tag("severity", severity)
            .counter();
            
        assertThat(complianceCounter).isNotNull();
        assertThat(complianceCounter.count()).isEqualTo(1.0);
        
        // And: Should have auto-generated audit event
        var auditCounter = meterRegistry.find("openfinance_audit_events_total")
            .tag("event_type", "COMPLIANCE_VIOLATION")
            .counter();
            
        assertThat(auditCounter).isNotNull();
        assertThat(auditCounter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Given audit event, When recording event, Then should track audit metrics")
    void should_record_audit_event_metrics() {
        // Given: Audit event scenario
        var eventType = "CONSENT_ACCESS";
        var eventData = java.util.Map.of(
            "consent_id", TEST_CONSENT.getValue(),
            "participant_id", TEST_PARTICIPANT.getValue(),
            "action", "ACCOUNT_DATA_ACCESSED"
        );
        
        // When: Recording audit event
        metricsCollector.recordAuditEvent(eventType, eventData);
        
        // Then: Should have incremented audit event counter
        var auditCounter = meterRegistry.find("openfinance_audit_events_total")
            .tag("event_type", eventType)
            .counter();
            
        assertThat(auditCounter).isNotNull();
        assertThat(auditCounter.count()).isEqualTo(1.0);
        
        // And: Should have updated last event timestamp
        var timestampGauge = meterRegistry.find("openfinance_audit_events_last_timestamp").gauge();
        assertThat(timestampGauge).isNotNull();
        assertThat(timestampGauge.value()).isGreaterThan(0);
    }

    // === TDD: Health Metrics Tests ===

    @Test
    @DisplayName("Given platform connection updates, When updating active connections, Then should track platform health")
    void should_track_active_connections_per_platform() {
        // Given: Platform connection updates
        var platform1 = "ENTERPRISE_LOANS";
        var platform2 = "AMANAHFI_PLATFORM";
        var connections1 = 15L;
        var connections2 = 8L;
        
        // When: Updating active connections
        metricsCollector.updateActiveConnections(platform1, connections1);
        metricsCollector.updateActiveConnections(platform2, connections2);
        
        // Then: Should provide health metrics including connections
        var healthMetrics = metricsCollector.getHealthMetrics();
        
        assertThat(healthMetrics).containsKey("active_connections");
        assertThat(healthMetrics.get("platform_latencies")).isNotNull();
        assertThat(healthMetrics.get("total_api_requests")).isNotNull();
        assertThat(healthMetrics.get("pci_compliance_score")).isEqualTo(100.0); // Default full compliance
    }

    @Test
    @DisplayName("Given platform latency updates, When recording latencies, Then should calculate average latency")
    void should_track_and_calculate_average_platform_latency() {
        // Given: Platform latency updates
        var platform1 = "ENTERPRISE_LOANS";
        var platform2 = "MASRUFI_FRAMEWORK";
        var latency1 = 120L; // 120ms
        var latency2 = 80L;  // 80ms
        
        // When: Recording platform latencies
        metricsCollector.recordPlatformLatency(platform1, latency1);
        metricsCollector.recordPlatformLatency(platform2, latency2);
        
        // Then: Should have individual platform latency gauges
        var platform1LatencyGauge = meterRegistry.find("openfinance_platform_latency_ms")
            .tag("platform", platform1)
            .gauge();
        assertThat(platform1LatencyGauge).isNotNull();
        assertThat(platform1LatencyGauge.value()).isEqualTo(latency1);
        
        // And: Should calculate average latency correctly
        var healthMetrics = metricsCollector.getHealthMetrics();
        @SuppressWarnings("unchecked")
        var platformLatencies = (java.util.Map<String, java.util.concurrent.atomic.AtomicLong>) healthMetrics.get("platform_latencies");
        assertThat(platformLatencies).hasSize(2);
        assertThat(platformLatencies.get(platform1).get()).isEqualTo(latency1);
        assertThat(platformLatencies.get(platform2).get()).isEqualTo(latency2);
    }
}