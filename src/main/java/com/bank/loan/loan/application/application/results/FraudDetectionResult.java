package com.bank.loanmanagement.loan.application.results;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record FraudDetectionResult(
    String detectionId,
    String transactionId,
    Double riskScore,
    String riskLevel,
    List<String> fraudIndicators,
    Map<String, Object> analysisDetails,
    LocalDateTime analyzedAt,
    String recommendation
) {}