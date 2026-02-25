package com.loanmanagement.security.zerotrust.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Request for policy enforcement
 */
public record PolicyEnforcementRequest(
    String userId,
    String resourceId,
    SecurityOperation operation,
    Map<String, Object> context,
    List<String> applicablePolicies,
    EnforcementMode mode
) {}