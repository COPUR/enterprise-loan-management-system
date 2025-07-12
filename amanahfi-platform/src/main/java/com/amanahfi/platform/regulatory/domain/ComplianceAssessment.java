package com.amanahfi.platform.regulatory.domain;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Compliance assessment details
 */
@Value
@Builder
public class ComplianceAssessment {
    RegulatoryAuthority authority;
    AssessmentResult result;
    Double score; // 0.0 to 100.0
    String referenceNumber;
    Instant assessmentDate;
    String assessorId;
    List<String> findings;
    List<String> recommendations;
    List<String> pendingRequirements;
    Instant validUntil;
}