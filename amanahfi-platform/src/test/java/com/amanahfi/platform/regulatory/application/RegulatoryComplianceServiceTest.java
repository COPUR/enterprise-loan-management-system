package com.amanahfi.platform.regulatory.application;

import com.amanahfi.platform.regulatory.domain.*;
import com.amanahfi.platform.regulatory.port.in.*;
import com.amanahfi.platform.regulatory.port.out.ComplianceRepository;
import com.amanahfi.platform.regulatory.port.out.RegulatoryApiClient;
import com.amanahfi.platform.shared.command.CommandMetadata;
import com.amanahfi.platform.shared.domain.DomainEventPublisher;
import com.amanahfi.platform.shared.idempotence.IdempotencyKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for RegulatoryComplianceService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Regulatory Compliance Service Tests")
class RegulatoryComplianceServiceTest {
    
    @Mock
    private ComplianceRepository complianceRepository;
    
    @Mock
    private RegulatoryApiClient regulatoryApiClient;
    
    @Mock
    private DomainEventPublisher eventPublisher;
    
    private RegulatoryComplianceService service;
    
    @BeforeEach
    void setUp() {
        service = new RegulatoryComplianceService(
            complianceRepository, regulatoryApiClient, eventPublisher
        );
    }
    
    @Nested
    @DisplayName("Creating Compliance")
    class CreatingCompliance {
        
        @Test
        @DisplayName("Should create new compliance successfully")
        void shouldCreateNewComplianceSuccessfully() {
            // Given
            CreateComplianceCommand command = CreateComplianceCommand.builder()
                .idempotencyKey(IdempotencyKey.of("key-001"))
                .metadata(CommandMetadata.create())
                .entityId("ENTITY-001")
                .complianceType(ComplianceType.OPEN_FINANCE_API)
                .jurisdiction(Jurisdiction.UAE)
                .build();
            
            when(complianceRepository.existsByEntityAndType(
                "ENTITY-001", ComplianceType.OPEN_FINANCE_API))
                .thenReturn(false);
            
            // When
            ComplianceId result = service.createCompliance(command);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getValue()).startsWith("COMP-");
            
            verify(complianceRepository).save(any(RegulatoryCompliance.class));
            verify(eventPublisher).publishAll(anyList());
        }
        
        @Test
        @DisplayName("Should reject duplicate compliance creation")
        void shouldRejectDuplicateComplianceCreation() {
            // Given
            CreateComplianceCommand command = CreateComplianceCommand.builder()
                .idempotencyKey(IdempotencyKey.of("key-001"))
                .metadata(CommandMetadata.create())
                .entityId("ENTITY-001")
                .complianceType(ComplianceType.OPEN_FINANCE_API)
                .jurisdiction(Jurisdiction.UAE)
                .build();
            
            when(complianceRepository.existsByEntityAndType(
                "ENTITY-001", ComplianceType.OPEN_FINANCE_API))
                .thenReturn(true);
            
            // When & Then
            assertThatThrownBy(() -> service.createCompliance(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Compliance already exists");
            
            verify(complianceRepository, never()).save(any());
            verify(eventPublisher, never()).publishAll(anyList());
        }
        
        @Test
        @DisplayName("Should reject invalid command")
        void shouldRejectInvalidCommand() {
            // Given
            CreateComplianceCommand command = CreateComplianceCommand.builder()
                .idempotencyKey(IdempotencyKey.of("key-001"))
                .metadata(CommandMetadata.create())
                .entityId("") // Invalid empty entity ID
                .complianceType(ComplianceType.OPEN_FINANCE_API)
                .jurisdiction(Jurisdiction.UAE)
                .build();
            
            // When & Then
            assertThatThrownBy(() -> service.createCompliance(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Entity ID cannot be empty");
        }
    }
    
    @Nested
    @DisplayName("Performing Assessments")
    class PerformingAssessments {
        
        @Test
        @DisplayName("Should perform CBUAE assessment successfully")
        void shouldPerformCbuaeAssessmentSuccessfully() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            PerformAssessmentCommand command = createPerformAssessmentCommand(
                complianceId, RegulatoryAuthority.CBUAE
            );
            
            RegulatoryCompliance compliance = createMockCompliance(complianceId, Jurisdiction.UAE);
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.of(compliance));
            
            // When
            service.performAssessment(command);
            
            // Then
            verify(complianceRepository).save(compliance);
            verify(eventPublisher).publishAll(anyList());
            verify(regulatoryApiClient).submitAssessment(
                eq(RegulatoryAuthority.CBUAE), any(), any()
            );
        }
        
        @Test
        @DisplayName("Should handle non-UAE authority assessment")
        void shouldHandleNonUaeAuthorityAssessment() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            PerformAssessmentCommand command = createPerformAssessmentCommand(
                complianceId, RegulatoryAuthority.SAMA
            );
            
            RegulatoryCompliance compliance = createMockCompliance(complianceId, Jurisdiction.SAUDI_ARABIA);
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.of(compliance));
            
            // When
            service.performAssessment(command);
            
            // Then
            verify(complianceRepository).save(compliance);
            verify(eventPublisher).publishAll(anyList());
            verify(regulatoryApiClient, never()).submitAssessment(any(), any(), any());
        }
        
        @Test
        @DisplayName("Should reject assessment for non-existent compliance")
        void shouldRejectAssessmentForNonExistentCompliance() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            PerformAssessmentCommand command = createPerformAssessmentCommand(
                complianceId, RegulatoryAuthority.CBUAE
            );
            
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.empty());
            
            // When & Then
            assertThatThrownBy(() -> service.performAssessment(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Compliance not found");
        }
    }
    
    @Nested
    @DisplayName("Submitting Reports")
    class SubmittingReports {
        
        @Test
        @DisplayName("Should submit CBUAE AML report successfully")
        void shouldSubmitCbuaeAmlReportSuccessfully() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            SubmitReportCommand command = createSubmitReportCommand(
                complianceId, ComplianceReport.ReportType.QUARTERLY_COMPLIANCE, RegulatoryAuthority.CBUAE
            );
            
            RegulatoryCompliance compliance = createMockCompliance(complianceId, Jurisdiction.UAE);
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.of(compliance));
            when(regulatoryApiClient.submitReport(any(), any(), any()))
                .thenReturn("CBUAE-ACK-12345");
            
            // When
            service.submitReport(command);
            
            // Then
            verify(complianceRepository, times(2)).save(compliance);
            verify(eventPublisher).publishAll(anyList());
            verify(regulatoryApiClient).submitReport(
                eq(RegulatoryAuthority.CBUAE), any(), eq(command.getReportData())
            );
        }
        
        @Test
        @DisplayName("Should submit HSA Islamic finance report successfully")
        void shouldSubmitHsaIslamicFinanceReportSuccessfully() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            SubmitReportCommand command = createSubmitReportCommand(
                complianceId, ComplianceReport.ReportType.SHARIA_COMPLIANCE, RegulatoryAuthority.HSA
            );
            
            RegulatoryCompliance compliance = createMockCompliance(complianceId, Jurisdiction.UAE);
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.of(compliance));
            when(regulatoryApiClient.submitReport(any(), any(), any()))
                .thenReturn("HSA-CERT-54321");
            
            // When
            service.submitReport(command);
            
            // Then
            verify(regulatoryApiClient).submitReport(
                eq(RegulatoryAuthority.HSA), any(), eq(command.getReportData())
            );
        }
        
        @Test
        @DisplayName("Should handle report submission with invalid period")
        void shouldHandleReportSubmissionWithInvalidPeriod() {
            // Given
            SubmitReportCommand command = SubmitReportCommand.builder()
                .idempotencyKey(IdempotencyKey.of("key-001"))
                .metadata(CommandMetadata.create())
                .complianceId(ComplianceId.generate())
                .reportType(ComplianceReport.ReportType.QUARTERLY_COMPLIANCE)
                .authority(RegulatoryAuthority.CBUAE)
                .periodStart(LocalDate.now())
                .periodEnd(LocalDate.now().minusDays(1)) // Invalid: end before start
                .submittedBy("USER-001")
                .reportData(Map.of())
                .build();
            
            // When & Then
            assertThatThrownBy(() -> service.submitReport(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Period end date must be after start date");
        }
    }
    
    @Nested
    @DisplayName("Recording Violations")
    class RecordingViolations {
        
        @Test
        @DisplayName("Should record critical violation and notify regulator")
        void shouldRecordCriticalViolationAndNotifyRegulator() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            RecordViolationCommand command = createRecordViolationCommand(
                complianceId, ViolationSeverity.CRITICAL
            );
            
            RegulatoryCompliance compliance = createMockCompliance(complianceId, Jurisdiction.UAE);
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.of(compliance));
            when(regulatoryApiClient.notifyViolation(any(), any(), any()))
                .thenReturn("NOTIF-12345");
            
            // When
            service.recordViolation(command);
            
            // Then
            verify(complianceRepository).save(compliance);
            verify(eventPublisher).publishAll(anyList());
            verify(regulatoryApiClient).notifyViolation(any(), any(), any());
        }
        
        @Test
        @DisplayName("Should record low severity violation without notification")
        void shouldRecordLowSeverityViolationWithoutNotification() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            RecordViolationCommand command = createRecordViolationCommand(
                complianceId, ViolationSeverity.LOW
            );
            
            RegulatoryCompliance compliance = createMockCompliance(complianceId, Jurisdiction.UAE);
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.of(compliance));
            
            // When
            service.recordViolation(command);
            
            // Then
            verify(complianceRepository).save(compliance);
            verify(eventPublisher).publishAll(anyList());
            verify(regulatoryApiClient, never()).notifyViolation(any(), any(), any());
        }
    }
    
    @Nested
    @DisplayName("Remediating Violations")
    class RemediatingViolations {
        
        @Test
        @DisplayName("Should remediate violation with regulatory notification")
        void shouldRemediateViolationWithRegulatoryNotification() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            RemediateViolationCommand command = createRemediateViolationCommand(
                complianceId, true
            );
            
            RegulatoryCompliance compliance = createMockCompliance(complianceId, Jurisdiction.UAE);
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.of(compliance));
            when(regulatoryApiClient.notifyRemediation(any(), any(), any()))
                .thenReturn("REM-NOTIF-12345");
            
            // When
            service.remediateViolation(command);
            
            // Then
            verify(complianceRepository).save(compliance);
            verify(eventPublisher).publishAll(anyList());
            verify(regulatoryApiClient).notifyRemediation(
                eq(Jurisdiction.UAE), eq("VIOL-001"), any()
            );
        }
        
        @Test
        @DisplayName("Should remediate violation without regulatory notification")
        void shouldRemediateViolationWithoutRegulatoryNotification() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            RemediateViolationCommand command = createRemediateViolationCommand(
                complianceId, false
            );
            
            RegulatoryCompliance compliance = createMockCompliance(complianceId, Jurisdiction.UAE);
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.of(compliance));
            
            // When
            service.remediateViolation(command);
            
            // Then
            verify(complianceRepository).save(compliance);
            verify(eventPublisher).publishAll(anyList());
            verify(regulatoryApiClient, never()).notifyRemediation(any(), any(), any());
        }
    }
    
    @Nested
    @DisplayName("Query Operations")
    class QueryOperations {
        
        @Test
        @DisplayName("Should get compliance summary")
        void shouldGetComplianceSummary() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            RegulatoryCompliance compliance = createMockCompliance(complianceId, Jurisdiction.UAE);
            when(complianceRepository.findById(complianceId))
                .thenReturn(Optional.of(compliance));
            
            // When
            ComplianceSummary result = service.getComplianceSummary(complianceId);
            
            // Then
            assertThat(result).isNotNull();
            assertThat(result.getComplianceId()).isEqualTo(complianceId);
        }
        
        @Test
        @DisplayName("Should get compliance by entity")
        void shouldGetComplianceByEntity() {
            // Given
            String entityId = "ENTITY-001";
            RegulatoryCompliance compliance1 = createMockCompliance(ComplianceId.generate(), Jurisdiction.UAE);
            RegulatoryCompliance compliance2 = createMockCompliance(ComplianceId.generate(), Jurisdiction.UAE);
            when(complianceRepository.findByEntityId(entityId))
                .thenReturn(List.of(compliance1, compliance2));
            
            // When
            List<ComplianceSummary> result = service.getComplianceByEntity(entityId);
            
            // Then
            assertThat(result).hasSize(2);
        }
    }
    
    // Helper methods
    
    private PerformAssessmentCommand createPerformAssessmentCommand(
            ComplianceId complianceId, RegulatoryAuthority authority) {
        return PerformAssessmentCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .complianceId(complianceId)
            .authority(authority)
            .result(AssessmentResult.PASS)
            .score(90.0)
            .assessorId("ASSESSOR-001")
            .findings(List.of("Finding 1"))
            .recommendations(List.of("Recommendation 1"))
            .pendingRequirements(List.of())
            .build();
    }
    
    private SubmitReportCommand createSubmitReportCommand(
            ComplianceId complianceId, 
            ComplianceReport.ReportType reportType,
            RegulatoryAuthority authority) {
        return SubmitReportCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .complianceId(complianceId)
            .reportType(reportType)
            .authority(authority)
            .periodStart(LocalDate.now().minusMonths(3))
            .periodEnd(LocalDate.now())
            .submittedBy("USER-001")
            .reportData(Map.of("transactions", 1000, "volume", 50000.0))
            .build();
    }
    
    private RecordViolationCommand createRecordViolationCommand(
            ComplianceId complianceId, ViolationSeverity severity) {
        return RecordViolationCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .complianceId(complianceId)
            .authority(RegulatoryAuthority.CBUAE)
            .severity(severity)
            .violationCode("AML-001")
            .description("AML compliance violation")
            .regulatoryReference("REF-001")
            .detectedBy("SYSTEM")
            .build();
    }
    
    private RemediateViolationCommand createRemediateViolationCommand(
            ComplianceId complianceId, boolean notifyRegulator) {
        return RemediateViolationCommand.builder()
            .idempotencyKey(IdempotencyKey.of("key-001"))
            .metadata(CommandMetadata.create())
            .complianceId(complianceId)
            .violationId("VIOL-001")
            .remediatedBy("ADMIN-001")
            .description("Violation remediated")
            .actionsTaken(List.of("Action 1"))
            .preventiveMeasures(List.of("Measure 1"))
            .evidenceReference("EVIDENCE-001")
            .regulatoryNotificationRequired(notifyRegulator)
            .build();
    }
    
    private RegulatoryCompliance createMockCompliance(ComplianceId complianceId, Jurisdiction jurisdiction) {
        return RegulatoryCompliance.createCompliance(
            complianceId,
            "ENTITY-001",
            ComplianceType.OPEN_FINANCE_API,
            jurisdiction
        );
    }
}