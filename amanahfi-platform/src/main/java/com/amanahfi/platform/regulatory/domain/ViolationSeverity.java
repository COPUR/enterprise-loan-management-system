package com.amanahfi.platform.regulatory.domain;

/**
 * Severity levels for compliance violations
 */
public enum ViolationSeverity {
    LOW,      // Minor violations with minimal impact
    MEDIUM,   // Moderate violations requiring attention
    HIGH,     // Significant violations with material impact
    CRITICAL  // Critical violations requiring immediate action
}