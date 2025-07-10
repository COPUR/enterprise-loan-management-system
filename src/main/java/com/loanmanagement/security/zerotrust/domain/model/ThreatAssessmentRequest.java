package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request for threat assessment
 */
public record ThreatAssessmentRequest(
    String sourceId,
    String targetResource,
    ThreatType threatType,
    List<ThreatIndicator> indicators,
    Map<String, Object> environment,
    LocalDateTime assessmentTime
) {}