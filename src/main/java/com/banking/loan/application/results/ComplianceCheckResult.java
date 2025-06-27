package com.banking.loan.application.results;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ComplianceCheckResult(
    String checkId,
    String entityId,
    String complianceStatus,
    List<String> violations,
    List<String> warnings,
    Map<String, Object> checkDetails,
    LocalDateTime checkedAt,
    String recommendation
) {
    public boolean isCompliant() {
        return "COMPLIANT".equals(complianceStatus) && 
               (violations == null || violations.isEmpty());
    }
}