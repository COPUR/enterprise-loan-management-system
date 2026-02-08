package com.amanahfi.platform.security.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Command to refresh authentication token
 */
@Value
@Builder
public class RefreshTokenCommand implements Command {
    
    String refreshToken;
    String sessionId;
    String clientId;
    String scope;
    String correlationId;
    String clientIp;
    String userAgent;
    boolean extendSession;
    
    @Override
    public void validate() {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
        
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be null or empty");
        }
    }
}