package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateTransactionReadPort;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateTransactionDocument;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository.CorporateTransactionMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Repository
@ConditionalOnProperty(prefix = "openfinance.businessfinancialdata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoCorporateTransactionReadAdapter implements CorporateTransactionReadPort {

    private final CorporateTransactionMongoRepository repository;

    public MongoCorporateTransactionReadAdapter(CorporateTransactionMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CorporateTransactionSnapshot> findByAccountIds(Set<String> accountIds) {
        return repository.findByAccountIdIn(accountIds).stream()
                .map(MongoCorporateTransactionReadAdapter::toDomain)
                .sorted(Comparator.comparing(CorporateTransactionSnapshot::bookingDateTime).reversed())
                .toList();
    }

    private static CorporateTransactionSnapshot toDomain(CorporateTransactionDocument document) {
        return new CorporateTransactionSnapshot(
                document.id(),
                document.accountId(),
                document.amount(),
                document.currency(),
                document.bookingDateTime(),
                document.transactionCode(),
                document.proprietaryCode(),
                document.description()
        );
    }
}
