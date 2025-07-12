package com.amanahfi.platform.regulatory.infrastructure.dto.cbuae;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

/**
 * DTO for CBUAE AML report submission
 */
@Data
@Builder
public class CbuaeAmlReport {
    private String reportId;
    private LocalDate reportingPeriodStart;
    private LocalDate reportingPeriodEnd;
    private Map<String, Object> reportData;
    private String submittedBy;
}