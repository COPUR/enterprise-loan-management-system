package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateAccountSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateAccountReadPort;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateAccountDocument;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository.CorporateAccountMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "openfinance.businessfinancialdata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoCorporateAccountReadAdapter implements CorporateAccountReadPort {

    private final CorporateAccountMongoRepository repository;

    public MongoCorporateAccountReadAdapter(CorporateAccountMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CorporateAccountSnapshot> findByCorporateId(String corporateId) {
        return repository.findByCorporateId(corporateId).stream()
                .map(MongoCorporateAccountReadAdapter::toDomain)
                .sorted(Comparator.comparing(CorporateAccountSnapshot::accountId))
                .toList();
    }

    @Override
    public Optional<CorporateAccountSnapshot> findById(String accountId) {
        return repository.findById(accountId).map(MongoCorporateAccountReadAdapter::toDomain);
    }

    private static CorporateAccountSnapshot toDomain(CorporateAccountDocument document) {
        return new CorporateAccountSnapshot(
                document.id(),
                document.corporateId(),
                document.masterAccountId(),
                document.iban(),
                document.currency(),
                document.status(),
                document.accountType(),
                document.virtual()
        );
    }
}
