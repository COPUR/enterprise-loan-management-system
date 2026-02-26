package com.enterprise.openfinance.consentauthorization.jwtlifecycle.infrastructure.persistence;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretRecord;
import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out.InternalSystemSecretPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(
        name = "openfinance.internal.secrets.storage",
        havingValue = "database",
        matchIfMissing = true
)
public class JpaInternalSystemSecretAdapter implements InternalSystemSecretPort {

    private final InternalSystemSecretJpaRepository repository;

    public JpaInternalSystemSecretAdapter(InternalSystemSecretJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<InternalSystemSecretRecord> findBySecretKey(String secretKey) {
        return repository.findBySecretKey(secretKey).map(InternalSystemSecretJpaEntity::toDomain);
    }

    @Override
    public InternalSystemSecretRecord save(InternalSystemSecretRecord record) {
        InternalSystemSecretJpaEntity entity = repository.findBySecretKey(record.secretKey())
                .orElseGet(() -> InternalSystemSecretJpaEntity.fromDomain(record));
        entity.mergeFrom(record);
        return repository.save(entity).toDomain();
    }
}
