package com.amanahfi.platform.security.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Command to logout a user
 */
@Value
@Builder
public class LogoutCommand implements Command {
    
    String sessionId;
    String userId;
    String reason;
    boolean revokeAllSessions;
    boolean revokeRefreshTokens;
    String correlationId;
    String clientIp;
    String userAgent;
    
    @Override
    public void validate() {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
    }
}