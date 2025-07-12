package com.amanahfi.platform.regulatory.infrastructure.dto.cbuae;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for CBUAE compliance submission
 */
@Data
@Builder
public class CbuaeComplianceSubmission {
    private String submissionId;
    private String entityId;
    private String complianceType;
    private Double assessmentScore;
    private String assessmentResult;
    private List<String> findings;
    private List<String> recommendations;
    private String assessorId;
}