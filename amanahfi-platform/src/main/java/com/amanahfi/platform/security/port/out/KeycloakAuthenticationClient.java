package com.amanahfi.platform.security.port.out;

import com.amanahfi.platform.security.domain.AuthenticationMethod;
import com.amanahfi.platform.security.domain.SecurityPrincipal;

import java.util.Map;

/**
 * Port for Keycloak authentication operations
 */
public interface KeycloakAuthenticationClient {
    
    /**
     * Authenticate user with username/password
     */
    KeycloakAuthenticationResult authenticateWithPassword(
        String username, 
        String password, 
        String clientId, 
        String realm
    );
    
    /**
     * Authenticate user with MFA
     */
    KeycloakAuthenticationResult authenticateWithMFA(
        String username, 
        String password, 
        String mfaCode, 
        String clientId, 
        String realm
    );
    
    /**
     * Authenticate with OAuth2 token
     */
    KeycloakAuthenticationResult authenticateWithOAuth2(
        String oauth2Token, 
        String clientId, 
        String realm
    );
    
    /**
     * Authenticate with JWT token
     */
    KeycloakAuthenticationResult authenticateWithJWT(
        String jwtToken, 
        String expectedAudience, 
        String realm
    );
    
    /**
     * Authenticate with client certificate
     */
    KeycloakAuthenticationResult authenticateWithCertificate(
        String certificateSubject, 
        String certificateFingerprint, 
        String realm
    );
    
    /**
     * Validate access token
     */
    boolean validateAccessToken(String accessToken, String realm);
    
    /**
     * Refresh access token
     */
    KeycloakTokenResponse refreshToken(
        String refreshToken, 
        String clientId, 
        String realm
    );
    
    /**
     * Revoke token
     */
    void revokeToken(String token, String tokenType, String clientId, String realm);
    
    /**
     * Logout user
     */
    void logout(String refreshToken, String clientId, String realm);
    
    /**
     * Get user info from token
     */
    KeycloakUserInfo getUserInfo(String accessToken, String realm);
    
    /**
     * Enable MFA for user
     */
    void enableMFA(String userId, String mfaType, Map<String, String> mfaConfig, String realm);
    
    /**
     * Disable MFA for user
     */
    void disableMFA(String userId, String realm);
    
    /**
     * Verify MFA code
     */
    boolean verifyMFACode(String userId, String code, String realm);
    
    /**
     * Get user roles and permissions
     */
    KeycloakUserRoles getUserRoles(String userId, String realm);
    
    /**
     * Check if user has role
     */
    boolean hasRole(String userId, String role, String realm);
    
    /**
     * Check if user has permission
     */
    boolean hasPermission(String userId, String permission, String realm);
    
    /**
     * Authentication result
     */
    record KeycloakAuthenticationResult(
        boolean successful,
        String accessToken,
        String refreshToken,
        String idToken,
        String tokenType,
        long expiresIn,
        String scope,
        String error,
        String errorDescription
    ) {}
    
    /**
     * Token response
     */
    record KeycloakTokenResponse(
        String accessToken,
        String refreshToken,
        String idToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        String scope
    ) {}
    
    /**
     * User information
     */
    record KeycloakUserInfo(
        String userId,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean emailVerified,
        Map<String, Object> attributes
    ) {}
    
    /**
     * User roles and permissions
     */
    record KeycloakUserRoles(
        java.util.Set<String> realmRoles,
        java.util.Set<String> clientRoles,
        java.util.Set<String> permissions,
        Map<String, java.util.Set<String>> resourcePermissions
    ) {}
}