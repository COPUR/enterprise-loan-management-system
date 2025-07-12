package com.amanahfi.platform.shared.exception;

import com.amanahfi.platform.shared.i18n.MessageService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import java.util.Map;

/**
 * Exception for security-related violations
 */
@Getter
@Configurable
public class SecurityException extends AmanahFiException {
    
    @Autowired
    private transient MessageService messageService;
    
    private final String securityPolicy;
    private final String userId;
    private final String clientIp;
    private final Object[] messageParams;
    
    public SecurityException(
            String errorCode,
            String securityPolicy,
            String userId,
            String clientIp,
            String userMessage,
            String technicalMessage,
            ErrorSeverity severity,
            Map<String, Object> errorContext,
            Object... messageParams) {
        
        super(errorCode, userMessage, technicalMessage, severity, ErrorCategory.SECURITY, errorContext);
        this.securityPolicy = securityPolicy;
        this.userId = userId;
        this.clientIp = clientIp;
        this.messageParams = messageParams != null ? messageParams.clone() : new Object[0];
    }
    
    public SecurityException(
            String errorCode,
            String securityPolicy,
            String userId,
            String clientIp,
            String userMessage,
            String technicalMessage,
            Map<String, Object> errorContext,
            Object... messageParams) {
        
        this(errorCode, securityPolicy, userId, clientIp, userMessage, technicalMessage, 
             ErrorSeverity.HIGH, errorContext, messageParams);
    }
    
    public SecurityException(
            String errorCode,
            String securityPolicy,
            String userMessage,
            String technicalMessage,
            Object... messageParams) {
        
        this(errorCode, securityPolicy, null, null, userMessage, technicalMessage, Map.of(), messageParams);
    }
    
    @Override
    public String getLocalizedMessage(String languageCode) {
        if (messageService == null) {
            return getUserMessage(); // Fallback if DI not available
        }
        
        try {
            // Try to get localized message using error code as key
            if (messageService.hasMessage(getErrorCode(), languageCode)) {
                return messageService.getMessage(getErrorCode(), languageCode, messageParams);
            }
            
            // Try with security policy as key
            if (messageService.hasMessage(securityPolicy, languageCode)) {
                return messageService.getMessage(securityPolicy, languageCode, messageParams);
            }
            
            // Fallback to original user message
            return getUserMessage();
            
        } catch (Exception e) {
            return getUserMessage(); // Fallback on any error
        }
    }
    
    // Common security exceptions factory methods
    
    public static SecurityException authenticationRequired() {
        return new SecurityException(
            "AUTHENTICATION_REQUIRED",
            "authentication.required",
            "Authentication is required to access this resource",
            "User attempted to access protected resource without authentication"
        );
    }
    
    public static SecurityException invalidCredentials(String username, String clientIp) {
        return new SecurityException(
            "INVALID_CREDENTIALS",
            "authentication.invalid",
            username,
            clientIp,
            "Invalid username or password",
            "Authentication failed for user: " + username + " from IP: " + clientIp,
            ErrorSeverity.MEDIUM,
            Map.of(
                "username", username,
                "clientIp", clientIp,
                "attempts", 1
            ),
            username
        );
    }
    
    public static SecurityException accessDenied(String userId, String resource, String action) {
        return new SecurityException(
            "ACCESS_DENIED",
            "authorization.access.denied",
            userId,
            null,
            "Access denied to requested resource",
            "User " + userId + " denied access to " + resource + " for action " + action,
            ErrorSeverity.MEDIUM,
            Map.of(
                "userId", userId,
                "resource", resource,
                "action", action
            ),
            userId, resource, action
        );
    }
    
    public static SecurityException insufficientPrivileges(String userId, String requiredRole) {
        return new SecurityException(
            "INSUFFICIENT_PRIVILEGES",
            "authorization.insufficient.privileges",
            userId,
            null,
            "Insufficient privileges to perform this operation",
            "User " + userId + " lacks required role: " + requiredRole,
            Map.of(
                "userId", userId,
                "requiredRole", requiredRole
            ),
            userId, requiredRole
        );
    }
    
    public static SecurityException sessionExpired(String sessionId, String userId) {
        return new SecurityException(
            "SESSION_EXPIRED",
            "session.expired",
            userId,
            null,
            "Your session has expired, please login again",
            "Session " + sessionId + " for user " + userId + " has expired",
            ErrorSeverity.LOW,
            Map.of(
                "sessionId", sessionId,
                "userId", userId
            ),
            sessionId
        );
    }
    
    public static SecurityException accountLocked(String userId, String reason) {
        return new SecurityException(
            "ACCOUNT_LOCKED",
            "account.locked",
            userId,
            null,
            "Your account has been locked",
            "Account " + userId + " is locked: " + reason,
            ErrorSeverity.HIGH,
            Map.of(
                "userId", userId,
                "reason", reason
            ),
            userId, reason
        );
    }
    
    public static SecurityException mfaRequired(String userId) {
        return new SecurityException(
            "MFA_REQUIRED",
            "mfa.required",
            userId,
            null,
            "Multi-factor authentication is required",
            "MFA required for user: " + userId,
            ErrorSeverity.MEDIUM,
            Map.of("userId", userId),
            userId
        );
    }
    
    public static SecurityException invalidMfaCode(String userId) {
        return new SecurityException(
            "INVALID_MFA_CODE",
            "mfa.invalid.code",
            userId,
            null,
            "Invalid MFA verification code",
            "Invalid MFA code provided by user: " + userId,
            Map.of("userId", userId),
            userId
        );
    }
    
    public static SecurityException certificateValidationFailed(String subject, String reason) {
        return new SecurityException(
            "CERTIFICATE_VALIDATION_FAILED",
            "certificate.validation.failed",
            null,
            null,
            "Client certificate validation failed",
            "Certificate validation failed for subject '" + subject + "': " + reason,
            ErrorSeverity.HIGH,
            Map.of(
                "subject", subject,
                "reason", reason,
                "category", "MTLS"
            ),
            subject, reason
        );
    }
    
    public static SecurityException dpopValidationFailed(String tokenId, String reason) {
        return new SecurityException(
            "DPOP_VALIDATION_FAILED",
            "dpop.validation.failed",
            null,
            null,
            "DPoP token validation failed",
            "DPoP validation failed for token " + tokenId + ": " + reason,
            ErrorSeverity.HIGH,
            Map.of(
                "tokenId", tokenId,
                "reason", reason,
                "category", "DPOP"
            ),
            tokenId, reason
        );
    }
    
    public static SecurityException suspiciousActivity(String userId, String clientIp, String activity) {
        return new SecurityException(
            "SUSPICIOUS_ACTIVITY",
            "security.suspicious.activity",
            userId,
            clientIp,
            "Suspicious activity detected",
            "Suspicious activity detected for user " + userId + " from IP " + clientIp + ": " + activity,
            ErrorSeverity.CRITICAL,
            Map.of(
                "userId", userId,
                "clientIp", clientIp,
                "activity", activity,
                "category", "FRAUD_DETECTION"
            ),
            userId, activity
        );
    }
    
    public static SecurityException rateLimit(String clientIp, String operation, int limit) {
        return new SecurityException(
            "RATE_LIMIT_EXCEEDED",
            "security.rate.limit.exceeded",
            null,
            clientIp,
            "Too many requests, please try again later",
            "Rate limit exceeded for IP " + clientIp + " on operation " + operation + " (limit: " + limit + ")",
            ErrorSeverity.MEDIUM,
            Map.of(
                "clientIp", clientIp,
                "operation", operation,
                "limit", limit
            ),
            operation, limit
        );
    }
    
    public static SecurityException tokenReplay(String tokenId, String clientIp) {
        return new SecurityException(
            "TOKEN_REPLAY_DETECTED",
            "security.token.replay",
            null,
            clientIp,
            "Security violation detected",
            "Token replay attack detected for token " + tokenId + " from IP " + clientIp,
            ErrorSeverity.CRITICAL,
            Map.of(
                "tokenId", tokenId,
                "clientIp", clientIp,
                "category", "REPLAY_ATTACK"
            ),
            tokenId
        );
    }
}