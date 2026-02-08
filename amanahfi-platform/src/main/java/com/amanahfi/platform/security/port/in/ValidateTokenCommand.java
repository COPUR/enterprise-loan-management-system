package com.amanahfi.platform.security.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Command to validate authentication token
 */
@Value
@Builder
public class ValidateTokenCommand implements Command {
    
    String token;
    String tokenType; // access_token, refresh_token, id_token
    String expectedAudience;
    String expectedIssuer;
    String expectedScope;
    boolean checkExpiration;
    boolean checkSignature;
    boolean checkRevocation;
    String correlationId;
    
    @Override
    public void validate() {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        
        if (tokenType == null || tokenType.trim().isEmpty()) {
            throw new IllegalArgumentException("Token type cannot be null or empty");
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
        
        // Validate token type
        if (!isValidTokenType(tokenType)) {
            throw new IllegalArgumentException("Invalid token type: " + tokenType + 
                ". Must be one of: access_token, refresh_token, id_token");
        }
    }
    
    private boolean isValidTokenType(String tokenType) {
        return "access_token".equals(tokenType) || 
               "refresh_token".equals(tokenType) || 
               "id_token".equals(tokenType);
    }
}