package com.amanahfi.platform.regulatory.infrastructure.dto.hsa;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for HSA Islamic finance report
 */
@Data
@Builder
public class HsaIslamicFinanceReport {
    private String reportId;
    private String reportType;
    private String reportingPeriodHijri;
    private Long murabahaContracts;
    private Long musharakahContracts;
    private Long ijarahContracts;
    private Long qardHassanContracts;
    private Double totalShariaCompliantAssets;
    private Double zakatCalculated;
}