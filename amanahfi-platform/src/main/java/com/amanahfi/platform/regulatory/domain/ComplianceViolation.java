package com.amanahfi.platform.regulatory.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

/**
 * Compliance violation record
 */
@Getter
@ToString
@Builder
public class ComplianceViolation {
    private final String violationId;
    private final RegulatoryAuthority authority;
    private final ViolationSeverity severity;
    private final String violationCode;
    private final String description;
    private final String regulatoryReference;
    private final Instant detectedAt;
    private final String detectedBy;
    private ViolationStatus status;
    private RemediationDetails remediationDetails;
    
    public void remediate(RemediationDetails remediation) {
        Objects.requireNonNull(remediation, "Remediation details cannot be null");
        
        if (status == ViolationStatus.REMEDIATED) {
            throw new IllegalStateException("Violation already remediated");
        }
        
        this.remediationDetails = remediation;
        this.status = ViolationStatus.REMEDIATED;
    }
    
    public boolean isRemediated() {
        return status == ViolationStatus.REMEDIATED;
    }
    
    public boolean isCritical() {
        return severity == ViolationSeverity.CRITICAL;
    }
}