package com.enterprise.openfinance.paymentinitiation.domain.port.out;

public interface JwsSignatureValidationPort {
    boolean isValid(String detachedJwsSignature, String payload);
}
