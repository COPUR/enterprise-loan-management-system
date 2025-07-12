package com.amanahfi.platform.security.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Command to disable multi-factor authentication
 */
@Value
@Builder
public class DisableMFACommand implements Command {
    
    String userId;
    String currentPassword;
    String verificationCode; // Current MFA code to confirm disable
    String reason;
    boolean revokeBackupCodes;
    boolean requireAdminApproval;
    String adminUserId; // If admin approval required
    String correlationId;
    
    @Override
    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Current password cannot be null or empty for MFA disable");
        }
        
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification code cannot be null or empty for MFA disable");
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
        
        if (requireAdminApproval && (adminUserId == null || adminUserId.trim().isEmpty())) {
            throw new IllegalArgumentException("Admin user ID is required when admin approval is needed");
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for disabling MFA cannot be null or empty");
        }
    }
}