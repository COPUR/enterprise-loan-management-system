package com.enterprise.openfinance.uc06.infrastructure.security;

import com.enterprise.openfinance.uc06.domain.port.out.PayloadHashPort;
import com.enterprise.openfinance.uc06.infrastructure.config.Uc06SecurityProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class Sha256PayloadHashAdapter implements PayloadHashPort {

    private final String algorithm;

    public Sha256PayloadHashAdapter(Uc06SecurityProperties securityProperties) {
        this.algorithm = securityProperties.getPayloadHashAlgorithm();
    }

    @Override
    public String hash(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Hash algorithm not available: " + algorithm, exception);
        }
    }
}
