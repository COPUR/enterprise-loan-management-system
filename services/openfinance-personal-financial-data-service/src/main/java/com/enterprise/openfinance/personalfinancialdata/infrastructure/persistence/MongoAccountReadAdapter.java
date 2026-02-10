package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AccountSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.port.out.AccountReadPort;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisAccountDocument;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository.AisAccountMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "openfinance.personalfinancialdata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoAccountReadAdapter implements AccountReadPort {

    private final AisAccountMongoRepository repository;

    public MongoAccountReadAdapter(AisAccountMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AccountSnapshot> findByPsuId(String psuId) {
        return repository.findByPsuId(psuId).stream()
                .map(MongoAccountReadAdapter::toDomain)
                .sorted(Comparator.comparing(AccountSnapshot::accountId))
                .toList();
    }

    @Override
    public Optional<AccountSnapshot> findById(String accountId) {
        return repository.findById(accountId).map(MongoAccountReadAdapter::toDomain);
    }

    private static AccountSnapshot toDomain(AisAccountDocument document) {
        return new AccountSnapshot(
                document.id(),
                document.psuId(),
                document.iban(),
                document.currency(),
                document.accountType(),
                document.status(),
                document.schemeName(),
                document.name()
        );
    }
}

