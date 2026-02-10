package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateBalanceSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateBalanceReadPort;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateBalanceDocument;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository.CorporateBalanceMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "openfinance.businessfinancialdata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoCorporateBalanceReadAdapter implements CorporateBalanceReadPort {

    private final CorporateBalanceMongoRepository repository;

    public MongoCorporateBalanceReadAdapter(CorporateBalanceMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CorporateBalanceSnapshot> findByMasterAccountId(String masterAccountId) {
        return repository.findByMasterAccountId(masterAccountId).stream()
                .map(MongoCorporateBalanceReadAdapter::toDomain)
                .toList();
    }

    private static CorporateBalanceSnapshot toDomain(CorporateBalanceDocument document) {
        return new CorporateBalanceSnapshot(
                document.accountId(),
                document.balanceType(),
                document.amount(),
                document.currency(),
                document.asOf()
        );
    }
}
