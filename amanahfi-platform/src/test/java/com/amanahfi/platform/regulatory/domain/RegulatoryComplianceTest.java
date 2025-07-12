package com.amanahfi.platform.regulatory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for RegulatoryCompliance aggregate
 */
@DisplayName("Regulatory Compliance Tests")
class RegulatoryComplianceTest {
    
    @Nested
    @DisplayName("Creating Compliance Records")
    class CreatingComplianceRecords {
        
        @Test
        @DisplayName("Should create UAE compliance with CBUAE, VARA, and HSA authorities")
        void shouldCreateUaeCompliance() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            String entityId = "ENTITY-001";
            ComplianceType complianceType = ComplianceType.OPEN_FINANCE_API;
            Jurisdiction jurisdiction = Jurisdiction.UAE;
            
            // When
            RegulatoryCompliance compliance = RegulatoryCompliance.createCompliance(
                complianceId, entityId, complianceType, jurisdiction
            );
            
            // Then
            assertThat(compliance.getId()).isEqualTo(complianceId);
            assertThat(compliance.getEntityId()).isEqualTo(entityId);
            assertThat(compliance.getComplianceType()).isEqualTo(complianceType);
            assertThat(compliance.getJurisdiction()).isEqualTo(jurisdiction);
            assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.PENDING_ASSESSMENT);
            
            // Should initialize UAE authorities
            assertThat(compliance.getAuthorityCompliances()).containsKeys(
                RegulatoryAuthority.CBUAE,
                RegulatoryAuthority.VARA,
                RegulatoryAuthority.HSA
            );
            
            // Should have compliance created event
            assertThat(compliance.getUncommittedEvents()).hasSize(1);
            assertThat(compliance.getUncommittedEvents().get(0))
                .isInstanceOf(com.amanahfi.platform.regulatory.domain.events.ComplianceCreatedEvent.class);
        }
        
        @Test
        @DisplayName("Should create Saudi Arabia compliance with SAMA authority")
        void shouldCreateSaudiArabiaCompliance() {
            // Given
            ComplianceId complianceId = ComplianceId.generate();
            String entityId = "ENTITY-SA-001";
            ComplianceType complianceType = ComplianceType.ISLAMIC_BANKING;
            Jurisdiction jurisdiction = Jurisdiction.SAUDI_ARABIA;
            
            // When
            RegulatoryCompliance compliance = RegulatoryCompliance.createCompliance(
                complianceId, entityId, complianceType, jurisdiction
            );
            
            // Then
            assertThat(compliance.getJurisdiction()).isEqualTo(jurisdiction);
            assertThat(compliance.getAuthorityCompliances()).containsKey(RegulatoryAuthority.SAMA);
            assertThat(compliance.getAuthorityCompliances()).doesNotContainKeys(
                RegulatoryAuthority.CBUAE, RegulatoryAuthority.VARA, RegulatoryAuthority.HSA
            );
        }
        
        @Test
        @DisplayName("Should reject null parameters")
        void shouldRejectNullParameters() {
            ComplianceId complianceId = ComplianceId.generate();
            String entityId = "ENTITY-001";
            ComplianceType complianceType = ComplianceType.OPEN_FINANCE_API;
            Jurisdiction jurisdiction = Jurisdiction.UAE;
            
            assertThatThrownBy(() -> RegulatoryCompliance.createCompliance(
                null, entityId, complianceType, jurisdiction))
                .isInstanceOf(NullPointerException.class);
                
            assertThatThrownBy(() -> RegulatoryCompliance.createCompliance(
                complianceId, null, complianceType, jurisdiction))
                .isInstanceOf(NullPointerException.class);
                
            assertThatThrownBy(() -> RegulatoryCompliance.createCompliance(
                complianceId, entityId, null, jurisdiction))
                .isInstanceOf(NullPointerException.class);
                
            assertThatThrownBy(() -> RegulatoryCompliance.createCompliance(
                complianceId, entityId, complianceType, null))
                .isInstanceOf(NullPointerException.class);
        }
    }
    
    @Nested
    @DisplayName("Performing Assessments")
    class PerformingAssessments {
        
        @Test
        @DisplayName("Should perform successful CBUAE assessment")
        void shouldPerformSuccessfulCbuaeAssessment() {
            // Given
            RegulatoryCompliance compliance = createUaeCompliance();
            ComplianceAssessment assessment = createCbuaeAssessment(AssessmentResult.PASS, 95.0);
            
            // When
            compliance.performAssessment(assessment);
            
            // Then
            assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.PARTIALLY_COMPLIANT);
            assertThat(compliance.getLastAssessmentDate()).isEqualTo(LocalDate.now());
            assertThat(compliance.getNextAssessmentDate()).isEqualTo(LocalDate.now().plusMonths(3));
            
            // Check authority compliance
            assertThat(compliance.getComplianceScore(RegulatoryAuthority.CBUAE))
                .isPresent()
                .hasValue(95.0);
            
            // Should have assessment event
            assertThat(compliance.getUncommittedEvents()).hasSize(2); // Created + Assessed
        }
        
        @Test
        @DisplayName("Should handle failing assessment")
        void shouldHandleFailingAssessment() {
            // Given
            RegulatoryCompliance compliance = createUaeCompliance();
            ComplianceAssessment assessment = createCbuaeAssessment(AssessmentResult.FAIL, 45.0);
            
            // When
            compliance.performAssessment(assessment);
            
            // Then
            assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.NON_COMPLIANT);
        }
        
        @Test
        @DisplayName("Should reject assessment for wrong authority")
        void shouldRejectAssessmentForWrongAuthority() {
            // Given
            RegulatoryCompliance compliance = createTurkeyCompliance();
            ComplianceAssessment assessment = createCbuaeAssessment(AssessmentResult.PASS, 90.0);
            
            // When & Then
            assertThatThrownBy(() -> compliance.performAssessment(assessment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authority not applicable for jurisdiction");
        }
        
        @Test
        @DisplayName("Should reject assessment when suspended")
        void shouldRejectAssessmentWhenSuspended() {
            // Given
            RegulatoryCompliance compliance = createUaeCompliance();
            recordCriticalViolation(compliance);
            assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.SUSPENDED);
            
            ComplianceAssessment assessment = createCbuaeAssessment(AssessmentResult.PASS, 90.0);
            
            // When & Then
            assertThatThrownBy(() -> compliance.performAssessment(assessment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot assess suspended compliance");
        }
    }
    
    @Nested
    @DisplayName("Recording Violations")
    class RecordingViolations {
        
        @Test
        @DisplayName("Should record medium severity violation")
        void shouldRecordMediumSeverityViolation() {
            // Given
            RegulatoryCompliance compliance = createUaeCompliance();
            ComplianceViolation violation = createViolation(ViolationSeverity.MEDIUM, RegulatoryAuthority.CBUAE);
            
            // When
            compliance.recordViolation(violation);
            
            // Then
            assertThat(compliance.getActiveViolationsCount()).isEqualTo(1);
            assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.PARTIALLY_COMPLIANT);
            
            // Should have violation event
            assertThat(compliance.getUncommittedEvents()).hasSize(2); // Created + Violation
        }
        
        @Test
        @DisplayName("Should record critical violation and suspend compliance")
        void shouldRecordCriticalViolationAndSuspend() {
            // Given
            RegulatoryCompliance compliance = createUaeCompliance();
            ComplianceViolation violation = createViolation(ViolationSeverity.CRITICAL, RegulatoryAuthority.VARA);
            
            // When
            compliance.recordViolation(violation);
            
            // Then
            assertThat(compliance.getActiveViolationsCount()).isEqualTo(1);
            assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.SUSPENDED);
        }
        
        @Test
        @DisplayName("Should handle HSA Sharia violation")
        void shouldHandleHsaShariaViolation() {
            // Given
            RegulatoryCompliance compliance = createUaeCompliance();
            ComplianceViolation violation = ComplianceViolation.builder()
                .violationId("HSA-VIOL-001")
                .authority(RegulatoryAuthority.HSA)
                .severity(ViolationSeverity.HIGH)
                .violationCode("RIBA-001")
                .description("Interest-based transaction detected")
                .regulatoryReference("HSA-REF-2024-001")
                .detectedAt(Instant.now())
                .detectedBy("SHARIA-AUDIT-SYSTEM")
                .status(ViolationStatus.DETECTED)
                .build();
            
            // When
            compliance.recordViolation(violation);
            
            // Then
            assertThat(compliance.getActiveViolationsCount()).isEqualTo(1);
            assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.NON_COMPLIANT);
        }
    }
    
    @Nested
    @DisplayName("Remediating Violations")
    class RemediatingViolations {
        
        @Test
        @DisplayName("Should remediate violation successfully")
        void shouldRemediateViolationSuccessfully() {
            // Given
            RegulatoryCompliance compliance = createUaeCompliance();
            ComplianceViolation violation = createViolation(ViolationSeverity.MEDIUM, RegulatoryAuthority.CBUAE);
            compliance.recordViolation(violation);
            
            RemediationDetails remediation = createRemediationDetails();
            
            // When
            compliance.remediateViolation(violation.getViolationId(), remediation);
            
            // Then
            assertThat(compliance.getActiveViolationsCount()).isEqualTo(0);
            assertThat(compliance.getStatus()).isEqualTo(ComplianceStatus.UNDER_REVIEW);
            
            // Should have remediation event
            assertThat(compliance.getUncommittedEvents()).hasSize(3); // Created + Violation + Remediation
        }
        
        @Test
        @DisplayName("Should reject remediation for non-existent violation")
        void shouldRejectRemediationForNonExistentViolation() {
            // Given
            RegulatoryCompliance compliance = createUaeCompliance();
            RemediationDetails remediation = createRemediationDetails();
            
            // When & Then
            assertThatThrownBy(() -> compliance.remediateViolation("NON-EXISTENT", remediation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Violation not found");
        }
    }
    
    @Nested
    @DisplayName("Compliance Summary")
    class ComplianceSummary {
        
        @Test
        @DisplayName("Should provide comprehensive compliance summary")
        void shouldProvideComprehensiveComplianceSummary() {
            // Given
            RegulatoryCompliance compliance = createUaeCompliance();
            performMixedAssessments(compliance);
            
            // When
            com.amanahfi.platform.regulatory.domain.ComplianceSummary summary = 
                compliance.getComplianceSummary();
            
            // Then
            assertThat(summary.getComplianceId()).isEqualTo(compliance.getId());
            assertThat(summary.getEntityId()).isEqualTo(compliance.getEntityId());
            assertThat(summary.getJurisdiction()).isEqualTo(Jurisdiction.UAE);
            assertThat(summary.getAuthorityStatuses()).hasSize(3);
            assertThat(summary.getLastAssessmentDate()).isEqualTo(LocalDate.now());
        }
    }
    
    // Helper methods
    
    private RegulatoryCompliance createUaeCompliance() {
        return RegulatoryCompliance.createCompliance(
            ComplianceId.generate(),
            "ENTITY-UAE-001",
            ComplianceType.OPEN_FINANCE_API,
            Jurisdiction.UAE
        );
    }
    
    private RegulatoryCompliance createTurkeyCompliance() {
        return RegulatoryCompliance.createCompliance(
            ComplianceId.generate(),
            "ENTITY-TR-001",
            ComplianceType.ANTI_MONEY_LAUNDERING,
            Jurisdiction.TURKEY
        );
    }
    
    private ComplianceAssessment createCbuaeAssessment(AssessmentResult result, Double score) {
        return ComplianceAssessment.builder()
            .authority(RegulatoryAuthority.CBUAE)
            .result(result)
            .score(score)
            .referenceNumber("CBUAE-ASSESS-001")
            .assessmentDate(Instant.now())
            .assessorId("ASSESSOR-001")
            .findings(List.of("Finding 1", "Finding 2"))
            .recommendations(List.of("Recommendation 1"))
            .pendingRequirements(List.of("Requirement 1"))
            .validUntil(Instant.now().plusSeconds(90L * 24 * 60 * 60))
            .build();
    }
    
    private ComplianceViolation createViolation(ViolationSeverity severity, RegulatoryAuthority authority) {
        return ComplianceViolation.builder()
            .violationId("VIOL-001")
            .authority(authority)
            .severity(severity)
            .violationCode("AML-001")
            .description("AML compliance violation")
            .regulatoryReference("REF-001")
            .detectedAt(Instant.now())
            .detectedBy("SYSTEM")
            .status(ViolationStatus.DETECTED)
            .build();
    }
    
    private void recordCriticalViolation(RegulatoryCompliance compliance) {
        ComplianceViolation criticalViolation = createViolation(ViolationSeverity.CRITICAL, RegulatoryAuthority.VARA);
        compliance.recordViolation(criticalViolation);
    }
    
    private RemediationDetails createRemediationDetails() {
        return RemediationDetails.builder()
            .remediationId("REM-001")
            .remediatedBy("ADMIN-001")
            .remediatedAt(Instant.now())
            .remediationDescription("Violation remediated")
            .actionsTaken(List.of("Action 1", "Action 2"))
            .preventiveMeasures(List.of("Measure 1"))
            .evidenceReference("EVIDENCE-001")
            .regulatoryNotificationRequired(false)
            .build();
    }
    
    private void performMixedAssessments(RegulatoryCompliance compliance) {
        // CBUAE assessment
        compliance.performAssessment(createCbuaeAssessment(AssessmentResult.PASS, 90.0));
        
        // VARA assessment
        ComplianceAssessment varaAssessment = ComplianceAssessment.builder()
            .authority(RegulatoryAuthority.VARA)
            .result(AssessmentResult.PASS_WITH_CONDITIONS)
            .score(85.0)
            .referenceNumber("VARA-ASSESS-001")
            .assessmentDate(Instant.now())
            .assessorId("VARA-ASSESSOR-001")
            .findings(List.of("VARA Finding"))
            .recommendations(List.of("VARA Recommendation"))
            .pendingRequirements(List.of("VARA Requirement"))
            .validUntil(Instant.now().plusSeconds(30L * 24 * 60 * 60))
            .build();
        compliance.performAssessment(varaAssessment);
    }
}