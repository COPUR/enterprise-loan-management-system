package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateAccountDocument;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateBalanceDocument;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateConsentDocument;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.document.CorporateTransactionDocument;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository.CorporateAccountMongoRepository;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository.CorporateBalanceMongoRepository;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository.CorporateConsentMongoRepository;
import com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence.mongo.repository.CorporateTransactionMongoRepository;
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
        CorporateConsentMongoRepository repository = mock(CorporateConsentMongoRepository.class);
        when(repository.findById("CONS-1")).thenReturn(Optional.of(new CorporateConsentDocument(
                "CONS-1",
                "TPP-1",
                "CORP-1",
                "FULL",
                Set.of("READACCOUNTS"),
                Set.of("ACC-1"),
                Instant.parse("2099-01-01T00:00:00Z")
        )));

        MongoCorporateConsentAdapter adapter = new MongoCorporateConsentAdapter(repository);
        var consent = adapter.findById("CONS-1").orElseThrow();
        assertThat(consent.consentId()).isEqualTo("CONS-1");
        assertThat(consent.hasScope("ReadAccounts")).isTrue();
    }

    @Test
    void shouldMapAccountDocumentToDomain() {
        CorporateAccountMongoRepository repository = mock(CorporateAccountMongoRepository.class);
        when(repository.findByCorporateId("CORP-1")).thenReturn(List.of(
                new CorporateAccountDocument("ACC-2", "CORP-1", null, "AE22", "AED", "Enabled", "Current", false),
                new CorporateAccountDocument("ACC-1", "CORP-1", null, "AE21", "AED", "Enabled", "Current", false)
        ));
        when(repository.findById("ACC-2")).thenReturn(Optional.of(
                new CorporateAccountDocument("ACC-2", "CORP-1", null, "AE22", "AED", "Enabled", "Current", false)
        ));

        MongoCorporateAccountReadAdapter adapter = new MongoCorporateAccountReadAdapter(repository);
        assertThat(adapter.findByCorporateId("CORP-1")).extracting("accountId").containsExactly("ACC-1", "ACC-2");
        assertThat(adapter.findById("ACC-2")).isPresent();
    }

    @Test
    void shouldMapBalanceDocumentToDomain() {
        CorporateBalanceMongoRepository repository = mock(CorporateBalanceMongoRepository.class);
        when(repository.findByMasterAccountId("ACC-M-1")).thenReturn(List.of(
                new CorporateBalanceDocument(
                        "BAL-1",
                        "ACC-M-1",
                        "ACC-M-1",
                        "InterimAvailable",
                        new BigDecimal("100.00"),
                        "AED",
                        Instant.parse("2026-02-10T10:00:00Z")
                )
        ));

        MongoCorporateBalanceReadAdapter adapter = new MongoCorporateBalanceReadAdapter(repository);
        assertThat(adapter.findByMasterAccountId("ACC-M-1"))
                .singleElement()
                .extracting("balanceType")
                .isEqualTo("InterimAvailable");
    }

    @Test
    void shouldMapTransactionDocumentToDomain() {
        CorporateTransactionMongoRepository repository = mock(CorporateTransactionMongoRepository.class);
        when(repository.findByAccountIdIn(Set.of("ACC-1"))).thenReturn(List.of(
                new CorporateTransactionDocument(
                        "TX-1",
                        "ACC-1",
                        new BigDecimal("12.00"),
                        "AED",
                        Instant.parse("2026-02-10T10:00:00Z"),
                        "BOOK",
                        null,
                        "desc"
                )
        ));

        MongoCorporateTransactionReadAdapter adapter = new MongoCorporateTransactionReadAdapter(repository);
        assertThat(adapter.findByAccountIds(Set.of("ACC-1")))
                .singleElement()
                .extracting("transactionId")
                .isEqualTo("TX-1");
    }
}
