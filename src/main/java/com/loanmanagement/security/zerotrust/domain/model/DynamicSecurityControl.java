package com.loanmanagement.security.zerotrust.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Dynamic security control recommendation
 */
public record DynamicSecurityControl(
    String controlType,
    String description,
    Map<String, Object> parameters,
    LocalDateTime appliedAt
) {}