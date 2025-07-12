package com.amanahfi.platform.security.application;

import com.amanahfi.platform.security.domain.*;
import com.amanahfi.platform.security.port.in.*;
import com.amanahfi.platform.security.port.out.KeycloakClient;
import com.amanahfi.platform.tenant.domain.TenantContextManager;
import com.amanahfi.platform.tenant.domain.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Authentication service using Keycloak
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthenticationService implements AuthenticationUseCase {
    
    private final KeycloakClient keycloakClient;
    private final SecurityContextManager securityContextManager;
    private final TenantContextManager tenantContextManager;
    private final CertificateValidationService certificateValidationService;
    
    @Override
    public SecurityPrincipal authenticate(AuthenticateCommand command) {
        log.info("Authenticating user: {} with method: {}", 
            command.getUsername(), command.getAuthenticationMethod());
        
        // Validate command
        command.validate();
        
        try {
            SecurityPrincipal principal = switch (command.getAuthenticationMethod()) {
                case PASSWORD -> authenticateWithPassword(command);
                case MFA -> authenticateWithMFA(command);
                case CLIENT_CERTIFICATE -> authenticateWithCertificate(command);
                case OAUTH2 -> authenticateWithOAuth2(command);
                case JWT -> authenticateWithJWT(command);
                case API_KEY -> authenticateWithApiKey(command);
                default -> throw new UnsupportedOperationException(
                    "Authentication method not supported: " + command.getAuthenticationMethod());
            };
            
            // Create security context
            SecurityContext securityContext = SecurityContext.builder()
                .principal(principal)
                .correlationId(command.getCorrelationId())
                .requiredSecurityLevel(command.getRequiredSecurityLevel())
                .requiresElevatedPrivileges(command.isRequiresElevatedPrivileges())
                .trustedSource(command.isTrustedSource())
                .clientCertificate(command.getClientCertificate())
                .requestTime(Instant.now())
                .securityAttributes(command.getSecurityAttributes())
                .build();
            
            // Set security context
            securityContextManager.setSecurityContext(securityContext);
            
            log.info("User authenticated successfully: {} - Tenant: {}", 
                principal.getUserId(), principal.getTenantId());
            
            return principal;
            
        } catch (Exception e) {
            log.error("Authentication failed for user: {} - Error: {}", 
                command.getUsername(), e.getMessage());
            throw new AuthenticationException("Authentication failed", e);
        }
    }
    
    @Override
    public void logout(LogoutCommand command) {
        log.info("Logging out user session: {}", command.getSessionId());
        
        // Validate command
        command.validate();
        
        try {
            // Logout from Keycloak
            keycloakClient.logout(command.getSessionId());
            
            // Clear security context
            securityContextManager.clearSecurityContext();
            
            // Clear tenant context
            tenantContextManager.clearTenantContext();
            
            log.info("User logged out successfully: {}", command.getSessionId());
            
        } catch (Exception e) {
            log.error("Logout failed for session: {} - Error: {}", 
                command.getSessionId(), e.getMessage());
            throw new AuthenticationException("Logout failed", e);
        }
    }
    
    @Override
    public SecurityPrincipal refreshToken(RefreshTokenCommand command) {
        log.debug("Refreshing token for user: {}", command.getUserId());
        
        // Validate command
        command.validate();
        
        try {
            // Refresh token with Keycloak
            KeycloakTokenResponse tokenResponse = keycloakClient.refreshToken(command.getRefreshToken());
            
            // Get user info
            KeycloakUserInfo userInfo = keycloakClient.getUserInfo(tokenResponse.getAccessToken());
            
            // Create security principal
            SecurityPrincipal principal = SecurityPrincipal.builder()
                .userId(userInfo.getUserId())
                .username(userInfo.getUsername())
                .fullName(userInfo.getFullName())
                .email(userInfo.getEmail())
                .tenantId(TenantId.of(userInfo.getTenantId()))
                .roles(userInfo.getRoles())
                .permissions(userInfo.getPermissions())
                .authenticationMethod(AuthenticationMethod.OAUTH2)
                .authenticated(true)
                .elevated(userInfo.isElevated())
                .authenticatedAt(Instant.now())
                .expiresAt(tokenResponse.getExpiresAt())
                .sessionId(tokenResponse.getSessionId())
                .clientIp(command.getClientIp())
                .userAgent(command.getUserAgent())
                .build();
            
            log.debug("Token refreshed successfully for user: {}", command.getUserId());
            return principal;
            
        } catch (Exception e) {
            log.error("Token refresh failed for user: {} - Error: {}", 
                command.getUserId(), e.getMessage());
            throw new AuthenticationException("Token refresh failed", e);
        }
    }
    
    @Override
    public boolean validateToken(ValidateTokenCommand command) {
        log.debug("Validating token for session: {}", command.getSessionId());
        
        // Validate command
        command.validate();
        
        try {
            // Validate token with Keycloak
            boolean isValid = keycloakClient.validateToken(command.getToken());
            
            log.debug("Token validation result for session: {} = {}", 
                command.getSessionId(), isValid);
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Token validation failed for session: {} - Error: {}", 
                command.getSessionId(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public void enableMFA(EnableMFACommand command) {
        log.info("Enabling MFA for user: {}", command.getUserId());
        
        // Validate command
        command.validate();
        
        try {
            // Enable MFA in Keycloak
            keycloakClient.enableMFA(command.getUserId(), command.getMfaType());
            
            log.info("MFA enabled successfully for user: {}", command.getUserId());
            
        } catch (Exception e) {
            log.error("MFA enable failed for user: {} - Error: {}", 
                command.getUserId(), e.getMessage());
            throw new AuthenticationException("MFA enable failed", e);
        }
    }
    
    @Override
    public void disableMFA(DisableMFACommand command) {
        log.info("Disabling MFA for user: {}", command.getUserId());
        
        // Validate command
        command.validate();
        
        try {
            // Disable MFA in Keycloak
            keycloakClient.disableMFA(command.getUserId());
            
            log.info("MFA disabled successfully for user: {}", command.getUserId());
            
        } catch (Exception e) {
            log.error("MFA disable failed for user: {} - Error: {}", 
                command.getUserId(), e.getMessage());
            throw new AuthenticationException("MFA disable failed", e);
        }
    }
    
    // Private helper methods
    
    private SecurityPrincipal authenticateWithPassword(AuthenticateCommand command) {
        // Authenticate with Keycloak using password
        KeycloakTokenResponse tokenResponse = keycloakClient.authenticateWithPassword(
            command.getUsername(), command.getPassword());
        
        return createSecurityPrincipal(tokenResponse, AuthenticationMethod.PASSWORD, command);
    }
    
    private SecurityPrincipal authenticateWithMFA(AuthenticateCommand command) {
        // Authenticate with Keycloak using MFA
        KeycloakTokenResponse tokenResponse = keycloakClient.authenticateWithMFA(
            command.getUsername(), command.getPassword(), command.getMfaCode());
        
        return createSecurityPrincipal(tokenResponse, AuthenticationMethod.MFA, command);
    }
    
    private SecurityPrincipal authenticateWithCertificate(AuthenticateCommand command) {
        // Validate client certificate
        if (command.getClientCertificate() == null) {
            throw new AuthenticationException("Client certificate required for certificate authentication");
        }
        
        // Validate certificate
        certificateValidationService.validateCertificate(command.getClientCertificate());
        
        // Authenticate with Keycloak using certificate
        KeycloakTokenResponse tokenResponse = keycloakClient.authenticateWithCertificate(
            command.getClientCertificate());
        
        return createSecurityPrincipal(tokenResponse, AuthenticationMethod.CLIENT_CERTIFICATE, command);
    }
    
    private SecurityPrincipal authenticateWithOAuth2(AuthenticateCommand command) {
        // Authenticate with Keycloak using OAuth2 token
        KeycloakTokenResponse tokenResponse = keycloakClient.authenticateWithOAuth2(
            command.getOauth2Token());
        
        return createSecurityPrincipal(tokenResponse, AuthenticationMethod.OAUTH2, command);
    }
    
    private SecurityPrincipal authenticateWithJWT(AuthenticateCommand command) {
        // Validate and authenticate with JWT token
        KeycloakTokenResponse tokenResponse = keycloakClient.authenticateWithJWT(
            command.getJwtToken());
        
        return createSecurityPrincipal(tokenResponse, AuthenticationMethod.JWT, command);
    }
    
    private SecurityPrincipal authenticateWithApiKey(AuthenticateCommand command) {
        // Authenticate with API key
        KeycloakTokenResponse tokenResponse = keycloakClient.authenticateWithApiKey(
            command.getApiKey());
        
        return createSecurityPrincipal(tokenResponse, AuthenticationMethod.API_KEY, command);
    }
    
    private SecurityPrincipal createSecurityPrincipal(
            KeycloakTokenResponse tokenResponse, 
            AuthenticationMethod authMethod, 
            AuthenticateCommand command) {
        
        // Get user info from token
        KeycloakUserInfo userInfo = keycloakClient.getUserInfo(tokenResponse.getAccessToken());
        
        // Create security principal
        return SecurityPrincipal.builder()
            .userId(userInfo.getUserId())
            .username(userInfo.getUsername())
            .fullName(userInfo.getFullName())
            .email(userInfo.getEmail())
            .tenantId(TenantId.of(userInfo.getTenantId()))
            .roles(userInfo.getRoles())
            .permissions(userInfo.getPermissions())
            .authenticationMethod(authMethod)
            .authenticated(true)
            .elevated(userInfo.isElevated())
            .authenticatedAt(Instant.now())
            .expiresAt(tokenResponse.getExpiresAt())
            .sessionId(tokenResponse.getSessionId())
            .clientIp(command.getClientIp())
            .userAgent(command.getUserAgent())
            .build();
    }
}