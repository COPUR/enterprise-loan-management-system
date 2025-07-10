package com.loanmanagement.security.zerotrust.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of threat assessment
 */
public record ThreatAssessmentResult(
    ThreatLevel threatLevel,
    List<ThreatIndicator> identifiedThreats,
    BigDecimal riskScore,
    List<String> recommendedMitigations,
    String landscapeAssessment,
    LocalDateTime assessmentTime
) {}