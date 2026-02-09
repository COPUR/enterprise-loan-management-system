package com.enterprise.openfinance.uc06.domain.port.out;

public interface JwsSignatureValidationPort {
    boolean isValid(String detachedJwsSignature, String payload);
}
