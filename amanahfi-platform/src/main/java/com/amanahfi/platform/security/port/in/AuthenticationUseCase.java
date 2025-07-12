package com.amanahfi.platform.security.port.in;

import com.amanahfi.platform.security.domain.SecurityPrincipal;

/**
 * Use case interface for authentication operations
 */
public interface AuthenticationUseCase {
    
    /**
     * Authenticate user
     */
    SecurityPrincipal authenticate(AuthenticateCommand command);
    
    /**
     * Logout user
     */
    void logout(LogoutCommand command);
    
    /**
     * Refresh authentication token
     */
    SecurityPrincipal refreshToken(RefreshTokenCommand command);
    
    /**
     * Validate authentication token
     */
    boolean validateToken(ValidateTokenCommand command);
    
    /**
     * Enable multi-factor authentication
     */
    void enableMFA(EnableMFACommand command);
    
    /**
     * Disable multi-factor authentication
     */
    void disableMFA(DisableMFACommand command);
}