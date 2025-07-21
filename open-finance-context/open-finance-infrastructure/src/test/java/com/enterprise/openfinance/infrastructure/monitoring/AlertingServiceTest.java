package com.enterprise.openfinance.infrastructure.monitoring;

import com.enterprise.openfinance.domain.model.participant.ParticipantId;
import com.enterprise.openfinance.infrastructure.monitoring.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for AlertingService.
 * Tests comprehensive alerting system for security violations,
 * compliance breaches, performance issues, and report distribution.
 */
@ExtendWith(MockitoExtension.class)
@Tag("alerting")
@Tag("tdd")
@Tag("monitoring")
@DisplayName("Alerting Service TDD Tests")
class AlertingServiceTest {

    @Mock
    private EmailNotificationService emailService;
    
    @Mock
    private SlackNotificationService slackService;
    
    @Mock
    private WebhookNotificationService webhookService;
    
    @Mock
    private SMSNotificationService smsService;
    
    @Mock
    private AlertConfigurationService alertConfigService;

    private AlertingService alertingService;
    
    // Test data
    private static final ParticipantId TEST_PARTICIPANT = ParticipantId.of("BANK-TEST01");
    private static final String TEST_PARTICIPANT_ID = TEST_PARTICIPANT.getValue();

    @BeforeEach
    void setUp() {
        alertingService = new AlertingService(
            emailService,
            slackService,
            webhookService,
            smsService,
            alertConfigService
        );
        
        // Setup default mock configurations
        when(alertConfigService.getSecurityAlertConfiguration())
            .thenReturn(createMockSecurityAlertConfig());
        when(alertConfigService.getComplianceAlertConfiguration())
            .thenReturn(createMockComplianceAlertConfig());
        when(alertConfigService.getPerformanceAlertConfiguration())
            .thenReturn(createMockPerformanceAlertConfig());
    }

    // === TDD: Red-Green-Refactor for Security Alerts ===

    @Test
    @DisplayName("RED: Given critical security alert, When sending alert, Then should notify all channels")
    void should_send_critical_security_alert_to_all_channels() {
        // Given: Critical security alert
        var alertTitle = "FAPI Security Breach Detected";
        var severity = "CRITICAL";
        var description = "Unauthorized access attempt with invalid DPoP token";
        
        // When: Sending critical security alert
        var futureResult = alertingService.sendSecurityAlert(
            alertTitle, TEST_PARTICIPANT_ID, severity, description
        );
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(5, TimeUnit.SECONDS);
        
        // And: Should have sent to all notification channels for critical alerts
        verify(emailService).sendSecurityAlert(any(SecurityAlert.class), anyList());
        verify(slackService).sendSecurityAlert(any(SecurityAlert.class), anyString());
        verify(webhookService).sendSecurityAlert(any(SecurityAlert.class), anyList());
        verify(smsService).sendCriticalAlert(any(SecurityAlert.class), anyList());
    }

    @Test
    @DisplayName("GREEN: Given high severity security alert, When sending alert, Then should use email and Slack")
    void should_send_high_severity_alert_via_email_and_slack() {
        // Given: High severity security alert
        var alertTitle = "Repeated Authentication Failures";
        var severity = "HIGH";
        var description = "Multiple failed login attempts from participant";
        
        // When: Sending high severity security alert
        var futureResult = alertingService.sendSecurityAlert(
            alertTitle, TEST_PARTICIPANT_ID, severity, description
        );
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have sent to email and Slack (not SMS for HIGH)
        verify(emailService).sendSecurityAlert(any(SecurityAlert.class), anyList());
        verify(slackService).sendSecurityAlert(any(SecurityAlert.class), anyString());
        verify(webhookService).sendSecurityAlert(any(SecurityAlert.class), anyList());
        
        // But: Should not have sent SMS for high severity
        verify(smsService, never()).sendCriticalAlert(any(), any());
    }

    @Test
    @DisplayName("REFACTOR: Given medium severity security alert, When sending alert, Then should use Slack only")
    void should_send_medium_severity_alert_via_slack_only() {
        // Given: Medium severity security alert
        var alertTitle = "Rate Limit Exceeded";
        var severity = "MEDIUM";
        var description = "Participant exceeded API rate limits";
        
        // When: Sending medium severity security alert
        var futureResult = alertingService.sendSecurityAlert(
            alertTitle, TEST_PARTICIPANT_ID, severity, description
        );
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have sent to Slack only for medium severity
        verify(slackService).sendSecurityAlert(any(SecurityAlert.class), anyString());
        verify(webhookService).sendSecurityAlert(any(SecurityAlert.class), anyList());
        
        // But: Should not have sent email or SMS for medium severity
        verify(emailService, never()).sendSecurityAlert(any(), any());
        verify(smsService, never()).sendCriticalAlert(any(), any());
    }

    // === TDD: FAPI Security Violation Tests ===

    @Test
    @DisplayName("Given FAPI security violation, When sending alert, Then should include technical details")
    void should_send_fapi_security_violation_with_details() {
        // Given: FAPI security violation
        var violationType = "INVALID_DPOP_TOKEN";
        var endpoint = "/open-finance/v1/accounts";
        var details = "DPoP token signature verification failed";
        
        // When: Sending FAPI security violation alert
        var futureResult = alertingService.sendFAPISecurityViolation(
            violationType, TEST_PARTICIPANT, endpoint, details
        );
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have sent with HIGH severity (FAPI violations are serious)
        verify(emailService).sendSecurityAlert(
            argThat(alert -> 
                alert.getSeverity() == AlertSeverity.HIGH &&
                alert.getParticipantId().equals(TEST_PARTICIPANT_ID) &&
                alert.getDescription().contains("FAPI 2.0 security violation") &&
                alert.getDescription().contains(violationType) &&
                alert.getDescription().contains(endpoint) &&
                alert.getDescription().contains(details)
            ),
            anyList()
        );
        
        verify(slackService).sendSecurityAlert(any(SecurityAlert.class), anyString());
    }

    // === TDD: Compliance Alerts Tests ===

    @Test
    @DisplayName("Given compliance alert, When sending alert, Then should notify compliance team")
    void should_send_compliance_alert_to_compliance_team() {
        // Given: Compliance violation alert
        var alertTitle = "PCI-DSS Compliance Violation";
        var complianceStatus = "NON_COMPLIANT_MAJOR";
        var details = "Encryption controls failed validation";
        
        // When: Sending compliance alert
        var futureResult = alertingService.sendComplianceAlert(
            alertTitle, TEST_PARTICIPANT_ID, complianceStatus, details
        );
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have sent to compliance team channels
        verify(emailService).sendComplianceAlert(any(ComplianceAlert.class), anyList());
        verify(slackService).sendComplianceAlert(any(ComplianceAlert.class), anyString());
        verify(webhookService).sendComplianceAlert(any(ComplianceAlert.class), anyList());
        
        // And: Should escalate major non-compliance to management
        verify(emailService).sendComplianceEscalation(any(ComplianceAlert.class), anyList());
        verify(smsService).sendComplianceAlert(any(ComplianceAlert.class), anyList());
    }

    @Test
    @DisplayName("Given PCI compliance violation, When sending alert, Then should include compliance score")
    void should_send_pci_compliance_violation_with_score() {
        // Given: PCI compliance violation with specific score
        var violationType = "ENCRYPTION_FAILURE";
        var participantId = TEST_PARTICIPANT_ID;
        var complianceScore = 65.5;
        
        // When: Sending PCI compliance violation alert
        var futureResult = alertingService.sendPCIComplianceViolation(
            violationType, participantId, complianceScore
        );
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have included compliance score in description
        verify(emailService).sendComplianceAlert(
            argThat(alert ->
                alert.getDescription().contains("PCI-DSS v4 compliance violation") &&
                alert.getDescription().contains(violationType) &&
                alert.getDescription().contains("65.50%") &&
                alert.getComplianceStatus() == ComplianceStatus.NON_COMPLIANT_MAJOR
            ),
            anyList()
        );
    }

    // === TDD: Performance Alerts Tests ===

    @Test
    @DisplayName("Given performance alert, When sending alert, Then should notify operations team")
    void should_send_performance_alert_to_operations_team() {
        // Given: Performance degradation alert
        var alertTitle = "API Response Time Degradation";
        var component = "DataSharingSaga";
        var metric = "response_time_p95";
        var threshold = "2000ms";
        var currentValue = "3500ms";
        
        // When: Sending performance alert
        var futureResult = alertingService.sendPerformanceAlert(
            alertTitle, component, metric, threshold, currentValue
        );
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have sent to operations team
        verify(slackService).sendPerformanceAlert(any(PerformanceAlert.class), anyString());
        verify(webhookService).sendPerformanceAlert(any(PerformanceAlert.class), anyList());
        
        // And: Should have sent email for HIGH severity performance issues
        verify(emailService).sendPerformanceAlert(any(PerformanceAlert.class), anyList());
    }

    @Test
    @DisplayName("Given data sharing failure, When sending alert, Then should include saga details")
    void should_send_data_sharing_failure_alert_with_saga_details() {
        // Given: Data sharing saga failure
        var sagaId = "SAGA-123456789";
        var platforms = new String[]{"ENTERPRISE_LOANS", "AMANAHFI_PLATFORM", "MASRUFI_FRAMEWORK"};
        var failureReason = "Enterprise loan service timeout after 2 minutes";
        
        // When: Sending data sharing failure alert
        var futureResult = alertingService.sendDataSharingFailureAlert(
            sagaId, platforms, failureReason, TEST_PARTICIPANT
        );
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have created performance alert for data sharing failure
        verify(slackService).sendPerformanceAlert(
            argThat(alert ->
                alert.getTitle().equals("Data Sharing Failure") &&
                alert.getComponent().equals("DataSharingSaga")
            ),
            anyString()
        );
    }

    // === TDD: Report Distribution Tests ===

    @Test
    @DisplayName("Given compliance report, When distributing report, Then should send to appropriate stakeholders")
    void should_distribute_compliance_report_to_stakeholders() {
        // Given: Daily compliance report
        var report = ComplianceReport.builder()
            .reportId("DAILY-2024-01-15")
            .reportType(ComplianceReportType.DAILY_SUMMARY)
            .hasSignificantViolations(true)
            .build();
        
        when(alertConfigService.getReportDistributionConfiguration())
            .thenReturn(createMockReportDistributionConfig());
        
        // When: Distributing compliance report
        var futureResult = alertingService.sendComplianceReport(report);
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have sent daily report to compliance team
        verify(emailService).sendDailyComplianceReport(eq(report), anyList());
        
        // And: Should have sent executive summary due to significant violations
        verify(emailService).sendExecutiveComplianceSummary(eq(report), anyList());
    }

    // === TDD: Alert Management Tests ===

    @Test
    @DisplayName("Given alert acknowledgment, When acknowledging alert, Then should record acknowledgment")
    void should_record_alert_acknowledgment() {
        // Given: Alert acknowledgment request
        var alertId = "SEC-1234567890";
        var acknowledgedBy = "security.team@enterprise.com";
        var notes = "Investigating suspicious activity patterns";
        
        when(alertConfigService.getGeneralAlertConfiguration())
            .thenReturn(createMockGeneralAlertConfig());
        
        // When: Acknowledging alert
        var futureResult = alertingService.acknowledgeAlert(alertId, acknowledgedBy, notes);
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have recorded acknowledgment
        verify(alertConfigService).recordAlertAcknowledgment(
            argThat(ack ->
                ack.getAlertId().equals(alertId) &&
                ack.getAcknowledgedBy().equals(acknowledgedBy) &&
                ack.getNotes().equals(notes)
            )
        );
        
        // And: Should have notified team of acknowledgment
        verify(slackService).sendAlertAcknowledgment(any(AlertAcknowledgment.class), anyString());
    }

    @Test
    @DisplayName("Given alert resolution, When resolving alert, Then should record resolution")
    void should_record_alert_resolution() {
        // Given: Alert resolution request
        var alertId = "COMP-9876543210";
        var resolvedBy = "compliance.officer@enterprise.com";
        var resolution = "Updated encryption configuration to meet PCI-DSS requirements";
        
        when(alertConfigService.getGeneralAlertConfiguration())
            .thenReturn(createMockGeneralAlertConfig());
        
        // When: Resolving alert
        var futureResult = alertingService.resolveAlert(alertId, resolvedBy, resolution);
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have recorded resolution
        verify(alertConfigService).recordAlertResolution(
            argThat(res ->
                res.getAlertId().equals(alertId) &&
                res.getResolvedBy().equals(resolvedBy) &&
                res.getResolution().equals(resolution)
            )
        );
        
        // And: Should have notified team of resolution
        verify(slackService).sendAlertResolution(any(AlertResolution.class), anyString());
    }

    @Test
    @DisplayName("Given alert escalation, When escalating alert, Then should notify higher level")
    void should_escalate_alert_to_higher_level() {
        // Given: Alert escalation request
        var alertId = "SEC-CRITICAL-001";
        var escalationReason = "No response from Level 1 team after 30 minutes";
        
        when(alertConfigService.getEscalationConfiguration())
            .thenReturn(createMockEscalationConfig());
        
        // When: Escalating alert
        var futureResult = alertingService.escalateAlert(alertId, escalationReason);
        
        // Then: Should complete successfully
        assertThat(futureResult).succeedsWithin(3, TimeUnit.SECONDS);
        
        // And: Should have recorded escalation
        verify(alertConfigService).recordAlertEscalation(
            argThat(esc ->
                esc.getAlertId().equals(alertId) &&
                esc.getEscalationReason().equals(escalationReason) &&
                esc.getEscalationLevel() == AlertEscalationLevel.LEVEL_2
            )
        );
        
        // And: Should have notified Level 2 team
        verify(emailService).sendAlertEscalation(any(AlertEscalation.class), anyList());
        verify(smsService).sendAlertEscalation(any(AlertEscalation.class), anyList());
    }

    // === Helper Methods for Mock Configurations ===

    private AlertConfiguration createMockSecurityAlertConfig() {
        return AlertConfiguration.builder()
            .securityTeamEmails(java.util.List.of("security@enterprise.com"))
            .securitySlackChannel("#security-alerts")
            .securityWebhooks(java.util.List.of("https://security.webhooks.enterprise.com"))
            .onCallNumbers(java.util.List.of("+1234567890"))
            .build();
    }

    private AlertConfiguration createMockComplianceAlertConfig() {
        return AlertConfiguration.builder()
            .complianceTeamEmails(java.util.List.of("compliance@enterprise.com"))
            .complianceSlackChannel("#compliance-alerts")
            .regulatoryWebhooks(java.util.List.of("https://regulatory.webhooks.enterprise.com"))
            .managementEmails(java.util.List.of("management@enterprise.com"))
            .managementPhones(java.util.List.of("+1234567891"))
            .build();
    }

    private AlertConfiguration createMockPerformanceAlertConfig() {
        return AlertConfiguration.builder()
            .operationsSlackChannel("#ops-alerts")
            .operationsTeamEmails(java.util.List.of("operations@enterprise.com"))
            .monitoringWebhooks(java.util.List.of("https://monitoring.webhooks.enterprise.com"))
            .build();
    }

    private AlertConfiguration createMockReportDistributionConfig() {
        return AlertConfiguration.builder()
            .complianceTeamEmails(java.util.List.of("compliance@enterprise.com"))
            .regulatoryWebhooks(java.util.List.of("https://regulatory.reporting.enterprise.com"))
            .executiveEmails(java.util.List.of("executives@enterprise.com"))
            .build();
    }

    private AlertConfiguration createMockGeneralAlertConfig() {
        return AlertConfiguration.builder()
            .generalSlackChannel("#general-alerts")
            .build();
    }

    private AlertConfiguration createMockEscalationConfig() {
        return AlertConfiguration.builder()
            .level2Emails(java.util.List.of("level2@enterprise.com"))
            .level2Phones(java.util.List.of("+1234567892"))
            .build();
    }
}