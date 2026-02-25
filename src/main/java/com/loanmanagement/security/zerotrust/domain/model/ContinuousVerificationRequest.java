package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request for continuous verification
 */
public record ContinuousVerificationRequest(
    String sessionId,
    String userId,
    LocalDateTime lastVerification,
    List<SecurityEvent> recentEvents,
    Map<String, Object> currentContext,
    VerificationTrigger trigger
) {}