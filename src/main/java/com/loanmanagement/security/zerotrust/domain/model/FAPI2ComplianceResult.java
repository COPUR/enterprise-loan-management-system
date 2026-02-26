package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result of FAPI2 compliance validation
 */
public record FAPI2ComplianceResult(
    boolean isCompliant,
    Map<String, Object> complianceChecks,
    LocalDateTime validationTime
) {}