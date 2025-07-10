package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Domain result for Zero Trust Security validation
 */
public record ZeroTrustSecurityResult(
    boolean isValid,
    SecurityLevel securityLevel,
    List<SecurityViolation> violations,
    Map<String, Object> securityMetrics,
    List<SecurityRecommendation> recommendations,
    LocalDateTime validationTime,
    String sessionId
) {}