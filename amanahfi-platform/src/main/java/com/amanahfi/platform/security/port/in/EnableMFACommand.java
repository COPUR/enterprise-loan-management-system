package com.amanahfi.platform.security.port.in;

import com.amanahfi.platform.shared.command.Command;
import lombok.Builder;
import lombok.Value;

/**
 * Command to enable multi-factor authentication
 */
@Value
@Builder
public class EnableMFACommand implements Command {
    
    String userId;
    String mfaType; // TOTP, SMS, EMAIL, HARDWARE_TOKEN
    String phoneNumber; // For SMS MFA
    String email; // For EMAIL MFA
    String deviceId; // For hardware token MFA
    String secretKey; // Generated TOTP secret
    String verificationCode; // Initial verification
    boolean backupCodes; // Generate backup codes
    String correlationId;
    
    @Override
    public void validate() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        if (mfaType == null || mfaType.trim().isEmpty()) {
            throw new IllegalArgumentException("MFA type cannot be null or empty");
        }
        
        if (correlationId == null || correlationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Correlation ID cannot be null or empty");
        }
        
        if (verificationCode == null || verificationCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Verification code cannot be null or empty");
        }
        
        // Validate based on MFA type
        switch (mfaType.toUpperCase()) {
            case "TOTP" -> {
                if (secretKey == null || secretKey.trim().isEmpty()) {
                    throw new IllegalArgumentException("Secret key is required for TOTP MFA");
                }
            }
            case "SMS" -> {
                if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                    throw new IllegalArgumentException("Phone number is required for SMS MFA");
                }
                validatePhoneNumber(phoneNumber);
            }
            case "EMAIL" -> {
                if (email == null || email.trim().isEmpty()) {
                    throw new IllegalArgumentException("Email is required for EMAIL MFA");
                }
                validateEmail(email);
            }
            case "HARDWARE_TOKEN" -> {
                if (deviceId == null || deviceId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Device ID is required for hardware token MFA");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported MFA type: " + mfaType + 
                ". Supported types: TOTP, SMS, EMAIL, HARDWARE_TOKEN");
        }
    }
    
    private void validatePhoneNumber(String phoneNumber) {
        // Basic phone number validation (UAE format)
        if (!phoneNumber.matches("^\\+971[0-9]{8,9}$")) {
            throw new IllegalArgumentException("Invalid UAE phone number format. Expected: +971XXXXXXXXX");
        }
    }
    
    private void validateEmail(String email) {
        // Basic email validation
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}