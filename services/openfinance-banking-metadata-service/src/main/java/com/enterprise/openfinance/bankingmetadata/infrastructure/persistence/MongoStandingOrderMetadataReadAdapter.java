package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import com.enterprise.openfinance.bankingmetadata.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.StandingOrderMetadataReadPort;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.StandingOrderMetadataDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.StandingOrderMetadataMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "openfinance.bankingmetadata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoStandingOrderMetadataReadAdapter implements StandingOrderMetadataReadPort {

    private final StandingOrderMetadataMongoRepository repository;

    public MongoStandingOrderMetadataReadAdapter(StandingOrderMetadataMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<StandingOrderMetadata> findByAccountId(String accountId) {
        return repository.findByAccountId(accountId).stream()
                .map(MongoStandingOrderMetadataReadAdapter::toDomain)
                .sorted(Comparator.comparing(StandingOrderMetadata::firstPaymentDate))
                .toList();
    }

    @Override
    public List<StandingOrderMetadata> findAll() {
        return repository.findAll().stream()
                .map(MongoStandingOrderMetadataReadAdapter::toDomain)
                .sorted(Comparator.comparing(StandingOrderMetadata::firstPaymentDate))
                .toList();
    }

    private static StandingOrderMetadata toDomain(StandingOrderMetadataDocument document) {
        return new StandingOrderMetadata(
                document.id(),
                document.accountId(),
                document.frequency(),
                document.firstPaymentDate(),
                document.finalPaymentDate(),
                document.amount(),
                document.currency()
        );
    }
}

