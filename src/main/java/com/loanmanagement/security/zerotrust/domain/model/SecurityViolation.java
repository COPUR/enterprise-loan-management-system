package com.loanmanagement.security.zerotrust.domain.model;

/**
 * Security violation representation
 */
public record SecurityViolation(
    String violationType,
    String description,
    SecurityLevel severity,
    String remediation
) {}