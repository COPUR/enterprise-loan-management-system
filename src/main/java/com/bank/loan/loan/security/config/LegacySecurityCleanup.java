package com.bank.loan.loan.security.config;

import org.springframework.context.annotation.Configuration;

/**
 * Legacy Security Feature Cleanup for FAPI 2.0 Migration
 * Removes deprecated security features that are replaced by DPoP
 */
@Configuration
public class LegacySecurityCleanup {

    /**
     * Certificate-bound token features removed in FAPI 2.0 migration
     */
    public static class RemovedCertificateBindingFeatures {
        
        // REMOVED: mTLS certificate binding
        // Previous: Client certificates were bound to access tokens
        // New: DPoP proof-of-possession replaces certificate binding
        public void configureMTLSCertificateBinding() {
            throw new UnsupportedOperationException(
                "mTLS certificate binding is no longer supported. " +
                "Use DPoP (Demonstrating Proof-of-Possession) instead."
            );
        }
        
        // REMOVED: Certificate thumbprint in token claims
        // Previous: Tokens contained cnf.x5t# claim with certificate thumbprint
        // New: Tokens contain cnf.jkt claim with DPoP key thumbprint
        public void configureCertificateThumbprintBinding() {
            throw new UnsupportedOperationException(
                "Certificate thumbprint binding (cnf.x5t#) is no longer supported. " +
                "Use DPoP key thumbprint binding (cnf.jkt) instead."
            );
        }
        
        // REMOVED: X.509 certificate validation for token binding
        // Previous: Resource servers validated client certificates against token binding
        // New: Resource servers validate DPoP proofs against token binding
        public void configureX509CertificateValidation() {
            throw new UnsupportedOperationException(
                "X.509 certificate validation for token binding is no longer supported. " +
                "Use DPoP proof validation instead."
            );
        }
        
        // REMOVED: TLS client authentication method
        // Previous: tls_client_auth and self_signed_tls_client_auth
        // New: private_key_jwt only
        public void configureTLSClientAuthentication() {
            throw new UnsupportedOperationException(
                "TLS client authentication methods (tls_client_auth, self_signed_tls_client_auth) " +
                "are no longer supported. Use private_key_jwt client authentication instead."
            );
        }
    }

    /**
     * Front-channel delivery features removed in FAPI 2.0 migration
     */
    public static class RemovedFrontChannelFeatures {
        
        // REMOVED: Front-channel ID token delivery via URL fragment
        // Previous: response_mode=fragment for implicit/hybrid flows
        // New: Only back-channel token delivery via authorization code flow
        public void configureFrontChannelIdTokenDelivery() {
            throw new UnsupportedOperationException(
                "Front-channel ID token delivery via URL fragment is no longer supported. " +
                "Use authorization code flow with back-channel token endpoint instead."
            );
        }
        
        // REMOVED: Form post response mode
        // Previous: response_mode=form_post for hybrid flows
        // New: Only query response mode supported
        public void configureFormPostResponseMode() {
            throw new UnsupportedOperationException(
                "Form post response mode is no longer supported. " +
                "Use query response mode with authorization code flow instead."
            );
        }
        
        // REMOVED: Hybrid flow support
        // Previous: response_type=code id_token, code token, code id_token token
        // New: Only response_type=code supported
        public void configureHybridFlowSupport() {
            throw new UnsupportedOperationException(
                "Hybrid flows (code id_token, code token, code id_token token) are no longer supported. " +
                "Use authorization code flow (response_type=code) with DPoP instead."
            );
        }
        
        // REMOVED: Implicit flow support
        // Previous: response_type=token, id_token
        // New: Only response_type=code supported
        public void configureImplicitFlowSupport() {
            throw new UnsupportedOperationException(
                "Implicit flows (response_type=token, id_token) are no longer supported. " +
                "Use authorization code flow (response_type=code) with PKCE and DPoP instead."
            );
        }
    }

    /**
     * Legacy token binding features removed
     */
    public static class RemovedTokenBindingFeatures {
        
        // REMOVED: RFC 8473 Token Binding support
        // Previous: Token binding based on TLS channel bindings
        // New: DPoP-based proof-of-possession
        public void configureTokenBinding() {
            throw new UnsupportedOperationException(
                "RFC 8473 Token Binding is no longer supported. " +
                "Use DPoP (RFC 9449) for proof-of-possession instead."
            );
        }
        
        // REMOVED: Certificate-bound access tokens without DPoP
        // Previous: cnf.x5t# claim in access tokens
        // New: cnf.jkt claim with DPoP binding
        public void configureCertificateBoundTokens() {
            throw new UnsupportedOperationException(
                "Certificate-bound access tokens (cnf.x5t#) are no longer supported. " +
                "Use DPoP-bound access tokens (cnf.jkt) instead."
            );
        }
    }

    /**
     * Configuration validation to prevent legacy features
     */
    public static class LegacyFeatureValidator {
        
        /**
         * Validate that no legacy mTLS features are configured
         */
        public static void validateNoLegacyMTLS() {
            // Check for removed mTLS configurations
            String[] removedProperties = {
                "server.ssl.client-auth",
                "spring.security.oauth2.resourceserver.jwt.certificate-thumbprint",
                "fapi.mtls.enabled",
                "oauth2.client.auth.tls_client_auth"
            };
            
            for (String property : removedProperties) {
                String value = System.getProperty(property);
                if (value != null && !"false".equals(value)) {
                    throw new IllegalStateException(
                        "Legacy mTLS property '" + property + "' is no longer supported. " +
                        "Remove this configuration and use DPoP instead."
                    );
                }
            }
        }
        
        /**
         * Validate that no legacy flow configurations are present
         */
        public static void validateNoLegacyFlows() {
            // Check for removed flow configurations
            String[] removedFlowTypes = {
                "oauth2.response.types.hybrid",
                "oauth2.response.types.implicit", 
                "oauth2.response.modes.fragment",
                "oauth2.response.modes.form_post"
            };
            
            for (String flowType : removedFlowTypes) {
                String value = System.getProperty(flowType);
                if (value != null && !"false".equals(value)) {
                    throw new IllegalStateException(
                        "Legacy flow configuration '" + flowType + "' is no longer supported. " +
                        "Remove this configuration and use authorization code flow with DPoP instead."
                    );
                }
            }
        }
        
        /**
         * Provide migration guidance for removed features
         */
        public static String getMigrationGuidance() {
            return """
                FAPI 2.0 Migration - Removed Features:
                
                1. mTLS Certificate Binding → DPoP Proof-of-Possession
                   - Remove: client certificate configuration
                   - Add: DPoP key pair generation and validation
                
                2. Front-channel Token Delivery → Back-channel Only
                   - Remove: response_mode=fragment or form_post
                   - Use: response_mode=query (default)
                
                3. Hybrid/Implicit Flows → Authorization Code Flow Only
                   - Remove: response_type=code id_token, token, id_token
                   - Use: response_type=code with PKCE and DPoP
                
                4. Legacy Client Authentication → private_key_jwt Only
                   - Remove: tls_client_auth, client_secret_*
                   - Use: private_key_jwt with client assertion
                
                5. Token Binding (RFC 8473) → DPoP (RFC 9449)
                   - Remove: cnf.x5t# certificate thumbprint
                   - Use: cnf.jkt DPoP key thumbprint
                
                For detailed migration instructions, see:
                - DPoP Migration Tool: DPoPMigrationTool.java
                - FAPI 2.0 Configuration: application-fapi2-dpop.yml
                """;
        }
    }
}