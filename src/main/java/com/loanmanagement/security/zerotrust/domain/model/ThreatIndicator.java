package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;

/**
 * Threat indicator information
 */
public record ThreatIndicator(
    String indicatorType,
    String value,
    ThreatSeverity severity,
    String source,
    LocalDateTime firstSeen,
    LocalDateTime lastSeen
) {}