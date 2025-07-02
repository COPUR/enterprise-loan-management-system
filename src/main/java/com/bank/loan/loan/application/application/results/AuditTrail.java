package com.bank.loanmanagement.loan.application.results;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AuditTrail(
    String processedBy,
    String processingSystem,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String ipAddress,
    String userAgent,
    List<String> systemEvents,
    Map<String, Object> metadata
) {}