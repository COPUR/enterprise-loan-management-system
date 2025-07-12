package com.amanahfi.platform.regulatory.domain;

import com.amanahfi.platform.shared.domain.AggregateRoot;
import com.amanahfi.platform.shared.domain.Money;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Regulatory Compliance Aggregate Root
 * Manages compliance with CBUAE, VARA, and HSA regulations
 */
@Getter
@ToString
public class RegulatoryCompliance extends AggregateRoot<ComplianceId> {
    
    private final String entityId;
    private final ComplianceType complianceType;
    private final Jurisdiction jurisdiction;
    private ComplianceStatus status;
    private final Map<RegulatoryAuthority, AuthorityCompliance> authorityCompliances;
    private final List<ComplianceReport> reports;
    private final List<ComplianceViolation> violations;
    private LocalDate lastAssessmentDate;
    private LocalDate nextAssessmentDate;
    private final Instant createdAt;
    private Instant updatedAt;
    
    // Private constructor for builder pattern
    private RegulatoryCompliance(ComplianceId complianceId,
                               String entityId,
                               ComplianceType complianceType,
                               Jurisdiction jurisdiction) {
        super(complianceId);
        this.entityId = Objects.requireNonNull(entityId, "Entity ID cannot be null");
        this.complianceType = Objects.requireNonNull(complianceType, "Compliance type cannot be null");
        this.jurisdiction = Objects.requireNonNull(jurisdiction, "Jurisdiction cannot be null");
        this.status = ComplianceStatus.PENDING_ASSESSMENT;
        this.authorityCompliances = new HashMap<>();
        this.reports = new ArrayList<>();
        this.violations = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        
        initializeAuthorityCompliances();
    }
    
    // Factory method for creating new compliance record
    public static RegulatoryCompliance createCompliance(
            ComplianceId complianceId,
            String entityId,
            ComplianceType complianceType,
            Jurisdiction jurisdiction) {
        
        RegulatoryCompliance compliance = new RegulatoryCompliance(
            complianceId, entityId, complianceType, jurisdiction
        );
        
        compliance.raiseEvent(ComplianceCreatedEvent.builder()
            .complianceId(complianceId.getValue())
            .entityId(entityId)
            .complianceType(complianceType)
            .jurisdiction(jurisdiction)
            .createdAt(compliance.createdAt)
            .build());
            
        return compliance;
    }
    
    // Initialize authority-specific compliance based on jurisdiction
    private void initializeAuthorityCompliances() {
        switch (jurisdiction) {
            case UAE:
                authorityCompliances.put(RegulatoryAuthority.CBUAE, 
                    new AuthorityCompliance(RegulatoryAuthority.CBUAE));
                authorityCompliances.put(RegulatoryAuthority.VARA, 
                    new AuthorityCompliance(RegulatoryAuthority.VARA));
                authorityCompliances.put(RegulatoryAuthority.HSA, 
                    new AuthorityCompliance(RegulatoryAuthority.HSA));
                break;
            case SAUDI_ARABIA:
                authorityCompliances.put(RegulatoryAuthority.SAMA, 
                    new AuthorityCompliance(RegulatoryAuthority.SAMA));
                break;
            case TURKEY:
                authorityCompliances.put(RegulatoryAuthority.BDDK, 
                    new AuthorityCompliance(RegulatoryAuthority.BDDK));
                break;
            case PAKISTAN:
                authorityCompliances.put(RegulatoryAuthority.SBP, 
                    new AuthorityCompliance(RegulatoryAuthority.SBP));
                break;
            default:
                // Add other jurisdictions as needed
                break;
        }
    }
    
    // Perform compliance assessment
    public void performAssessment(ComplianceAssessment assessment) {
        Objects.requireNonNull(assessment, "Assessment cannot be null");
        
        if (status == ComplianceStatus.SUSPENDED) {
            throw new IllegalStateException("Cannot assess suspended compliance");
        }
        
        // Update authority-specific compliance
        AuthorityCompliance authorityCompliance = authorityCompliances.get(assessment.getAuthority());
        if (authorityCompliance == null) {
            throw new IllegalArgumentException("Authority not applicable for jurisdiction: " + assessment.getAuthority());
        }
        
        authorityCompliance.updateAssessment(assessment);
        
        // Update overall status
        updateComplianceStatus();
        
        this.lastAssessmentDate = LocalDate.now();
        this.nextAssessmentDate = calculateNextAssessmentDate(assessment.getAuthority());
        this.updatedAt = Instant.now();
        
        raiseEvent(ComplianceAssessedEvent.builder()
            .complianceId(id.getValue())
            .authority(assessment.getAuthority())
            .assessmentResult(assessment.getResult())
            .score(assessment.getScore())
            .assessedAt(Instant.now())
            .build());
    }
    
    // Submit regulatory report
    public void submitReport(ComplianceReport report) {
        Objects.requireNonNull(report, "Report cannot be null");
        
        if (!isReportingAllowed()) {
            throw new IllegalStateException("Cannot submit report in current status: " + status);
        }
        
        reports.add(report);
        this.updatedAt = Instant.now();
        
        raiseEvent(ComplianceReportSubmittedEvent.builder()
            .complianceId(id.getValue())
            .reportId(report.getReportId())
            .reportType(report.getReportType())
            .authority(report.getAuthority())
            .submittedAt(report.getSubmittedAt())
            .build());
    }
    
    // Record compliance violation
    public void recordViolation(ComplianceViolation violation) {
        Objects.requireNonNull(violation, "Violation cannot be null");
        
        violations.add(violation);
        
        // Update authority compliance
        AuthorityCompliance authorityCompliance = authorityCompliances.get(violation.getAuthority());
        if (authorityCompliance != null) {
            authorityCompliance.recordViolation(violation);
        }
        
        // Check if violation is critical
        if (violation.getSeverity() == ViolationSeverity.CRITICAL) {
            this.status = ComplianceStatus.SUSPENDED;
        } else {
            updateComplianceStatus();
        }
        
        this.updatedAt = Instant.now();
        
        raiseEvent(ComplianceViolationRecordedEvent.builder()
            .complianceId(id.getValue())
            .violationId(violation.getViolationId())
            .authority(violation.getAuthority())
            .severity(violation.getSeverity())
            .description(violation.getDescription())
            .recordedAt(Instant.now())
            .build());
    }
    
    // Remediate violation
    public void remediateViolation(String violationId, RemediationDetails remediation) {
        Objects.requireNonNull(violationId, "Violation ID cannot be null");
        Objects.requireNonNull(remediation, "Remediation details cannot be null");
        
        ComplianceViolation violation = violations.stream()
            .filter(v -> v.getViolationId().equals(violationId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Violation not found: " + violationId));
            
        violation.remediate(remediation);
        
        // Update authority compliance
        AuthorityCompliance authorityCompliance = authorityCompliances.get(violation.getAuthority());
        if (authorityCompliance != null) {
            authorityCompliance.remediateViolation(violationId);
        }
        
        updateComplianceStatus();
        this.updatedAt = Instant.now();
        
        raiseEvent(ComplianceViolationRemediatedEvent.builder()
            .complianceId(id.getValue())
            .violationId(violationId)
            .remediationDetails(remediation)
            .remediatedAt(Instant.now())
            .build());
    }
    
    // Update overall compliance status based on authority compliances
    private void updateComplianceStatus() {
        boolean allCompliant = true;
        boolean anyNonCompliant = false;
        boolean anyUnderReview = false;
        
        for (AuthorityCompliance authCompliance : authorityCompliances.values()) {
            switch (authCompliance.getStatus()) {
                case NON_COMPLIANT:
                    anyNonCompliant = true;
                    allCompliant = false;
                    break;
                case PARTIALLY_COMPLIANT:
                case UNDER_REVIEW:
                    anyUnderReview = true;
                    allCompliant = false;
                    break;
                case PENDING:
                    allCompliant = false;
                    break;
            }
        }
        
        if (anyNonCompliant) {
            this.status = ComplianceStatus.NON_COMPLIANT;
        } else if (anyUnderReview) {
            this.status = ComplianceStatus.UNDER_REVIEW;
        } else if (allCompliant) {
            this.status = ComplianceStatus.COMPLIANT;
        } else {
            this.status = ComplianceStatus.PARTIALLY_COMPLIANT;
        }
    }
    
    // Calculate next assessment date based on authority requirements
    private LocalDate calculateNextAssessmentDate(RegulatoryAuthority authority) {
        return switch (authority) {
            case CBUAE -> LocalDate.now().plusMonths(3); // Quarterly
            case VARA -> LocalDate.now().plusMonths(1);  // Monthly
            case HSA -> LocalDate.now().plusMonths(6);   // Semi-annual
            case SAMA -> LocalDate.now().plusMonths(3);  // Quarterly
            case BDDK -> LocalDate.now().plusMonths(3);  // Quarterly
            case SBP -> LocalDate.now().plusMonths(3);   // Quarterly
            default -> LocalDate.now().plusMonths(3);
        };
    }
    
    // Check if reporting is allowed in current status
    private boolean isReportingAllowed() {
        return status != ComplianceStatus.SUSPENDED && 
               status != ComplianceStatus.PENDING_ASSESSMENT;
    }
    
    // Get compliance score for specific authority
    public Optional<Double> getComplianceScore(RegulatoryAuthority authority) {
        AuthorityCompliance authCompliance = authorityCompliances.get(authority);
        return authCompliance != null ? 
            Optional.ofNullable(authCompliance.getComplianceScore()) : 
            Optional.empty();
    }
    
    // Get active violations count
    public long getActiveViolationsCount() {
        return violations.stream()
            .filter(v -> !v.isRemediated())
            .count();
    }
    
    // Get compliance summary
    public ComplianceSummary getComplianceSummary() {
        return ComplianceSummary.builder()
            .complianceId(id)
            .entityId(entityId)
            .complianceType(complianceType)
            .jurisdiction(jurisdiction)
            .overallStatus(status)
            .authorityStatuses(getAuthorityStatuses())
            .activeViolations(getActiveViolationsCount())
            .lastAssessmentDate(lastAssessmentDate)
            .nextAssessmentDate(nextAssessmentDate)
            .build();
    }
    
    private Map<RegulatoryAuthority, AuthorityComplianceStatus> getAuthorityStatuses() {
        Map<RegulatoryAuthority, AuthorityComplianceStatus> statuses = new HashMap<>();
        authorityCompliances.forEach((authority, compliance) -> 
            statuses.put(authority, compliance.getStatus()));
        return statuses;
    }
}