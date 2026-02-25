package com.masrufi.framework.infrastructure.security;

/**
 * Local stub replacing
 * {@code com.bank.infrastructure.security.FAPISecurityException}.
 *
 * <p>
 * The security files in this package are excluded from Gradle compilation; this
 * stub
 * exists solely to keep the IDE error-free while the modularisation work is in
 * progress.
 * </p>
 */
public class FAPISecurityException extends RuntimeException {

    private final String errorCode;

    public FAPISecurityException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public FAPISecurityException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
