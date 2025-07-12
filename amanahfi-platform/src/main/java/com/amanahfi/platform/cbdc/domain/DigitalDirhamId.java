package com.amanahfi.platform.cbdc.domain;

import lombok.Value;
import java.util.Objects;
import java.util.UUID;

/**
 * Strongly-typed identifier for Digital Dirham CBDC
 */
@Value
public class DigitalDirhamId {
    String value;
    
    private DigitalDirhamId(String value) {
        this.value = Objects.requireNonNull(value, "Digital Dirham ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Digital Dirham ID cannot be empty");
        }
    }
    
    public static DigitalDirhamId of(String value) {
        return new DigitalDirhamId(value);
    }
    
    public static DigitalDirhamId generate() {
        return new DigitalDirhamId("DD-" + UUID.randomUUID().toString());
    }
    
    public static DigitalDirhamId fromWalletId(String walletId) {
        return new DigitalDirhamId("DD-WALLET-" + walletId);
    }
}