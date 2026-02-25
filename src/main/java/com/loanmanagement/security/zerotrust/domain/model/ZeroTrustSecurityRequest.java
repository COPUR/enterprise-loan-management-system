package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain request for Zero Trust Security validation
 */
public record ZeroTrustSecurityRequest(
    String sessionId,
    String userId,
    String deviceId,
    String ipAddress,
    String userAgent,
    LocalDateTime requestTime,
    Map<String, Object> context,
    SecurityOperation operation,
    String resourceId
) {}