package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import com.enterprise.openfinance.bankingmetadata.domain.model.PartyMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.PartyMetadataReadPort;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.PartyMetadataDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.PartyMetadataMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "openfinance.bankingmetadata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoPartyMetadataReadAdapter implements PartyMetadataReadPort {

    private final PartyMetadataMongoRepository repository;

    public MongoPartyMetadataReadAdapter(PartyMetadataMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PartyMetadata> findByAccountId(String accountId) {
        return repository.findByAccountId(accountId).stream()
                .map(MongoPartyMetadataReadAdapter::toDomain)
                .sorted(Comparator.comparing(PartyMetadata::relationshipStartDate))
                .toList();
    }

    private static PartyMetadata toDomain(PartyMetadataDocument document) {
        return new PartyMetadata(
                document.accountId(),
                document.fullLegalName(),
                document.kycStatus(),
                document.relationshipStartDate()
        );
    }
}

