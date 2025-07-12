package com.amanahfi.platform.regulatory.domain;

/**
 * Compliance status for a specific regulatory authority
 */
public enum AuthorityComplianceStatus {
    PENDING,              // Not yet assessed
    COMPLIANT,           // Fully compliant
    PARTIALLY_COMPLIANT, // Some requirements met
    NON_COMPLIANT,       // Not compliant
    UNDER_REVIEW,        // Under regulatory review
    NOT_APPLICABLE      // Authority not applicable
}