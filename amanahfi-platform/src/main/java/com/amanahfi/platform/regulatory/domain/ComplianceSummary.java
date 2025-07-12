package com.amanahfi.platform.regulatory.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Map;

/**
 * Summary of compliance status
 */
@Value
@Builder
public class ComplianceSummary {
    ComplianceId complianceId;
    String entityId;
    ComplianceType complianceType;
    Jurisdiction jurisdiction;
    ComplianceStatus overallStatus;
    Map<RegulatoryAuthority, AuthorityComplianceStatus> authorityStatuses;
    long activeViolations;
    LocalDate lastAssessmentDate;
    LocalDate nextAssessmentDate;
}