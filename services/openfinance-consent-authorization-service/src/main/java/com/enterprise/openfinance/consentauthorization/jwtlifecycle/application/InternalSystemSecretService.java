package com.enterprise.openfinance.consentauthorization.jwtlifecycle.application;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.command.UpsertInternalSystemSecretCommand;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretRecord;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretView;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.in.InternalSystemSecretUseCase;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalSystemSecretPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class InternalSystemSecretService implements InternalSystemSecretUseCase {

    private static final String DEFAULT_CLASSIFICATION = "INTERNAL";
    private static final int SALT_BYTES = 16;

    private final InternalSystemSecretPort secretPort;
    private final Clock clock;
    private final SecureRandom secureRandom;

    @Autowired
    public InternalSystemSecretService(InternalSystemSecretPort secretPort, Clock clock) {
        this(secretPort, clock, new SecureRandom());
    }

    InternalSystemSecretService(InternalSystemSecretPort secretPort, Clock clock, SecureRandom secureRandom) {
        this.secretPort = secretPort;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    @Override
    public InternalSystemSecretView upsert(UpsertInternalSystemSecretCommand command) {
        Instant now = Instant.now(clock);
        String key = normalizeKey(command.secretKey());
        String classification = normalizeClassification(command.classification());

        Optional<InternalSystemSecretRecord> existing = secretPort.findBySecretKey(key);
        long version = existing.map(record -> record.version() + 1).orElse(1L);
        Instant createdAt = existing.map(InternalSystemSecretRecord::createdAt).orElse(now);

        String salt = generateSalt();
        String hash = hash(command.secretValue(), salt);
        String maskedValue = mask(command.secretValue());

        InternalSystemSecretRecord saved = secretPort.save(new InternalSystemSecretRecord(
                key,
                maskedValue,
                hash,
                salt,
                classification,
                version,
                createdAt,
                now
        ));
        return saved.toView();
    }

    @Override
    public Optional<InternalSystemSecretView> getMetadata(String secretKey) {
        String key = normalizeKey(secretKey);
        return secretPort.findBySecretKey(key).map(InternalSystemSecretRecord::toView);
    }

    static String normalizeKey(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("Secret key is required");
        }
        return secretKey.trim().toUpperCase();
    }

    static String normalizeClassification(String classification) {
        if (classification == null || classification.isBlank()) {
            return DEFAULT_CLASSIFICATION;
        }
        return classification.trim().toUpperCase();
    }

    static String mask(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalArgumentException("Secret value is required");
        }

        if (rawSecret.length() <= 4) {
            return "****";
        }

        String prefix = rawSecret.substring(0, 2);
        String suffix = rawSecret.substring(rawSecret.length() - 2);
        return prefix + "****" + suffix;
    }

    private String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    static String hash(String secretValue, String salt) {
        if (secretValue == null || secretValue.isBlank()) {
            throw new IllegalArgumentException("Secret value is required");
        }
        if (salt == null || salt.isBlank()) {
            throw new IllegalArgumentException("Salt is required");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + ":" + secretValue).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
