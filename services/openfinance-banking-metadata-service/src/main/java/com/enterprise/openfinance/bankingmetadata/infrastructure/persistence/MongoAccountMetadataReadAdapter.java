package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import com.enterprise.openfinance.bankingmetadata.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.AccountMetadataReadPort;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.AccountMetadataDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.AccountMetadataMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "openfinance.bankingmetadata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoAccountMetadataReadAdapter implements AccountMetadataReadPort {

    private final AccountMetadataMongoRepository repository;

    public MongoAccountMetadataReadAdapter(AccountMetadataMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AccountSchemeMetadata> findByAccountId(String accountId) {
        return repository.findById(accountId).map(MongoAccountMetadataReadAdapter::toDomain);
    }

    private static AccountSchemeMetadata toDomain(AccountMetadataDocument document) {
        return new AccountSchemeMetadata(
                document.id(),
                document.schemeName(),
                document.secondaryIdentification()
        );
    }
}

