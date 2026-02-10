package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import com.enterprise.openfinance.bankingmetadata.domain.model.FxDetails;
import com.enterprise.openfinance.bankingmetadata.domain.model.GeoLocation;
import com.enterprise.openfinance.bankingmetadata.domain.model.TransactionMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.TransactionMetadataReadPort;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.TransactionMetadataDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.TransactionMetadataMongoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "openfinance.bankingmetadata.persistence", name = "mode", havingValue = "mongodb", matchIfMissing = true)
public class MongoTransactionMetadataReadAdapter implements TransactionMetadataReadPort {

    private final TransactionMetadataMongoRepository repository;

    public MongoTransactionMetadataReadAdapter(TransactionMetadataMongoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TransactionMetadata> findByAccountId(String accountId) {
        return repository.findByAccountId(accountId).stream()
                .map(MongoTransactionMetadataReadAdapter::toDomain)
                .sorted(Comparator.comparing(TransactionMetadata::bookingDateTime).reversed())
                .toList();
    }

    private static TransactionMetadata toDomain(TransactionMetadataDocument document) {
        GeoLocation geoLocation = new GeoLocation(
                document.geoLocation().latitude(),
                document.geoLocation().longitude()
        );

        FxDetails fxDetails = null;
        if (document.fxDetails() != null) {
            fxDetails = new FxDetails(
                    document.fxDetails().exchangeRate(),
                    document.fxDetails().originalCurrency(),
                    document.fxDetails().targetCurrency()
            );
        }

        return new TransactionMetadata(
                document.id(),
                document.accountId(),
                document.bookingDateTime(),
                document.amount(),
                document.chargeAmount(),
                document.currency(),
                document.merchantName(),
                document.categoryCode(),
                geoLocation,
                fxDetails
        );
    }
}

