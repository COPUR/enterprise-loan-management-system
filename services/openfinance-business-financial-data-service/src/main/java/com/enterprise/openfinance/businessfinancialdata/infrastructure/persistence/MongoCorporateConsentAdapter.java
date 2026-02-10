package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateConsentContext;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateConsentPort;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateConsentDocument;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository.CorporateConsentMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "openfinance.businessfinancialdata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoCorporateConsentAdapter implements CorporateConsentPort {

    private final CorporateConsentMongoRepository repository;

    public MongoCorporateConsentAdapter(CorporateConsentMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CorporateConsentContext> findById(String consentId) {
        return repository.findById(consentId).map(MongoCorporateConsentAdapter::toDomain);
    }

    private static CorporateConsentContext toDomain(CorporateConsentDocument document) {
        return new CorporateConsentContext(
                document.id(),
                document.tppId(),
                document.corporateId(),
                document.entitlement(),
                document.scopes(),
                document.accountIds(),
                document.expiresAt()
        );
    }
}
