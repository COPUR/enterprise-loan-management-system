package com.bank.loan.loan.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * FAPI 2.0 Compliance Configuration
 * Explicitly defines what is supported and what is removed in FAPI 2.0 migration
 */
@Configuration
public class FAPI2ComplianceConfig {

    /**
     * Supported Response Types (FAPI 2.0 compliant)
     */
    @Bean
    public Set<String> supportedResponseTypes() {
        // FAPI 2.0 only supports authorization code flow
        return Set.of("code");
    }

    /**
     * Removed Response Types (no longer supported)
     */
    @Bean
    public Set<String> removedResponseTypes() {
        // These are explicitly removed in FAPI 2.0 migration
        return Set.of(
            "token",                    // Implicit flow - REMOVED
            "id_token",                 // Implicit flow - REMOVED
            "code id_token",            // Hybrid flow - REMOVED
            "code token",               // Hybrid flow - REMOVED
            "code id_token token"       // Hybrid flow - REMOVED
        );
    }

    /**
     * Supported Response Modes (FAPI 2.0 compliant)
     */
    @Bean
    public Set<String> supportedResponseModes() {
        // FAPI 2.0 only supports query mode (back-channel delivery)
        return Set.of("query");
    }

    /**
     * Removed Response Modes (no longer supported)
     */
    @Bean
    public Set<String> removedResponseModes() {
        // These are explicitly removed in FAPI 2.0 migration
        return Set.of(
            "fragment",     // Front-channel delivery via URL fragment - REMOVED
            "form_post"     // Front-channel delivery via form post - REMOVED
        );
    }

    /**
     * Supported Grant Types (FAPI 2.0 compliant)
     */
    @Bean
    public Set<String> supportedGrantTypes() {
        return Set.of(
            "authorization_code",   // Authorization code grant with PKCE
            "refresh_token"         // Refresh token grant
        );
    }

    /**
     * Removed Grant Types (no longer supported)
     */
    @Bean
    public Set<String> removedGrantTypes() {
        // These are explicitly removed in FAPI 2.0 migration
        return Set.of(
            "implicit",             // Implicit grant - REMOVED
            "password",             // Resource owner password credentials - REMOVED
            "client_credentials"    // Client credentials grant - REMOVED (for this use case)
        );
    }

    /**
     * Supported Client Authentication Methods (FAPI 2.0 compliant)
     */
    @Bean
    public Set<String> supportedClientAuthMethods() {
        return Set.of(
            "private_key_jwt"       // Only private_key_jwt for FAPI 2.0
        );
    }

    /**
     * Removed Client Authentication Methods (no longer supported)
     */
    @Bean
    public Set<String> removedClientAuthMethods() {
        // These are explicitly removed in FAPI 2.0 migration
        return Set.of(
            "client_secret_basic",  // Basic auth with client secret - REMOVED
            "client_secret_post",   // POST with client secret - REMOVED
            "client_secret_jwt",    // JWT with shared secret - REMOVED
            "none",                 // No authentication - REMOVED
            "tls_client_auth"       // mTLS client auth - REPLACED by DPoP
        );
    }

    /**
     * Required Security Features (FAPI 2.0 compliance)
     */
    @Bean
    public Set<String> requiredSecurityFeatures() {
        return Set.of(
            "PAR",              // Pushed Authorization Requests - REQUIRED
            "PKCE",             // Proof Key for Code Exchange - REQUIRED
            "DPoP",             // Demonstrating Proof-of-Possession - REQUIRED
            "private_key_jwt",  // Private key JWT client auth - REQUIRED
            "JTI_validation",   // JTI replay prevention - REQUIRED
            "nonce_support"     // Cryptographic nonces - REQUIRED
        );
    }

    /**
     * Removed Security Features (no longer used)
     */
    @Bean
    public Set<String> removedSecurityFeatures() {
        return Set.of(
            "mTLS",                     // Mutual TLS - REPLACED by DPoP
            "certificate_binding",      // Certificate-bound tokens - REPLACED by DPoP
            "hybrid_flow",              // Hybrid flow support - REMOVED
            "implicit_flow",            // Implicit flow support - REMOVED
            "front_channel_logout",     // Front-channel logout - REMOVED
            "id_token_delivery"         // Front-channel ID token delivery - REMOVED
        );
    }

    /**
     * Migration Validation Rules
     */
    @Bean
    public FAPI2MigrationValidator fapi2MigrationValidator() {
        return new FAPI2MigrationValidator(
            supportedResponseTypes(),
            removedResponseTypes(),
            supportedResponseModes(),
            removedResponseModes(),
            supportedGrantTypes(),
            removedGrantTypes(),
            supportedClientAuthMethods(),
            removedClientAuthMethods()
        );
    }

    /**
     * FAPI 2.0 Migration Validator
     */
    public static class FAPI2MigrationValidator {
        private final Set<String> supportedResponseTypes;
        private final Set<String> removedResponseTypes;
        private final Set<String> supportedResponseModes;
        private final Set<String> removedResponseModes;
        private final Set<String> supportedGrantTypes;
        private final Set<String> removedGrantTypes;
        private final Set<String> supportedClientAuthMethods;
        private final Set<String> removedClientAuthMethods;

        public FAPI2MigrationValidator(Set<String> supportedResponseTypes,
                                     Set<String> removedResponseTypes,
                                     Set<String> supportedResponseModes,
                                     Set<String> removedResponseModes,
                                     Set<String> supportedGrantTypes,
                                     Set<String> removedGrantTypes,
                                     Set<String> supportedClientAuthMethods,
                                     Set<String> removedClientAuthMethods) {
            this.supportedResponseTypes = supportedResponseTypes;
            this.removedResponseTypes = removedResponseTypes;
            this.supportedResponseModes = supportedResponseModes;
            this.removedResponseModes = removedResponseModes;
            this.supportedGrantTypes = supportedGrantTypes;
            this.removedGrantTypes = removedGrantTypes;
            this.supportedClientAuthMethods = supportedClientAuthMethods;
            this.removedClientAuthMethods = removedClientAuthMethods;
        }

        /**
         * Validate response type is FAPI 2.0 compliant
         */
        public boolean isValidResponseType(String responseType) {
            if (responseType == null) return false;
            
            // Check if it's a removed response type
            if (removedResponseTypes.contains(responseType)) {
                throw new IllegalArgumentException(
                    "Response type '" + responseType + "' is not supported in FAPI 2.0. " +
                    "Only 'code' response type is allowed."
                );
            }
            
            return supportedResponseTypes.contains(responseType);
        }

        /**
         * Validate response mode is FAPI 2.0 compliant
         */
        public boolean isValidResponseMode(String responseMode) {
            if (responseMode == null) return true; // Default to query mode
            
            // Check if it's a removed response mode
            if (removedResponseModes.contains(responseMode)) {
                throw new IllegalArgumentException(
                    "Response mode '" + responseMode + "' is not supported in FAPI 2.0. " +
                    "Front-channel delivery via fragment or form_post is not allowed."
                );
            }
            
            return supportedResponseModes.contains(responseMode);
        }

        /**
         * Validate grant type is FAPI 2.0 compliant
         */
        public boolean isValidGrantType(String grantType) {
            if (grantType == null) return false;
            
            // Check if it's a removed grant type
            if (removedGrantTypes.contains(grantType)) {
                throw new IllegalArgumentException(
                    "Grant type '" + grantType + "' is not supported in FAPI 2.0. " +
                    "Only authorization_code and refresh_token grants are allowed."
                );
            }
            
            return supportedGrantTypes.contains(grantType);
        }

        /**
         * Validate client authentication method is FAPI 2.0 compliant
         */
        public boolean isValidClientAuthMethod(String authMethod) {
            if (authMethod == null) return false;
            
            // Check if it's a removed auth method
            if (removedClientAuthMethods.contains(authMethod)) {
                throw new IllegalArgumentException(
                    "Client authentication method '" + authMethod + "' is not supported in FAPI 2.0. " +
                    "Only private_key_jwt is allowed."
                );
            }
            
            return supportedClientAuthMethods.contains(authMethod);
        }

        /**
         * Get validation summary for logging/monitoring
         */
        public String getValidationSummary() {
            return String.format(
                "FAPI 2.0 Compliance - Supported: response_types=%s, response_modes=%s, " +
                "grant_types=%s, client_auth_methods=%s",
                supportedResponseTypes, supportedResponseModes,
                supportedGrantTypes, supportedClientAuthMethods
            );
        }

        /**
         * Get removed features summary for migration tracking
         */
        public String getRemovedFeaturesSummary() {
            return String.format(
                "FAPI 2.0 Migration - Removed: response_types=%s, response_modes=%s, " +
                "grant_types=%s, client_auth_methods=%s",
                removedResponseTypes, removedResponseModes,
                removedGrantTypes, removedClientAuthMethods
            );
        }
    }
}