package com.amanahfi.platform.regulatory.infrastructure.dto.vara;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for VARA CBDC transaction report
 */
@Data
@Builder
public class VaraCbdcReport {
    private String reportId;
    private String reportingPeriod;
    private String cbdcType;
    private Long totalTransactions;
    private Double totalVolume;
    private Long crossBorderTransactions;
    private Long suspiciousActivities;
}