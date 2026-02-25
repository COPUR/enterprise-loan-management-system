package com.loanmanagement.security.zerotrust.domain.model;

/**
 * Status of verification process
 */
public enum VerificationStatus {
    VERIFIED,
    SUSPICIOUS,
    FAILED,
    PENDING,
    EXPIRED,
    BLOCKED
}