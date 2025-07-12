package com.amanahfi.platform.regulatory.infrastructure.dto.cbuae;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for CBUAE Suspicious Activity Report (SAR) submission
 */
@Data
@Builder
public class CbuaeSarSubmission {
    private String sarId;
    private String violationCode;
    private String description;
    private String severity;
    private Map<String, Object> transactionDetails;
    private String detectedBy;
    private Instant detectedAt;
}