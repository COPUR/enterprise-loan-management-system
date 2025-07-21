package com.enterprise.openfinance.infrastructure.monitoring.integration;

import com.enterprise.openfinance.domain.model.consent.ConsentId;
import com.enterprise.openfinance.domain.model.participant.ParticipantId;
import com.enterprise.openfinance.infrastructure.monitoring.*;
import com.enterprise.openfinance.infrastructure.monitoring.model.*;
import com.enterprise.openfinance.infrastructure.monitoring.repository.ComplianceReportingRepository;
import com.enterprise.shared.domain.CustomerId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for complete monitoring and compliance infrastructure.
 * Tests end-to-end workflows including metrics collection, compliance monitoring,
 * violation detection, and automated alerting.
 */
@ExtendWith(MockitoExtension.class)
@Tag("integration")
@Tag("monitoring")
@Tag("compliance")
@DisplayName("Monitoring Integration Tests")
class MonitoringIntegrationTest {

    private MeterRegistry meterRegistry;
    private PrometheusMetricsCollector metricsCollector;
    
    @Mock
    private ComplianceReportingRepository reportingRepository;
    
    @Mock
    private AlertingService alertingService;
    
    @Mock
    private AuditTrailService auditTrailService;
    
    private ComplianceMonitoringService complianceService;
    
    // Test data
    private static final ConsentId TEST_CONSENT = ConsentId.of("CONSENT-INTEGRATION-001");
    private static final ParticipantId TEST_PARTICIPANT = ParticipantId.of("BANK-INTEG01");
    private static final CustomerId TEST_CUSTOMER = CustomerId.of("CUSTOMER-INTEG-001");

    @BeforeEach
    void setUp() {
        // Real metrics registry for integration testing
        meterRegistry = new SimpleMeterRegistry();
        metricsCollector = new PrometheusMetricsCollector(meterRegistry);
        
        complianceService = new ComplianceMonitoringService(
            metricsCollector,
            reportingRepository,
            alertingService,
            auditTrailService
        );
    }

    // === End-to-End Compliance Monitoring Workflow ===

    @Test
    @DisplayName("Given complete monitoring workflow, When processing API request, Then should track all metrics")
    void should_execute_complete_monitoring_workflow_for_api_request() {
        // Given: Complete API request workflow simulation
        var endpoint = "/open-finance/v1/accounts";
        var method = "GET";
        
        // When: Simulating complete API request with monitoring
        
        // 1. Start API request timing
        var apiTimer = metricsCollector.startApiRequestTimer(endpoint, method, TEST_PARTICIPANT);
        
        // 2. Validate FAPI compliance during request processing
        var dpopToken = "eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IlJTMjU2In0.valid.signature";
        var requestSignature = "valid-signature";
        complianceService.validateFAPICompliance(dpopToken, requestSignature, TEST_PARTICIPANT, endpoint);
        
        // 3. Process consent validation
        var consentTimer = metricsCollector.startConsentValidation(TEST_CONSENT, TEST_PARTICIPANT);
        metricsCollector.recordConsentValidation(consentTimer, true, TEST_CONSENT, "VALID");
        
        // 4. Record successful API completion
        metricsCollector.recordApiRequest(apiTimer, endpoint, method, "200", TEST_PARTICIPANT);
        
        // Then: Should have recorded comprehensive metrics
        
        // API metrics
        var requestCounter = meterRegistry.find("openfinance_api_requests_total")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .tag("status", "200")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
        assertThat(requestCounter).isNotNull();
        assertThat(requestCounter.count()).isEqualTo(1.0);
        
        // FAPI security checks
        var dpopCheckCounter = meterRegistry.find("openfinance_fapi_security_checks_total")
            .tag("check_type", "DPOP_VALIDATION")
            .tag("result", "passed")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
        assertThat(dpopCheckCounter).isNotNull();
        assertThat(dpopCheckCounter.count()).isEqualTo(1.0);
        
        // Consent validation metrics
        var consentValidationCounter = meterRegistry.find("openfinance_consent_validations_total")
            .tag("result", "valid")
            .tag("validation_result", "VALID")
            .counter();
        assertThat(consentValidationCounter).isNotNull();
        assertThat(consentValidationCounter.count()).isEqualTo(1.0);
        
        // Consent validation timing
        var consentValidationTimer = meterRegistry.find("openfinance_consent_validation_duration").timer();
        assertThat(consentValidationTimer).isNotNull();
        assertThat(consentValidationTimer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Given security violation detection, When processing violation, Then should trigger complete alert workflow")
    void should_execute_complete_security_violation_workflow() {
        // Given: Security violation scenario
        var violationType = "FAPI_VIOLATION";
        var severity = "HIGH";
        var details = "Invalid DPoP token detected in API request";
        
        // When: Recording security violation (triggers metrics and alerting)
        metricsCollector.recordSecurityViolation(violationType, TEST_PARTICIPANT, severity, details);
        
        // Then: Should have recorded security violation metrics
        var securityViolationCounter = meterRegistry.find("openfinance_security_violations_total")
            .tag("violation_type", violationType)
            .tag("participant", TEST_PARTICIPANT.getValue())
            .tag("severity", severity)
            .counter();
        assertThat(securityViolationCounter).isNotNull();
        assertThat(securityViolationCounter.count()).isEqualTo(1.0);
        
        // And: Simulating complete FAPI validation that triggers alerting
        complianceService.validateFAPICompliance(
            "invalid-token", "invalid-signature", TEST_PARTICIPANT, "/test-endpoint"
        );
        
        // And: Should have triggered security alert
        verify(alertingService).sendSecurityAlert(
            eq("FAPI 2.0 Compliance Violation"),
            eq(TEST_PARTICIPANT.getValue()),
            eq("HIGH"),
            argThat(message -> message.contains("failed FAPI security checks"))
        );
    }

    @Test
    @DisplayName("Given comprehensive compliance check, When performing CBUAE validation, Then should complete full workflow")
    void should_execute_complete_cbuae_compliance_workflow() {
        // Given: Mock successful compliance statistics
        when(reportingRepository.getComplianceStatistics(any(Instant.class), any(Instant.class)))
            .thenReturn(ComplianceStatistics.builder().violationCount(0).build());
        
        // When: Performing comprehensive CBUAE compliance check
        var complianceCheckFuture = complianceService.performCBUAEComplianceCheck(
            TEST_CONSENT, TEST_PARTICIPANT, TEST_CUSTOMER
        );
        
        // Then: Should complete successfully
        assertThat(complianceCheckFuture).succeedsWithin(10, TimeUnit.SECONDS);
        
        var result = complianceCheckFuture.join();
        
        // And: Should have comprehensive compliance check result
        assertThat(result.getCheckId()).startsWith("CBUAE-");
        assertThat(result.getConsentId()).isEqualTo(TEST_CONSENT);
        assertThat(result.getParticipantId()).isEqualTo(TEST_PARTICIPANT);
        assertThat(result.getCustomerId()).isEqualTo(TEST_CUSTOMER);
        assertThat(result.getCheckType()).isEqualTo(ComplianceCheckType.CBUAE_REGULATION);
        
        // And: Should have performed all required checks
        assertThat(result.getCheckResults()).containsKeys(
            "consent_validity",
            "participant_authorization",
            "data_access_limits", 
            "customer_notifications",
            "audit_trail"
        );
        
        // And: Should have saved compliance result
        verify(reportingRepository).saveComplianceCheckResult(result);
        
        // And: Should have calculated overall compliance score
        assertThat(result.getOverallScore()).isEqualTo(100.0);
        assertThat(result.getComplianceStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
    }

    @Test
    @DisplayName("Given PCI compliance check, When validating security controls, Then should complete full assessment")
    void should_execute_complete_pci_compliance_workflow() {
        // Given: PCI compliance check context
        var dataProcessingContext = "OPEN_FINANCE_API";
        
        // When: Performing PCI-DSS v4 compliance check
        var pciCheckFuture = complianceService.performPCIComplianceCheck(
            dataProcessingContext, TEST_PARTICIPANT
        );
        
        // Then: Should complete successfully
        assertThat(pciCheckFuture).succeedsWithin(10, TimeUnit.SECONDS);
        
        var result = pciCheckFuture.join();
        
        // And: Should have comprehensive PCI check result
        assertThat(result.getCheckId()).startsWith("PCI-");
        assertThat(result.getParticipantId()).isEqualTo(TEST_PARTICIPANT);
        assertThat(result.getCheckType()).isEqualTo(ComplianceCheckType.PCI_DSS_V4);
        
        // And: Should have validated all PCI-DSS requirements
        assertThat(result.getCheckResults()).containsKeys(
            "data_encryption",      // Requirement 3
            "access_control",       // Requirement 7  
            "network_security",     // Requirement 1
            "monitoring_logging",   // Requirement 10
            "authentication"        // Requirement 8
        );
        
        // And: Should have recorded audit event
        verify(metricsCollector).recordAuditEvent(
            eq("PCI_COMPLIANCE_CHECK"),
            argThat(eventData -> eventData.containsKey("score") && eventData.containsKey("status"))
        );
        
        // And: Should have achieved full compliance
        assertThat(result.getOverallScore()).isEqualTo(100.0);
        assertThat(result.getComplianceStatus()).isEqualTo(ComplianceStatus.COMPLIANT);
    }

    // === Cross-Platform Data Sharing Monitoring ===

    @Test
    @DisplayName("Given cross-platform data sharing, When orchestrating saga, Then should monitor complete workflow")
    void should_monitor_complete_cross_platform_data_sharing_workflow() {
        // Given: Cross-platform data sharing scenario
        var requestId = "REQ-INTEGRATION-001";
        var platforms = new String[]{"ENTERPRISE_LOANS", "AMANAHFI_PLATFORM", "MASRUFI_FRAMEWORK"};
        var dataSize = 4096L;
        
        // When: Monitoring complete data sharing workflow
        
        // 1. Start data sharing request tracking
        var dataSharingTimer = metricsCollector.startDataSharingRequest(
            requestId, platforms, TEST_PARTICIPANT
        );
        
        // 2. Record platform latencies during processing
        metricsCollector.recordPlatformLatency("ENTERPRISE_LOANS", 150L);
        metricsCollector.recordPlatformLatency("AMANAHFI_PLATFORM", 200L);
        metricsCollector.recordPlatformLatency("MASRUFI_FRAMEWORK", 120L);
        
        // 3. Complete data sharing successfully
        metricsCollector.recordDataSharingCompletion(
            dataSharingTimer, requestId, true, dataSize, platforms
        );
        
        // Then: Should have recorded comprehensive data sharing metrics
        
        // Data sharing request count
        var dataSharingRequestCounter = meterRegistry.find("openfinance_data_sharing_requests_total")
            .tag("platforms_count", "3")
            .tag("participant", TEST_PARTICIPANT.getValue())
            .counter();
        assertThat(dataSharingRequestCounter).isNotNull();
        assertThat(dataSharingRequestCounter.count()).isEqualTo(1.0);
        
        // Data sharing timing
        var dataSharingTimingTimer = meterRegistry.find("openfinance_data_sharing_duration").timer();
        assertThat(dataSharingTimingTimer).isNotNull();
        assertThat(dataSharingTimingTimer.count()).isEqualTo(1);
        
        // Data size tracking
        var dataSizeGauge = meterRegistry.find("openfinance_data_sharing_size_bytes").gauge();
        assertThat(dataSizeGauge).isNotNull();
        assertThat(dataSizeGauge.value()).isEqualTo(dataSize);
        
        // Platform-specific latency tracking
        var enterpriseLatencyGauge = meterRegistry.find("openfinance_platform_latency_ms")
            .tag("platform", "ENTERPRISE_LOANS")
            .gauge();
        assertThat(enterpriseLatencyGauge).isNotNull();
        assertThat(enterpriseLatencyGauge.value()).isEqualTo(150.0);
        
        var amanahfiLatencyGauge = meterRegistry.find("openfinance_platform_latency_ms")
            .tag("platform", "AMANAHFI_PLATFORM")
            .gauge();
        assertThat(amanahfiLatencyGauge).isNotNull();
        assertThat(amanahfiLatencyGauge.value()).isEqualTo(200.0);
    }

    @Test
    @DisplayName("Given data sharing failure, When handling failure, Then should trigger complete alert workflow")
    void should_handle_complete_data_sharing_failure_workflow() {
        // Given: Data sharing failure scenario
        var requestId = "REQ-FAIL-001";
        var platforms = new String[]{"ENTERPRISE_LOANS", "AMANAHFI_PLATFORM"};
        var sagaId = "SAGA-FAIL-001";
        var failureReason = "Enterprise loan service timeout after 2 minutes";
        
        // When: Recording data sharing failure
        var timer = metricsCollector.startDataSharingRequest(requestId, platforms, TEST_PARTICIPANT);
        metricsCollector.recordDataSharingCompletion(timer, requestId, false, 0L, platforms);
        
        // And: Triggering data sharing failure alert
        var alertFuture = alertingService.sendDataSharingFailureAlert(
            sagaId, platforms, failureReason, TEST_PARTICIPANT
        );
        
        // Then: Should have recorded failure metrics
        var failureCounter = meterRegistry.find("openfinance_data_sharing_failures_total")
            .tag("platforms_count", "2")
            .counter();
        assertThat(failureCounter).isNotNull();
        assertThat(failureCounter.count()).isEqualTo(1.0);
        
        // And: Should complete alert successfully
        assertThat(alertFuture).succeedsWithin(5, TimeUnit.SECONDS);
    }

    // === Compliance Reporting Integration ===

    @Test
    @DisplayName("Given daily compliance monitoring, When generating report, Then should complete full reporting workflow")
    void should_execute_complete_daily_compliance_reporting_workflow() {
        // Given: Mock daily compliance data
        when(reportingRepository.getComplianceStatistics(any(Instant.class), any(Instant.class)))
            .thenReturn(ComplianceStatistics.builder()
                .violationCount(3)
                .build());
                
        when(reportingRepository.getSecurityViolations(any(Instant.class), any(Instant.class)))
            .thenReturn(List.of(
                SecurityViolation.builder()
                    .violationType("FAPI_VIOLATION")
                    .severity("HIGH")
                    .description("Multiple DPoP token failures")
                    .build(),
                SecurityViolation.builder()
                    .violationType("RATE_LIMIT_EXCEEDED")
                    .severity("MEDIUM")
                    .description("API rate limits exceeded")
                    .build()
            ));
            
        when(auditTrailService.getAuditEvents(any(Instant.class), any(Instant.class)))
            .thenReturn(Map.of(
                "CONSENT_ACCESS", 250L,
                "DATA_SHARING", 189L,
                "SECURITY_VIOLATION", 3L
            ));
        
        // When: Generating daily compliance report
        complianceService.generateDailyComplianceReport();
        
        // Then: Should have completed full reporting workflow
        
        // Should have collected compliance statistics
        verify(reportingRepository).getComplianceStatistics(any(Instant.class), any(Instant.class));
        
        // Should have collected security violations
        verify(reportingRepository).getSecurityViolations(any(Instant.class), any(Instant.class));
        
        // Should have collected audit events
        verify(auditTrailService).getAuditEvents(any(Instant.class), any(Instant.class));
        
        // Should have saved daily report
        verify(reportingRepository).saveDailyComplianceReport(any(ComplianceReport.class));
        
        // Should have distributed report due to violations
        verify(alertingService).sendComplianceReport(any(ComplianceReport.class));
    }

    // === Performance and Scalability Integration ===

    @Test
    @DisplayName("Given high-volume monitoring, When processing concurrent requests, Then should handle load efficiently")
    void should_handle_high_volume_monitoring_efficiently() {
        // Given: High-volume concurrent monitoring scenario
        var concurrentRequests = 50;
        
        // When: Processing multiple concurrent API requests with full monitoring
        var futures = java.util.stream.IntStream.range(0, concurrentRequests)
            .parallel()
            .mapToObj(i -> CompletableFuture.runAsync(() -> {
                // Simulate complete API request monitoring workflow
                var endpoint = "/open-finance/v1/accounts/" + i;
                var participantId = ParticipantId.of("BANK-" + String.format("%02d", i % 10));
                
                // API timing
                var timer = metricsCollector.startApiRequestTimer(endpoint, "GET", participantId);
                metricsCollector.recordApiRequest(timer, endpoint, "GET", "200", participantId);
                
                // FAPI validation
                complianceService.validateFAPICompliance(
                    "eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IlJTMjU2In0.valid.sig" + i,
                    "signature-" + i,
                    participantId,
                    endpoint
                );
                
                // Audit event
                metricsCollector.recordAuditEvent("API_ACCESS", Map.of(
                    "endpoint", endpoint,
                    "participant", participantId.getValue(),
                    "request_id", "REQ-" + i
                ));
            }))
            .toList();
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // Then: Should have processed all requests successfully
        
        // API request metrics should be recorded
        var totalApiRequests = meterRegistry.find("openfinance_api_requests_total").counter();
        assertThat(totalApiRequests).isNotNull();
        assertThat(totalApiRequests.count()).isEqualTo(concurrentRequests);
        
        // FAPI security checks should be recorded
        var totalFapiChecks = meterRegistry.find("openfinance_fapi_security_checks_total").counter();
        assertThat(totalFapiChecks).isNotNull();
        assertThat(totalFapiChecks.count()).isGreaterThanOrEqualTo(concurrentRequests * 4); // 4 checks per request
        
        // Audit events should be recorded
        var totalAuditEvents = meterRegistry.find("openfinance_audit_events_total").counter();
        assertThat(totalAuditEvents).isNotNull();
        assertThat(totalAuditEvents.count()).isEqualTo(concurrentRequests);
        
        // Health metrics should be available
        var healthMetrics = metricsCollector.getHealthMetrics();
        assertThat(healthMetrics).containsKeys(
            "total_api_requests",
            "total_errors", 
            "security_violations",
            "pci_compliance_score"
        );
    }

    // === Real-time Metrics Validation ===

    @Test
    @DisplayName("Given metrics collection, When checking health status, Then should provide real-time insights")
    void should_provide_real_time_monitoring_insights() {
        // Given: Various monitoring activities
        
        // Record API activities
        var timer1 = metricsCollector.startApiRequestTimer("/accounts", "GET", TEST_PARTICIPANT);
        metricsCollector.recordApiRequest(timer1, "/accounts", "GET", "200", TEST_PARTICIPANT);
        
        var timer2 = metricsCollector.startApiRequestTimer("/loans", "GET", TEST_PARTICIPANT);  
        metricsCollector.recordApiRequest(timer2, "/loans", "GET", "500", TEST_PARTICIPANT);
        
        // Record security events
        metricsCollector.recordSecurityViolation("FAPI_VIOLATION", TEST_PARTICIPANT, "HIGH", "Test violation");
        
        // Record platform latencies
        metricsCollector.recordPlatformLatency("ENTERPRISE_LOANS", 180L);
        metricsCollector.recordPlatformLatency("AMANAHFI_PLATFORM", 220L);
        
        // Update active connections
        metricsCollector.updateActiveConnections("ENTERPRISE_LOANS", 25L);
        metricsCollector.updateActiveConnections("AMANAHFI_PLATFORM", 18L);
        
        // When: Getting health metrics
        var healthMetrics = metricsCollector.getHealthMetrics();
        
        // Then: Should provide comprehensive real-time insights
        assertThat(healthMetrics).containsKeys(
            "active_connections",
            "platform_latencies", 
            "total_api_requests",
            "total_errors",
            "security_violations",
            "pci_compliance_score"
        );
        
        // And: Should have accurate metrics
        assertThat((Double) healthMetrics.get("total_api_requests")).isEqualTo(2.0);
        assertThat((Double) healthMetrics.get("total_errors")).isEqualTo(1.0);
        assertThat((Double) healthMetrics.get("security_violations")).isEqualTo(1.0);
        assertThat((Double) healthMetrics.get("pci_compliance_score")).isEqualTo(100.0);
        
        // And: Should have platform-specific data
        @SuppressWarnings("unchecked")
        var activeConnections = (Map<String, ?>) healthMetrics.get("active_connections");
        assertThat(activeConnections).containsKeys("ENTERPRISE_LOANS", "AMANAHFI_PLATFORM");
        
        @SuppressWarnings("unchecked")
        var platformLatencies = (Map<String, ?>) healthMetrics.get("platform_latencies");
        assertThat(platformLatencies).containsKeys("ENTERPRISE_LOANS", "AMANAHFI_PLATFORM");
    }
}