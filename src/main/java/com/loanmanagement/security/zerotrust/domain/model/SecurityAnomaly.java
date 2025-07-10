package com.loanmanagement.security.zerotrust.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Security anomaly detection
 */
public record SecurityAnomaly(
    String anomalyType,
    String description,
    BigDecimal severity,
    LocalDateTime timestamp
) {}