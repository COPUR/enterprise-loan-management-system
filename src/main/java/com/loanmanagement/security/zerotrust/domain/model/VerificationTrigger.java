package com.loanmanagement.security.zerotrust.domain.model;

/**
 * Triggers for continuous verification
 */
public enum VerificationTrigger {
    TIME_BASED,
    BEHAVIOR_CHANGE,
    LOCATION_CHANGE,
    DEVICE_CHANGE,
    RISK_ESCALATION,
    POLICY_REQUIREMENT,
    MANUAL_REQUEST,
    SUSPICIOUS_ACTIVITY,
    COMPLIANCE_CHECK
}