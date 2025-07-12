package com.amanahfi.platform.regulatory.infrastructure.dto.hsa;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for HSA Sharia compliance submission
 */
@Data
@Builder
public class HsaShariaComplianceSubmission {
    private String submissionId;
    private String institutionCode;
    private String entityId;
    private String productType;
    private Double shariaComplianceScore;
    private String shariaBoard;
    private String fatwaReference;
    private boolean ribaFree;
    private boolean ghararFree;
    private boolean maysirFree;
    private boolean assetBacked;
}