package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.AccountMetadataDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.MetadataConsentDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.PartyMetadataDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.StandingOrderMetadataDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.document.TransactionMetadataDocument;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.AccountMetadataMongoRepository;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.MetadataConsentMongoRepository;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.PartyMetadataMongoRepository;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.StandingOrderMetadataMongoRepository;
import com.enterprise.openfinance.bankingmetadata.infrastructure.persistence.mongo.repository.TransactionMetadataMongoRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class MongoPersistenceAdaptersTest {

    @Test
    void shouldMapConsentDocumentToDomain() {
        MetadataConsentMongoRepository repository = mock(MetadataConsentMongoRepository.class);
        when(repository.findById("CONS-1")).thenReturn(Optional.of(new MetadataConsentDocument(
                "CONS-1",
                "TPP-1",
                Set.of("READTRANSACTIONS"),
                Set.of("ACC-1"),
                Instant.parse("2099-01-01T00:00:00Z")
        )));

        MongoMetadataConsentAdapter adapter = new MongoMetadataConsentAdapter(repository);
        var consent = adapter.findById("CONS-1").orElseThrow();

        assertThat(consent.consentId()).isEqualTo("CONS-1");
        assertThat(consent.hasScope("ReadTransactions")).isTrue();
    }

    @Test
    void shouldMapAccountDocumentToDomain() {
        AccountMetadataMongoRepository repository = mock(AccountMetadataMongoRepository.class);
        when(repository.findById("ACC-1")).thenReturn(Optional.of(
                new AccountMetadataDocument("ACC-1", "IBAN", "MOBILE:+9715001")
        ));

        MongoAccountMetadataReadAdapter adapter = new MongoAccountMetadataReadAdapter(repository);

        assertThat(adapter.findByAccountId("ACC-1")).isPresent();
        assertThat(adapter.findByAccountId("ACC-1").orElseThrow().schemeName()).isEqualTo("IBAN");
    }

    @Test
    void shouldMapPartyDocumentsToDomain() {
        PartyMetadataMongoRepository repository = mock(PartyMetadataMongoRepository.class);
        when(repository.findByAccountId("ACC-1")).thenReturn(List.of(
                new PartyMetadataDocument("PARTY-1", "ACC-1", "Alpha", "VERIFIED", Instant.parse("2020-01-01T00:00:00Z")),
                new PartyMetadataDocument("PARTY-2", "ACC-1", "Bravo", "VERIFIED", Instant.parse("2019-01-01T00:00:00Z"))
        ));

        MongoPartyMetadataReadAdapter adapter = new MongoPartyMetadataReadAdapter(repository);

        assertThat(adapter.findByAccountId("ACC-1"))
                .extracting("fullLegalName")
                .containsExactly("Bravo", "Alpha");
    }

    @Test
    void shouldMapStandingOrderDocumentsToDomain() {
        StandingOrderMetadataMongoRepository repository = mock(StandingOrderMetadataMongoRepository.class);
        when(repository.findByAccountId("ACC-1")).thenReturn(List.of(
                new StandingOrderMetadataDocument("SO-1", "ACC-1", "EvryMnth",
                        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-01T00:00:00Z"),
                        new BigDecimal("100.00"), "AED")
        ));
        when(repository.findAll()).thenReturn(List.of(
                new StandingOrderMetadataDocument("SO-1", "ACC-1", "EvryMnth",
                        Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-12-01T00:00:00Z"),
                        new BigDecimal("100.00"), "AED")
        ));

        MongoStandingOrderMetadataReadAdapter adapter = new MongoStandingOrderMetadataReadAdapter(repository);

        assertThat(adapter.findByAccountId("ACC-1")).singleElement().extracting("standingOrderId").isEqualTo("SO-1");
        assertThat(adapter.findAll()).singleElement().extracting("standingOrderId").isEqualTo("SO-1");
    }

    @Test
    void shouldMapTransactionDocumentsToDomain() {
        TransactionMetadataMongoRepository repository = mock(TransactionMetadataMongoRepository.class);
        when(repository.findByAccountId("ACC-1")).thenReturn(List.of(
                new TransactionMetadataDocument(
                        "TX-1",
                        "ACC-1",
                        Instant.parse("2026-02-10T10:00:00Z"),
                        new BigDecimal("50.00"),
                        new BigDecimal("0.50"),
                        "AED",
                        "Merchant A",
                        "5411",
                        new TransactionMetadataDocument.GeoLocationDocument(25.2, 55.3),
                        new TransactionMetadataDocument.FxDetailsDocument(
                                new BigDecimal("0.272900"),
                                "AED",
                                "USD"
                        )
                )
        ));

        MongoTransactionMetadataReadAdapter adapter = new MongoTransactionMetadataReadAdapter(repository);

        assertThat(adapter.findByAccountId("ACC-1"))
                .singleElement()
                .extracting("transactionId")
                .isEqualTo("TX-1");
    }
}

