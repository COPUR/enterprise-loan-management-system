package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.personalfinancialdata.domain.model.TransactionSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.port.out.TransactionReadPort;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisTransactionDocument;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository.AisTransactionMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "openfinance.personalfinancialdata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoTransactionReadAdapter implements TransactionReadPort {

    private final AisTransactionMongoRepository repository;

    public MongoTransactionReadAdapter(AisTransactionMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TransactionSnapshot> findByAccountId(String accountId) {
        return repository.findByAccountId(accountId).stream()
                .map(MongoTransactionReadAdapter::toDomain)
                .sorted(Comparator.comparing(TransactionSnapshot::bookingDateTime).reversed())
                .toList();
    }

    private static TransactionSnapshot toDomain(AisTransactionDocument document) {
        return new TransactionSnapshot(
                document.id(),
                document.accountId(),
                document.amount(),
                document.currency(),
                document.bookingDateTime(),
                document.valueDateTime(),
                document.creditDebitIndicator(),
                document.status(),
                document.merchantName()
        );
    }
}

