package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out;

public interface InternalCredentialPort {

    boolean matches(String username, String password);
}

