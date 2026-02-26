package com.loanmanagement.security.zerotrust.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of continuous verification
 */
public record ContinuousVerificationResult(
    boolean verificationPassed,
    VerificationStatus status,
    List<SecurityAnomaly> anomalies,
    BigDecimal confidenceScore,
    LocalDateTime nextVerificationTime,
    List<DynamicSecurityControl> recommendedControls
) {}