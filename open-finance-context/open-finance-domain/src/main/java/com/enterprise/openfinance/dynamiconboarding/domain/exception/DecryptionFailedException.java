package com.enterprise.openfinance.dynamiconboarding.domain.exception;

public class DecryptionFailedException extends RuntimeException {

    public DecryptionFailedException(String message) {
        super(message);
    }
}
