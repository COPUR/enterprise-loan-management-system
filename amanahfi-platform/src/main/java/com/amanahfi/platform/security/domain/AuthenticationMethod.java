package com.amanahfi.platform.security.domain;

/**
 * Authentication methods supported by the platform
 */
public enum AuthenticationMethod {
    
    /**
     * Username and password authentication
     */
    PASSWORD,
    
    /**
     * Multi-factor authentication
     */
    MFA,
    
    /**
     * Client certificate authentication (mTLS)
     */
    CLIENT_CERTIFICATE,
    
    /**
     * OAuth2/OIDC token authentication
     */
    OAUTH2,
    
    /**
     * SAML authentication
     */
    SAML,
    
    /**
     * API key authentication
     */
    API_KEY,
    
    /**
     * JWT token authentication
     */
    JWT,
    
    /**
     * Biometric authentication
     */
    BIOMETRIC,
    
    /**
     * Hardware security key
     */
    HARDWARE_KEY;
    
    /**
     * Check if authentication method is considered strong
     */
    public boolean isStrong() {
        return this == MFA || 
               this == CLIENT_CERTIFICATE || 
               this == BIOMETRIC || 
               this == HARDWARE_KEY;
    }
    
    /**
     * Check if authentication method is certificate-based
     */
    public boolean isCertificateBased() {
        return this == CLIENT_CERTIFICATE;
    }
    
    /**
     * Check if authentication method is token-based
     */
    public boolean isTokenBased() {
        return this == OAUTH2 || this == JWT || this == API_KEY;
    }
    
    /**
     * Check if authentication method requires additional verification
     */
    public boolean requiresAdditionalVerification() {
        return this == PASSWORD || this == API_KEY;
    }
}