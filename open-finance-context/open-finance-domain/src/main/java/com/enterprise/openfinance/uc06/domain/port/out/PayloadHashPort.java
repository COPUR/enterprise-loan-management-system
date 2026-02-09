package com.enterprise.openfinance.uc06.domain.port.out;

public interface PayloadHashPort {
    String hash(String payload);
}
