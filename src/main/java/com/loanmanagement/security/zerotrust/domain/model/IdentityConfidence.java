package com.loanmanagement.security.zerotrust.domain.model;

/**
 * Identity verification confidence levels
 */
public enum IdentityConfidence {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH,
    ABSOLUTE
}