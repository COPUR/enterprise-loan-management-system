package com.amanahfi.platform.regulatory.domain;

/**
 * Result of a compliance assessment
 */
public enum AssessmentResult {
    PASS,                  // Fully compliant
    PASS_WITH_CONDITIONS,  // Compliant with conditions
    FAIL,                  // Non-compliant
    REVIEW_REQUIRED       // Further review needed
}