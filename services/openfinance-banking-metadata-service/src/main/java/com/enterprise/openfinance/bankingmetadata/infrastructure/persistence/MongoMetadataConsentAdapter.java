package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataConsentContext;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.MetadataConsentPort;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.MetadataConsentDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.MetadataConsentMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "openfinance.bankingmetadata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoMetadataConsentAdapter implements MetadataConsentPort {

    private final MetadataConsentMongoRepository repository;

    public MongoMetadataConsentAdapter(MetadataConsentMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MetadataConsentContext> findById(String consentId) {
        return repository.findById(consentId).map(MongoMetadataConsentAdapter::toDomain);
    }

    private static MetadataConsentContext toDomain(MetadataConsentDocument document) {
        return new MetadataConsentContext(
                document.id(),
                document.tppId(),
                document.scopes(),
                document.accountIds(),
                document.expiresAt()
        );
    }
}

