package com.amanahfi.platform.regulatory.infrastructure.dto.vara;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for VARA compliance submission
 */
@Data
@Builder
public class VaraComplianceSubmission {
    private String submissionId;
    private String licenseNumber;
    private String entityId;
    private String complianceCategory;
    private Double assessmentScore;
    private String complianceStatus;
    private String[] cryptoAssetTypes;
    private boolean amlKycCompliant;
    private String custodyArrangements;
}