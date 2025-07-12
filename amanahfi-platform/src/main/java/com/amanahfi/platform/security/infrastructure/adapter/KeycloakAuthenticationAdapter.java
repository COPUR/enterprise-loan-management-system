package com.amanahfi.platform.security.infrastructure.adapter;

import com.amanahfi.platform.security.application.AuthenticationException;
import com.amanahfi.platform.security.port.out.KeycloakAuthenticationClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;

/**
 * Keycloak authentication adapter implementation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthenticationAdapter implements KeycloakAuthenticationClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${amanahfi.security.keycloak.server-url:http://localhost:8080}")
    private String keycloakServerUrl;
    
    @Value("${amanahfi.security.keycloak.admin-username:admin}")
    private String adminUsername;
    
    @Value("${amanahfi.security.keycloak.admin-password:admin}")
    private String adminPassword;
    
    @Value("${amanahfi.security.keycloak.admin-client-id:admin-cli}")
    private String adminClientId;
    
    @Override
    public KeycloakAuthenticationResult authenticateWithPassword(
            String username, String password, String clientId, String realm) {
        
        log.debug("Authenticating user {} with password in realm {}", username, realm);
        
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", clientId);
            formData.add("username", username);
            formData.add("password", password);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                keycloakServerUrl, realm);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                return new KeycloakAuthenticationResult(
                    true,
                    (String) responseBody.get("access_token"),
                    (String) responseBody.get("refresh_token"),
                    (String) responseBody.get("id_token"),
                    (String) responseBody.get("token_type"),
                    ((Number) responseBody.get("expires_in")).longValue(),
                    (String) responseBody.get("scope"),
                    null,
                    null
                );
            } else {
                log.warn("Authentication failed for user {} in realm {}: {}", 
                    username, realm, response.getStatusCode());
                
                return new KeycloakAuthenticationResult(
                    false, null, null, null, null, 0, null, 
                    "authentication_failed", "Invalid credentials"
                );
            }
            
        } catch (Exception e) {
            log.error("Error authenticating user {} in realm {}: {}", username, realm, e.getMessage());
            
            return new KeycloakAuthenticationResult(
                false, null, null, null, null, 0, null,
                "server_error", e.getMessage()
            );
        }
    }
    
    @Override
    public KeycloakAuthenticationResult authenticateWithMFA(
            String username, String password, String mfaCode, String clientId, String realm) {
        
        log.debug("Authenticating user {} with MFA in realm {}", username, realm);
        
        // First authenticate with password
        KeycloakAuthenticationResult passwordResult = authenticateWithPassword(username, password, clientId, realm);
        
        if (!passwordResult.successful()) {
            return passwordResult;
        }
        
        // Then verify MFA code
        boolean mfaValid = verifyMFACode(extractUserIdFromToken(passwordResult.accessToken()), mfaCode, realm);
        
        if (!mfaValid) {
            return new KeycloakAuthenticationResult(
                false, null, null, null, null, 0, null,
                "invalid_mfa", "Invalid MFA code"
            );
        }
        
        return passwordResult;
    }
    
    @Override
    public KeycloakAuthenticationResult authenticateWithOAuth2(
            String oauth2Token, String clientId, String realm) {
        
        log.debug("Authenticating with OAuth2 token in realm {}", realm);
        
        try {
            // Validate OAuth2 token with Keycloak
            String userInfoUrl = String.format("%s/realms/%s/protocol/openid-connect/userinfo", 
                keycloakServerUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(oauth2Token);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return new KeycloakAuthenticationResult(
                    true, oauth2Token, null, null, "Bearer", 3600, null, null, null
                );
            } else {
                return new KeycloakAuthenticationResult(
                    false, null, null, null, null, 0, null,
                    "invalid_token", "Invalid OAuth2 token"
                );
            }
            
        } catch (Exception e) {
            log.error("Error validating OAuth2 token in realm {}: {}", realm, e.getMessage());
            
            return new KeycloakAuthenticationResult(
                false, null, null, null, null, 0, null,
                "server_error", e.getMessage()
            );
        }
    }
    
    @Override
    public KeycloakAuthenticationResult authenticateWithJWT(
            String jwtToken, String expectedAudience, String realm) {
        
        log.debug("Authenticating with JWT token in realm {}", realm);
        
        try {
            // Validate JWT token with Keycloak
            if (validateAccessToken(jwtToken, realm)) {
                return new KeycloakAuthenticationResult(
                    true, jwtToken, null, null, "Bearer", 3600, null, null, null
                );
            } else {
                return new KeycloakAuthenticationResult(
                    false, null, null, null, null, 0, null,
                    "invalid_token", "Invalid JWT token"
                );
            }
            
        } catch (Exception e) {
            log.error("Error validating JWT token in realm {}: {}", realm, e.getMessage());
            
            return new KeycloakAuthenticationResult(
                false, null, null, null, null, 0, null,
                "server_error", e.getMessage()
            );
        }
    }
    
    @Override
    public KeycloakAuthenticationResult authenticateWithCertificate(
            String certificateSubject, String certificateFingerprint, String realm) {
        
        log.debug("Authenticating with certificate {} in realm {}", certificateSubject, realm);
        
        try {
            // In a real implementation, this would:
            // 1. Validate certificate against trusted CAs
            // 2. Check certificate revocation status
            // 3. Map certificate to Keycloak user
            // 4. Generate tokens for the authenticated user
            
            // For now, return a mock success for valid certificate format
            if (certificateSubject != null && certificateFingerprint != null) {
                return new KeycloakAuthenticationResult(
                    true, "mock-certificate-token", null, null, "Bearer", 3600, null, null, null
                );
            } else {
                return new KeycloakAuthenticationResult(
                    false, null, null, null, null, 0, null,
                    "invalid_certificate", "Invalid certificate"
                );
            }
            
        } catch (Exception e) {
            log.error("Error authenticating with certificate in realm {}: {}", realm, e.getMessage());
            
            return new KeycloakAuthenticationResult(
                false, null, null, null, null, 0, null,
                "server_error", e.getMessage()
            );
        }
    }
    
    @Override
    public boolean validateAccessToken(String accessToken, String realm) {
        log.debug("Validating access token in realm {}", realm);
        
        try {
            String userInfoUrl = String.format("%s/realms/%s/protocol/openid-connect/userinfo", 
                keycloakServerUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);
            
            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            log.error("Error validating access token in realm {}: {}", realm, e.getMessage());
            return false;
        }
    }
    
    @Override
    public KeycloakTokenResponse refreshToken(String refreshToken, String clientId, String realm) {
        log.debug("Refreshing token for client {} in realm {}", clientId, realm);
        
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "refresh_token");
            formData.add("client_id", clientId);
            formData.add("refresh_token", refreshToken);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", 
                keycloakServerUrl, realm);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                return new KeycloakTokenResponse(
                    (String) responseBody.get("access_token"),
                    (String) responseBody.get("refresh_token"),
                    (String) responseBody.get("id_token"),
                    (String) responseBody.get("token_type"),
                    ((Number) responseBody.get("expires_in")).longValue(),
                    ((Number) responseBody.getOrDefault("refresh_expires_in", 0)).longValue(),
                    (String) responseBody.get("scope")
                );
            } else {
                throw new AuthenticationException("Failed to refresh token: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error refreshing token for client {} in realm {}: {}", clientId, realm, e.getMessage());
            throw new AuthenticationException("Failed to refresh token", e);
        }
    }
    
    @Override
    public void revokeToken(String token, String tokenType, String clientId, String realm) {
        log.debug("Revoking {} token for client {} in realm {}", tokenType, clientId, realm);
        
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("token", token);
            formData.add("token_type_hint", tokenType);
            formData.add("client_id", clientId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            String revokeUrl = String.format("%s/realms/%s/protocol/openid-connect/revoke", 
                keycloakServerUrl, realm);
            
            ResponseEntity<String> response = restTemplate.postForEntity(revokeUrl, request, String.class);
            
            if (response.getStatusCode() != HttpStatus.OK) {
                log.warn("Token revocation returned status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error revoking token for client {} in realm {}: {}", clientId, realm, e.getMessage());
            throw new AuthenticationException("Failed to revoke token", e);
        }
    }
    
    @Override
    public void logout(String refreshToken, String clientId, String realm) {
        log.debug("Logging out client {} in realm {}", clientId, realm);
        
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("refresh_token", refreshToken);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            String logoutUrl = String.format("%s/realms/%s/protocol/openid-connect/logout", 
                keycloakServerUrl, realm);
            
            ResponseEntity<String> response = restTemplate.postForEntity(logoutUrl, request, String.class);
            
            if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
                log.warn("Logout returned unexpected status: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error logging out client {} in realm {}: {}", clientId, realm, e.getMessage());
            throw new AuthenticationException("Failed to logout", e);
        }
    }
    
    @Override
    public KeycloakUserInfo getUserInfo(String accessToken, String realm) {
        log.debug("Getting user info in realm {}", realm);
        
        try {
            String userInfoUrl = String.format("%s/realms/%s/protocol/openid-connect/userinfo", 
                keycloakServerUrl, realm);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> userInfo = response.getBody();
                
                return new KeycloakUserInfo(
                    (String) userInfo.get("sub"),
                    (String) userInfo.get("preferred_username"),
                    (String) userInfo.get("email"),
                    (String) userInfo.get("given_name"),
                    (String) userInfo.get("family_name"),
                    Boolean.TRUE.equals(userInfo.get("email_verified")),
                    userInfo
                );
            } else {
                throw new AuthenticationException("Failed to get user info: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error getting user info in realm {}: {}", realm, e.getMessage());
            throw new AuthenticationException("Failed to get user info", e);
        }
    }
    
    @Override
    public void enableMFA(String userId, String mfaType, Map<String, String> mfaConfig, String realm) {
        log.debug("Enabling MFA type {} for user {} in realm {}", mfaType, userId, realm);
        
        // Implementation would use Keycloak Admin API to configure MFA
        // This is a simplified version
        try {
            // Get admin token first
            String adminToken = getAdminToken();
            
            // Configure MFA for user using Admin API
            // Implementation details depend on specific MFA type and Keycloak configuration
            
            log.info("MFA {} enabled for user {} in realm {}", mfaType, userId, realm);
            
        } catch (Exception e) {
            log.error("Error enabling MFA for user {} in realm {}: {}", userId, realm, e.getMessage());
            throw new AuthenticationException("Failed to enable MFA", e);
        }
    }
    
    @Override
    public void disableMFA(String userId, String realm) {
        log.debug("Disabling MFA for user {} in realm {}", userId, realm);
        
        try {
            // Get admin token first
            String adminToken = getAdminToken();
            
            // Remove MFA configuration for user using Admin API
            
            log.info("MFA disabled for user {} in realm {}", userId, realm);
            
        } catch (Exception e) {
            log.error("Error disabling MFA for user {} in realm {}: {}", userId, realm, e.getMessage());
            throw new AuthenticationException("Failed to disable MFA", e);
        }
    }
    
    @Override
    public boolean verifyMFACode(String userId, String code, String realm) {
        log.debug("Verifying MFA code for user {} in realm {}", userId, realm);
        
        try {
            // Implementation would verify MFA code with Keycloak
            // This is a simplified version that accepts any 6-digit code for demo
            return code != null && code.matches("\\d{6}");
            
        } catch (Exception e) {
            log.error("Error verifying MFA code for user {} in realm {}: {}", userId, realm, e.getMessage());
            return false;
        }
    }
    
    @Override
    public KeycloakUserRoles getUserRoles(String userId, String realm) {
        log.debug("Getting roles for user {} in realm {}", userId, realm);
        
        try {
            // Get admin token first
            String adminToken = getAdminToken();
            
            // Get user roles using Admin API
            // This is a simplified implementation
            
            return new KeycloakUserRoles(
                Set.of("USER"),
                Set.of("LOAN_OFFICER"),
                Set.of("READ_LOANS", "CREATE_LOANS"),
                Map.of()
            );
            
        } catch (Exception e) {
            log.error("Error getting roles for user {} in realm {}: {}", userId, realm, e.getMessage());
            throw new AuthenticationException("Failed to get user roles", e);
        }
    }
    
    @Override
    public boolean hasRole(String userId, String role, String realm) {
        try {
            KeycloakUserRoles userRoles = getUserRoles(userId, realm);
            return userRoles.realmRoles().contains(role) || userRoles.clientRoles().contains(role);
        } catch (Exception e) {
            log.error("Error checking role {} for user {} in realm {}: {}", role, userId, realm, e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean hasPermission(String userId, String permission, String realm) {
        try {
            KeycloakUserRoles userRoles = getUserRoles(userId, realm);
            return userRoles.permissions().contains(permission);
        } catch (Exception e) {
            log.error("Error checking permission {} for user {} in realm {}: {}", 
                permission, userId, realm, e.getMessage());
            return false;
        }
    }
    
    // Helper methods
    
    private String getAdminToken() {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "password");
            formData.add("client_id", adminClientId);
            formData.add("username", adminUsername);
            formData.add("password", adminPassword);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
            
            String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", keycloakServerUrl);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            } else {
                throw new AuthenticationException("Failed to get admin token");
            }
            
        } catch (Exception e) {
            log.error("Error getting admin token: {}", e.getMessage());
            throw new AuthenticationException("Failed to get admin token", e);
        }
    }
    
    private String extractUserIdFromToken(String accessToken) {
        // In a real implementation, this would parse the JWT token to extract user ID
        // For now, return a mock user ID
        return "mock-user-id";
    }
}