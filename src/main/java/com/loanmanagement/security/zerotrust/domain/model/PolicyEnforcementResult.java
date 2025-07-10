package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of policy enforcement
 */
public record PolicyEnforcementResult(
    boolean enforcementSuccessful,
    List<String> actionsApplied,
    List<String> violations,
    String effectiveness,
    List<String> adaptations,
    LocalDateTime enforcementTime
) {}