package com.amanahfi.platform.regulatory.domain;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compliance status for a specific regulatory authority
 */
@Getter
@ToString
public class AuthorityCompliance {
    private final RegulatoryAuthority authority;
    private AuthorityComplianceStatus status;
    private Double complianceScore;
    private Instant lastAssessmentDate;
    private String lastAssessmentReference;
    private final List<String> activeViolationIds;
    private final List<String> pendingRequirements;
    
    public AuthorityCompliance(RegulatoryAuthority authority) {
        this.authority = Objects.requireNonNull(authority, "Authority cannot be null");
        this.status = AuthorityComplianceStatus.PENDING;
        this.activeViolationIds = new ArrayList<>();
        this.pendingRequirements = new ArrayList<>();
    }
    
    public void updateAssessment(ComplianceAssessment assessment) {
        Objects.requireNonNull(assessment, "Assessment cannot be null");
        
        if (!assessment.getAuthority().equals(authority)) {
            throw new IllegalArgumentException("Assessment authority mismatch");
        }
        
        this.complianceScore = assessment.getScore();
        this.lastAssessmentDate = assessment.getAssessmentDate();
        this.lastAssessmentReference = assessment.getReferenceNumber();
        
        // Update status based on assessment result
        this.status = mapResultToStatus(assessment.getResult());
        
        // Update pending requirements
        this.pendingRequirements.clear();
        this.pendingRequirements.addAll(assessment.getPendingRequirements());
    }
    
    public void recordViolation(ComplianceViolation violation) {
        if (!violation.getAuthority().equals(authority)) {
            throw new IllegalArgumentException("Violation authority mismatch");
        }
        
        activeViolationIds.add(violation.getViolationId());
        
        // Update status based on violation severity
        if (violation.getSeverity() == ViolationSeverity.CRITICAL) {
            this.status = AuthorityComplianceStatus.NON_COMPLIANT;
        } else if (this.status == AuthorityComplianceStatus.COMPLIANT) {
            this.status = AuthorityComplianceStatus.PARTIALLY_COMPLIANT;
        }
    }
    
    public void remediateViolation(String violationId) {
        activeViolationIds.remove(violationId);
        
        // Re-evaluate status if no active violations
        if (activeViolationIds.isEmpty() && 
            status == AuthorityComplianceStatus.NON_COMPLIANT) {
            this.status = AuthorityComplianceStatus.UNDER_REVIEW;
        }
    }
    
    private AuthorityComplianceStatus mapResultToStatus(AssessmentResult result) {
        return switch (result) {
            case PASS -> AuthorityComplianceStatus.COMPLIANT;
            case PASS_WITH_CONDITIONS -> AuthorityComplianceStatus.PARTIALLY_COMPLIANT;
            case FAIL -> AuthorityComplianceStatus.NON_COMPLIANT;
            case REVIEW_REQUIRED -> AuthorityComplianceStatus.UNDER_REVIEW;
        };
    }
    
    public boolean hasActiveViolations() {
        return !activeViolationIds.isEmpty();
    }
    
    public boolean isPendingAssessment() {
        return status == AuthorityComplianceStatus.PENDING;
    }
}