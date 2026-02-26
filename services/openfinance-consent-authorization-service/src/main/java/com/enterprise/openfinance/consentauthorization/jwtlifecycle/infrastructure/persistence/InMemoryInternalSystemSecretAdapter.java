package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.persistence;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretRecord;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalSystemSecretPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "openfinance.internal.secrets.storage", havingValue = "memory")
public class InMemoryInternalSystemSecretAdapter implements InternalSystemSecretPort {

    private final Map<String, InternalSystemSecretRecord> secrets = new ConcurrentHashMap<>();

    @Override
    public Optional<InternalSystemSecretRecord> findBySecretKey(String secretKey) {
        return Optional.ofNullable(secrets.get(secretKey));
    }

    @Override
    public InternalSystemSecretRecord save(InternalSystemSecretRecord record) {
        secrets.put(record.secretKey(), record);
        return record;
    }
}
