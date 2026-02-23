package com.masrufi.framework.infrastructure.security;

import java.util.HashMap;
import java.util.Map;

/**
 * Local base class replacing the external
 * {@code com.bank.infrastructure.security.FAPISecurityValidator}.
 * Provides the minimal FAPI security contract needed by
 * {@link IslamicFAPISecurityValidator}.
 *
 * <p>
 * The files in this package are excluded from the Gradle compilation surface
 * via
 * {@code sourceSets.main.java.exclude} in {@code build.gradle}. This stub
 * exists solely
 * to keep the IDE error-free while the modularisation work is in progress.
 * </p>
 */
public abstract class FAPISecurityValidatorBase {

    protected final String signingKey;

    protected FAPISecurityValidatorBase(String signingKey) {
        this.signingKey = signingKey;
    }

    /**
     * Validate that the request signature matches the expected HMAC.
     */
    protected void validateRequestSignature(Object request, String expectedSignature) {
        // Stub – override in subclass or integrate with actual FAPI validator
    }

    /**
     * Generate an HMAC signature for the given request payload.
     */
    protected String generateHMACSignature(Object request) {
        return Integer.toHexString(request.hashCode());
    }

    /**
     * Build a FAPI-compliant error response map.
     */
    protected Map<String, Object> createFAPIErrorResponse(String errorCode,
            String errorDescription,
            String interactionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", errorCode);
        response.put("error_description", errorDescription);
        response.put("x_fapi_interaction_id", interactionId);
        return response;
    }
}
