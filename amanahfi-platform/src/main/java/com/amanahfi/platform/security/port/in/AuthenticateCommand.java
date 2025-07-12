package com.amanahfi.platform.security.port.in;

import com.amanahfi.platform.security.domain.AuthenticationMethod;
import com.amanahfi.platform.security.domain.ClientCertificate;
import com.amanahfi.platform.security.domain.SecurityLevel;
import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Command to authenticate a user
 */
@Value
@Builder
public class AuthenticateCommand implements Command {
    
    String username;
    String password;
    String mfaCode;
    String oauth2Token;
    String jwtToken;
    String apiKey;
    ClientCertificate clientCertificate;
    AuthenticationMethod authenticationMethod;
    SecurityLevel requiredSecurityLevel;
    boolean requiresElevatedPrivileges;
    boolean trustedSource;
    String correlationId;
    String clientIp;
    String userAgent;
    Map<String, String> securityAttributes;
    
    @Override
    public void validate() {
        if (authenticationMethod == null) {
            throw new IllegalArgumentException("Authentication method cannot be null");
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
        
        // Validate based on authentication method
        switch (authenticationMethod) {
            case PASSWORD -> {
                if (username == null || username.trim().isEmpty()) {
                    throw new IllegalArgumentException("Username cannot be null or empty for password authentication");
                }
                if (password == null || password.trim().isEmpty()) {
                    throw new IllegalArgumentException("Password cannot be null or empty for password authentication");
                }
            }
            case MFA -> {
                if (username == null || username.trim().isEmpty()) {
                    throw new IllegalArgumentException("Username cannot be null or empty for MFA authentication");
                }
                if (password == null || password.trim().isEmpty()) {
                    throw new IllegalArgumentException("Password cannot be null or empty for MFA authentication");
                }
                if (mfaCode == null || mfaCode.trim().isEmpty()) {
                    throw new IllegalArgumentException("MFA code cannot be null or empty for MFA authentication");
                }
            }
            case CLIENT_CERTIFICATE -> {
                if (clientCertificate == null) {
                    throw new IllegalArgumentException("Client certificate cannot be null for certificate authentication");
                }
            }
            case OAUTH2 -> {
                if (oauth2Token == null || oauth2Token.trim().isEmpty()) {
                    throw new IllegalArgumentException("OAuth2 token cannot be null or empty for OAuth2 authentication");
                }
            }
            case JWT -> {
                if (jwtToken == null || jwtToken.trim().isEmpty()) {
                    throw new IllegalArgumentException("JWT token cannot be null or empty for JWT authentication");
                }
            }
            case API_KEY -> {
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new IllegalArgumentException("API key cannot be null or empty for API key authentication");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported authentication method: " + authenticationMethod);
        }
    }
}