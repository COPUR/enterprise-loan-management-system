package com.enterprise.openfinance.paymentinitiation.domain.port.out;

public interface PayloadHashPort {
    String hash(String payload);
}
