package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisAccountDocument;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisBalanceDocument;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisConsentDocument;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.document.AisTransactionDocument;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository.AisAccountMongoRepository;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository.AisBalanceMongoRepository;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository.AisConsentMongoRepository;
import com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence.mongo.repository.AisTransactionMongoRepository;
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
        AisConsentMongoRepository repository = mock(AisConsentMongoRepository.class);
        when(repository.findById("CONS-1")).thenReturn(Optional.of(new AisConsentDocument(
                "CONS-1",
                "TPP-1",
                "PSU-1",
                Set.of("READACCOUNTS", "READBALANCES"),
                Set.of("ACC-1"),
                Instant.parse("2099-01-01T00:00:00Z")
        )));

        MongoAisConsentAdapter adapter = new MongoAisConsentAdapter(repository);
        var consent = adapter.findById("CONS-1").orElseThrow();

        assertThat(consent.consentId()).isEqualTo("CONS-1");
        assertThat(consent.hasScope("ReadAccounts")).isTrue();
        assertThat(consent.hasScope("ReadBalances")).isTrue();
    }

    @Test
    void shouldMapAccountDocumentToDomain() {
        AisAccountMongoRepository repository = mock(AisAccountMongoRepository.class);
        when(repository.findByPsuId("PSU-1")).thenReturn(List.of(
                new AisAccountDocument("ACC-2", "PSU-1", "AE2", "USD", "Current", "Enabled", "IBAN", "Account B"),
                new AisAccountDocument("ACC-1", "PSU-1", "AE1", "AED", "Current", "Enabled", "IBAN", "Account A")
        ));
        when(repository.findById("ACC-2")).thenReturn(Optional.of(
                new AisAccountDocument("ACC-2", "PSU-1", "AE2", "USD", "Current", "Enabled", "IBAN", "Account B")
        ));

        MongoAccountReadAdapter adapter = new MongoAccountReadAdapter(repository);

        assertThat(adapter.findByPsuId("PSU-1")).extracting("accountId").containsExactly("ACC-1", "ACC-2");
        assertThat(adapter.findById("ACC-2")).isPresent();
    }

    @Test
    void shouldMapBalanceDocumentToDomain() {
        AisBalanceMongoRepository repository = mock(AisBalanceMongoRepository.class);
        when(repository.findByAccountId("ACC-1")).thenReturn(List.of(
                new AisBalanceDocument("BAL-1", "ACC-1", "InterimAvailable", new BigDecimal("100.00"), "AED",
                        Instant.parse("2026-02-10T10:00:00Z"))
        ));

        MongoBalanceReadAdapter adapter = new MongoBalanceReadAdapter(repository);

        assertThat(adapter.findByAccountId("ACC-1"))
                .singleElement()
                .extracting("balanceType")
                .isEqualTo("InterimAvailable");
    }

    @Test
    void shouldMapTransactionDocumentToDomain() {
        AisTransactionMongoRepository repository = mock(AisTransactionMongoRepository.class);
        when(repository.findByAccountId("ACC-1")).thenReturn(List.of(
                new AisTransactionDocument(
                        "TX-1",
                        "ACC-1",
                        new BigDecimal("12.00"),
                        "AED",
                        Instant.parse("2026-02-10T10:00:00Z"),
                        Instant.parse("2026-02-10T10:00:00Z"),
                        "Debit",
                        "Booked",
                        "Merchant A"
                )
        ));

        MongoTransactionReadAdapter adapter = new MongoTransactionReadAdapter(repository);

        assertThat(adapter.findByAccountId("ACC-1"))
                .singleElement()
                .extracting("transactionId")
                .isEqualTo("TX-1");
    }
}

