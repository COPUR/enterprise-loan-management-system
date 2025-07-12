package com.amanahfi.platform.regulatory.domain;

/**
 * Status of a compliance violation
 */
public enum ViolationStatus {
    DETECTED,           // Violation detected
    ACKNOWLEDGED,       // Violation acknowledged by management
    UNDER_REMEDIATION, // Remediation in progress
    REMEDIATED,        // Violation remediated
    ESCALATED,         // Escalated to regulatory authority
    CLOSED            // Violation closed
}