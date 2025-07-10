package com.loanmanagement.security.zerotrust.domain.model;

/**
 * Security recommendation
 */
public record SecurityRecommendation(
    String recommendation,
    SecurityLevel priority,
    String reason
) {}