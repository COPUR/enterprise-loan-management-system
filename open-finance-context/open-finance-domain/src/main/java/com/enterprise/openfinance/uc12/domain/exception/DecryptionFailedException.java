package com.enterprise.openfinance.uc12.domain.exception;

public class DecryptionFailedException extends RuntimeException {

    public DecryptionFailedException(String message) {
        super(message);
    }
}
