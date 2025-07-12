package com.amanahfi.platform.regulatory.application;

import com.amanahfi.platform.regulatory.domain.*;
import com.amanahfi.platform.regulatory.domain.events.*;
import com.amanahfi.platform.regulatory.port.in.*;
import com.amanahfi.platform.regulatory.port.out.ComplianceRepository;
import com.amanahfi.platform.regulatory.port.out.RegulatoryApiClient;
import com.amanahfi.platform.shared.domain.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Application service for regulatory compliance management
 * Implements CBUAE, VARA, and HSA compliance requirements
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class RegulatoryComplianceService implements RegulatoryComplianceUseCase {
    
    private final ComplianceRepository complianceRepository;
    private final RegulatoryApiClient regulatoryApiClient;
    private final DomainEventPublisher eventPublisher;
    
    @Override
    public ComplianceId createCompliance(CreateComplianceCommand command) {
        log.info("Creating regulatory compliance for entity: {} in jurisdiction: {}", 
            command.getEntityId(), command.getJurisdiction());
        
        // Validate command
        command.validate();
        
        // Check for existing compliance
        if (complianceRepository.existsByEntityAndType(command.getEntityId(), command.getComplianceType())) {
            throw new IllegalStateException(
                "Compliance already exists for entity: " + command.getEntityId() + 
                " and type: " + command.getComplianceType()
            );
        }
        
        // Create compliance aggregate
        ComplianceId complianceId = ComplianceId.generate();
        RegulatoryCompliance compliance = RegulatoryCompliance.createCompliance(
            complianceId,
            command.getEntityId(),
            command.getComplianceType(),
            command.getJurisdiction()
        );
        
        // Save compliance
        complianceRepository.save(compliance);
        
        // Publish events
        eventPublisher.publishAll(compliance.getUncommittedEvents());
        compliance.markEventsAsCommitted();
        
        log.info("Created regulatory compliance with ID: {}", complianceId.getValue());
        return complianceId;
    }
    
    @Override
    public void performAssessment(PerformAssessmentCommand command) {
        log.info("Performing compliance assessment for ID: {} by authority: {}", 
            command.getComplianceId(), command.getAuthority());
        
        // Validate command
        command.validate();
        
        // Load compliance aggregate
        RegulatoryCompliance compliance = complianceRepository.findById(command.getComplianceId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Compliance not found: " + command.getComplianceId()
            ));
        
        // Create assessment
        ComplianceAssessment assessment = ComplianceAssessment.builder()
            .authority(command.getAuthority())
            .result(command.getResult())
            .score(command.getScore())
            .referenceNumber("ASSESS-" + UUID.randomUUID())
            .assessmentDate(Instant.now())
            .assessorId(command.getAssessorId())
            .findings(command.getFindings())
            .recommendations(command.getRecommendations())
            .pendingRequirements(command.getPendingRequirements())
            .validUntil(Instant.now().plusSeconds(90L * 24 * 60 * 60)) // 90 days
            .build();
        
        // Perform assessment
        compliance.performAssessment(assessment);
        
        // Save updated compliance
        complianceRepository.save(compliance);
        
        // Publish events
        eventPublisher.publishAll(compliance.getUncommittedEvents());
        compliance.markEventsAsCommitted();
        
        // If authority is CBUAE, VARA, or HSA, submit to regulatory API
        if (isUAEAuthority(command.getAuthority())) {
            submitAssessmentToRegulator(compliance, assessment);
        }
        
        log.info("Completed compliance assessment with result: {}", command.getResult());
    }
    
    @Override
    public void submitReport(SubmitReportCommand command) {
        log.info("Submitting compliance report for ID: {} to authority: {}", 
            command.getComplianceId(), command.getAuthority());
        
        // Validate command
        command.validate();
        
        // Load compliance aggregate
        RegulatoryCompliance compliance = complianceRepository.findById(command.getComplianceId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Compliance not found: " + command.getComplianceId()
            ));
        
        // Create report
        ComplianceReport report = ComplianceReport.builder()
            .reportId("REPORT-" + UUID.randomUUID())
            .reportType(command.getReportType())
            .authority(command.getAuthority())
            .reportingPeriodStart(command.getPeriodStart())
            .reportingPeriodEnd(command.getPeriodEnd())
            .submissionReference("SUB-" + UUID.randomUUID())
            .submittedAt(Instant.now())
            .submittedBy(command.getSubmittedBy())
            .status(ComplianceReport.ReportStatus.SUBMITTED)
            .build();
        
        // Submit report
        compliance.submitReport(report);
        
        // Save updated compliance
        complianceRepository.save(compliance);
        
        // Submit to regulatory API
        String acknowledgmentNumber = regulatoryApiClient.submitReport(
            command.getAuthority(),
            report,
            command.getReportData()
        );
        
        // Update report with acknowledgment
        ComplianceReport acknowledgedReport = ComplianceReport.builder()
            .reportId(report.getReportId())
            .reportType(report.getReportType())
            .authority(report.getAuthority())
            .reportingPeriodStart(report.getReportingPeriodStart())
            .reportingPeriodEnd(report.getReportingPeriodEnd())
            .submissionReference(report.getSubmissionReference())
            .submittedAt(report.getSubmittedAt())
            .submittedBy(report.getSubmittedBy())
            .status(ComplianceReport.ReportStatus.ACKNOWLEDGED)
            .acknowledgmentNumber(acknowledgmentNumber)
            .build();
        
        // Save acknowledged report
        complianceRepository.save(compliance);
        
        // Publish events
        eventPublisher.publishAll(compliance.getUncommittedEvents());
        compliance.markEventsAsCommitted();
        
        log.info("Submitted compliance report with acknowledgment: {}", acknowledgmentNumber);
    }
    
    @Override
    public void recordViolation(RecordViolationCommand command) {
        log.info("Recording compliance violation for ID: {} with severity: {}", 
            command.getComplianceId(), command.getSeverity());
        
        // Validate command
        command.validate();
        
        // Load compliance aggregate
        RegulatoryCompliance compliance = complianceRepository.findById(command.getComplianceId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Compliance not found: " + command.getComplianceId()
            ));
        
        // Create violation
        ComplianceViolation violation = ComplianceViolation.builder()
            .violationId("VIOL-" + UUID.randomUUID())
            .authority(command.getAuthority())
            .severity(command.getSeverity())
            .violationCode(command.getViolationCode())
            .description(command.getDescription())
            .regulatoryReference(command.getRegulatoryReference())
            .detectedAt(Instant.now())
            .detectedBy(command.getDetectedBy())
            .status(ViolationStatus.DETECTED)
            .build();
        
        // Record violation
        compliance.recordViolation(violation);
        
        // Save updated compliance
        complianceRepository.save(compliance);
        
        // Publish events
        eventPublisher.publishAll(compliance.getUncommittedEvents());
        compliance.markEventsAsCommitted();
        
        // Notify regulatory authority if critical
        if (violation.isCritical()) {
            notifyRegulatoryAuthority(compliance, violation);
        }
        
        log.info("Recorded compliance violation with ID: {}", violation.getViolationId());
    }
    
    @Override
    public void remediateViolation(RemediateViolationCommand command) {
        log.info("Remediating violation: {} for compliance ID: {}", 
            command.getViolationId(), command.getComplianceId());
        
        // Validate command
        command.validate();
        
        // Load compliance aggregate
        RegulatoryCompliance compliance = complianceRepository.findById(command.getComplianceId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Compliance not found: " + command.getComplianceId()
            ));
        
        // Create remediation details
        RemediationDetails remediation = RemediationDetails.builder()
            .remediationId("REM-" + UUID.randomUUID())
            .remediatedBy(command.getRemediatedBy())
            .remediatedAt(Instant.now())
            .remediationDescription(command.getDescription())
            .actionsTaken(command.getActionsTaken())
            .preventiveMeasures(command.getPreventiveMeasures())
            .evidenceReference(command.getEvidenceReference())
            .regulatoryNotificationRequired(command.isRegulatoryNotificationRequired())
            .build();
        
        // Remediate violation
        compliance.remediateViolation(command.getViolationId(), remediation);
        
        // Save updated compliance
        complianceRepository.save(compliance);
        
        // Publish events
        eventPublisher.publishAll(compliance.getUncommittedEvents());
        compliance.markEventsAsCommitted();
        
        // Notify regulatory authority if required
        if (command.isRegulatoryNotificationRequired()) {
            String notificationRef = regulatoryApiClient.notifyRemediation(
                compliance.getJurisdiction(),
                command.getViolationId(),
                remediation
            );
            log.info("Notified regulatory authority, reference: {}", notificationRef);
        }
        
        log.info("Remediated violation: {}", command.getViolationId());
    }
    
    @Override
    public ComplianceSummary getComplianceSummary(ComplianceId complianceId) {
        log.debug("Getting compliance summary for ID: {}", complianceId.getValue());
        
        RegulatoryCompliance compliance = complianceRepository.findById(complianceId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Compliance not found: " + complianceId.getValue()
            ));
        
        return compliance.getComplianceSummary();
    }
    
    @Override
    public List<ComplianceSummary> getComplianceByEntity(String entityId) {
        log.debug("Getting compliance records for entity: {}", entityId);
        
        return complianceRepository.findByEntityId(entityId).stream()
            .map(RegulatoryCompliance::getComplianceSummary)
            .toList();
    }
    
    @Override
    public List<ComplianceReport> getReportsByAuthority(
            ComplianceId complianceId, 
            RegulatoryAuthority authority) {
        log.debug("Getting reports for compliance: {} and authority: {}", 
            complianceId.getValue(), authority);
        
        RegulatoryCompliance compliance = complianceRepository.findById(complianceId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Compliance not found: " + complianceId.getValue()
            ));
        
        return compliance.getReports().stream()
            .filter(report -> report.getAuthority().equals(authority))
            .toList();
    }
    
    // Helper methods
    
    private boolean isUAEAuthority(RegulatoryAuthority authority) {
        return authority == RegulatoryAuthority.CBUAE ||
               authority == RegulatoryAuthority.VARA ||
               authority == RegulatoryAuthority.HSA;
    }
    
    private void submitAssessmentToRegulator(
            RegulatoryCompliance compliance, 
            ComplianceAssessment assessment) {
        try {
            regulatoryApiClient.submitAssessment(
                assessment.getAuthority(),
                compliance.getComplianceSummary(),
                assessment
            );
            log.info("Submitted assessment to regulatory authority: {}", 
                assessment.getAuthority());
        } catch (Exception e) {
            log.error("Failed to submit assessment to regulator: {}", 
                assessment.getAuthority(), e);
            // Assessment is still recorded locally
        }
    }
    
    private void notifyRegulatoryAuthority(
            RegulatoryCompliance compliance, 
            ComplianceViolation violation) {
        try {
            String notificationRef = regulatoryApiClient.notifyViolation(
                violation.getAuthority(),
                compliance.getComplianceSummary(),
                violation
            );
            log.info("Notified regulatory authority of critical violation, reference: {}", 
                notificationRef);
        } catch (Exception e) {
            log.error("Failed to notify regulator of violation: {}", 
                violation.getViolationId(), e);
            // Violation is still recorded locally
        }
    }
}