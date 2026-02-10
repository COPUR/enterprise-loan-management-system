package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.personalfinancialdata.domain.model.BalanceSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.port.out.BalanceReadPort;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisBalanceDocument;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository.AisBalanceMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "openfinance.personalfinancialdata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoBalanceReadAdapter implements BalanceReadPort {

    private final AisBalanceMongoRepository repository;

    public MongoBalanceReadAdapter(AisBalanceMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<BalanceSnapshot> findByAccountId(String accountId) {
        return repository.findByAccountId(accountId).stream()
                .map(MongoBalanceReadAdapter::toDomain)
                .sorted(Comparator.comparing(BalanceSnapshot::asOf).reversed())
                .toList();
    }

    private static BalanceSnapshot toDomain(AisBalanceDocument document) {
        return new BalanceSnapshot(
                document.accountId(),
                document.balanceType(),
                document.amount(),
                document.currency(),
                document.asOf()
        );
    }
}

