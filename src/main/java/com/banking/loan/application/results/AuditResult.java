package com.banking.loan.application.results;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AuditResult(
    String auditId,
    String transactionId,
    String auditStatus,
    List<String> findings,
    Map<String, Object> auditDetails,
    LocalDateTime auditedAt,
    String auditorId
) {}