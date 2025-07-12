package com.amanahfi.platform.regulatory.domain;

/**
 * Overall compliance status
 */
public enum ComplianceStatus {
    PENDING_ASSESSMENT,    // Initial state, awaiting first assessment
    COMPLIANT,            // Fully compliant with all requirements
    PARTIALLY_COMPLIANT,  // Some requirements met, others pending
    NON_COMPLIANT,        // Not meeting regulatory requirements
    UNDER_REVIEW,         // Compliance under regulatory review
    SUSPENDED,            // Compliance suspended due to violations
    EXEMPTED             // Exempted from certain requirements
}