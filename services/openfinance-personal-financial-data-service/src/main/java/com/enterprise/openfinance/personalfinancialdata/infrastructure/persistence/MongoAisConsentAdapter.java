package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AisConsentContext;
import com.enterprise.openfinance.personalfinancialdata.domain.port.out.AisConsentPort;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisConsentDocument;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository.AisConsentMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "openfinance.personalfinancialdata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoAisConsentAdapter implements AisConsentPort {

    private final AisConsentMongoRepository repository;

    public MongoAisConsentAdapter(AisConsentMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AisConsentContext> findById(String consentId) {
        return repository.findById(consentId).map(MongoAisConsentAdapter::toDomain);
    }

    private static AisConsentContext toDomain(AisConsentDocument document) {
        return new AisConsentContext(
                document.id(),
                document.tppId(),
                document.psuId(),
                document.scopes(),
                document.accountIds(),
                document.expiresAt()
        );
    }
}

