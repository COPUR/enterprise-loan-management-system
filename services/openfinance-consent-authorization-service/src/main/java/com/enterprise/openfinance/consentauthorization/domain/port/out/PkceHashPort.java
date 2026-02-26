package com.enterprise.openfinance.consentauthorization.domain.port.out;

public interface PkceHashPort {

    byte[] sha256(byte[] payload);
}

