package com.bank.loanmanagement.loan.application.results;

import java.time.LocalDateTime;
import java.util.Map;

public record RegulatoryReportResult(
    String reportId,
    String reportType,
    String status,
    String reportLocation,
    LocalDateTime generatedAt,
    Map<String, Object> reportSummary,
    String format
) {}