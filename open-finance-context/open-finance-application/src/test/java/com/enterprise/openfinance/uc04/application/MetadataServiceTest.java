package com.enterprise.openfinance.uc04.application;

import com.enterprise.openfinance.uc04.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc04.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc04.domain.model.AccountSchemeMetadata;
import com.enterprise.openfinance.uc04.domain.model.FxDetails;
import com.enterprise.openfinance.uc04.domain.model.GeoLocation;
import com.enterprise.openfinance.uc04.domain.model.MetadataConsentContext;
import com.enterprise.openfinance.uc04.domain.model.MetadataItemResult;
import com.enterprise.openfinance.uc04.domain.model.MetadataListResult;
import com.enterprise.openfinance.uc04.domain.model.MetadataQueryResult;
import com.enterprise.openfinance.uc04.domain.model.MetadataSettings;
import com.enterprise.openfinance.uc04.domain.model.PartyMetadata;
import com.enterprise.openfinance.uc04.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.uc04.domain.model.TransactionMetadata;
import com.enterprise.openfinance.uc04.domain.port.out.AccountMetadataReadPort;
import com.enterprise.openfinance.uc04.domain.port.out.MetadataCachePort;
import com.enterprise.openfinance.uc04.domain.port.out.MetadataConsentPort;
import com.enterprise.openfinance.uc04.domain.port.out.PartyMetadataReadPort;
import com.enterprise.openfinance.uc04.domain.port.out.StandingOrderMetadataReadPort;
import com.enterprise.openfinance.uc04.domain.port.out.TransactionMetadataReadPort;
import com.enterprise.openfinance.uc04.domain.query.GetAccountMetadataQuery;
import com.enterprise.openfinance.uc04.domain.query.GetPartiesMetadataQuery;
import com.enterprise.openfinance.uc04.domain.query.GetStandingOrdersMetadataQuery;
import com.enterprise.openfinance.uc04.domain.query.GetTransactionMetadataQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {

    @Mock
    private MetadataConsentPort consentPort;

    @Mock
    private TransactionMetadataReadPort transactionReadPort;

    @Mock
    private PartyMetadataReadPort partyReadPort;

    @Mock
    private AccountMetadataReadPort accountReadPort;

    @Mock
    private StandingOrderMetadataReadPort standingOrderReadPort;

    @Mock
    private MetadataCachePort cachePort;

    private MetadataService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-02-09T10:15:30Z"), ZoneOffset.UTC);
        service = new MetadataService(
                consentPort,
                transactionReadPort,
                partyReadPort,
                accountReadPort,
                standingOrderReadPort,
                cachePort,
                new MetadataSettings(Duration.ofSeconds(30), 100, 100),
                clock
        );
    }

    @Test
    void shouldReturnTransactionsFromSourceAndPopulateCacheOnMiss() {
        when(consentPort.findById("CONS-META-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getTransactions(anyString(), any())).thenReturn(Optional.empty());
        when(transactionReadPort.findByAccountId("ACC-001")).thenReturn(List.of(
                transaction("TXN-001", "2026-01-10T00:00:00Z"),
                transaction("TXN-002", "2026-01-15T00:00:00Z"),
                transaction("TXN-003", "2025-12-15T00:00:00Z")
        ));

        MetadataQueryResult<TransactionMetadata> result = service.getTransactions(new GetTransactionMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                "ix-1",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T00:00:00Z"),
                1,
                1
        ));

        assertThat(result.cacheHit()).isFalse();
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().transactionId()).isEqualTo("TXN-002");
        assertThat(result.totalRecords()).isEqualTo(2);
        verify(cachePort).putTransactions(anyString(), any(), any());
    }

    @Test
    void shouldReturnTransactionsFromCacheOnHit() {
        when(consentPort.findById("CONS-META-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getTransactions(anyString(), any())).thenReturn(Optional.of(new MetadataQueryResult<>(
                List.of(transaction("TXN-001", "2026-01-10T00:00:00Z")),
                1,
                100,
                1,
                false
        )));

        MetadataQueryResult<TransactionMetadata> result = service.getTransactions(new GetTransactionMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                "ix-2",
                null,
                null,
                1,
                100
        ));

        assertThat(result.cacheHit()).isTrue();
        verify(transactionReadPort, never()).findByAccountId(anyString());
    }

    @Test
    void shouldRejectTransactionAccessWhenScopeMissing() {
        MetadataConsentContext consent = new MetadataConsentContext(
                "CONS-META-PARTY-ONLY",
                "TPP-001",
                Set.of("READPARTIES"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
        when(consentPort.findById("CONS-META-PARTY-ONLY")).thenReturn(Optional.of(consent));

        assertThatThrownBy(() -> service.getTransactions(new GetTransactionMetadataQuery(
                "CONS-META-PARTY-ONLY",
                "TPP-001",
                "ACC-001",
                "ix-3",
                null,
                null,
                1,
                100
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("ReadTransactions");
    }

    @Test
    void shouldRejectBolaWhenAccountNotLinkedToConsent() {
        when(consentPort.findById("CONS-META-001")).thenReturn(Optional.of(fullConsent()));

        assertThatThrownBy(() -> service.getParties(new GetPartiesMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                "ACC-999",
                "ix-4"
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Resource not linked to consent");
    }

    @Test
    void shouldReturnPartiesAndCache() {
        when(consentPort.findById("CONS-META-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getParties(anyString(), any())).thenReturn(Optional.empty());
        when(partyReadPort.findByAccountId("ACC-001")).thenReturn(List.of(party()));

        MetadataListResult<PartyMetadata> result = service.getParties(new GetPartiesMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                "ix-5"
        ));

        assertThat(result.cacheHit()).isFalse();
        assertThat(result.items()).hasSize(1);
        verify(cachePort).putParties(anyString(), any(), any());
    }

    @Test
    void shouldThrowNotFoundWhenPartiesMissing() {
        when(consentPort.findById("CONS-META-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getParties(anyString(), any())).thenReturn(Optional.empty());
        when(partyReadPort.findByAccountId("ACC-001")).thenReturn(List.of());

        assertThatThrownBy(() -> service.getParties(new GetPartiesMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                "ix-6"
        ))).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Parties metadata not found");
    }

    @Test
    void shouldReturnAccountMetadataFromCache() {
        when(consentPort.findById("CONS-META-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getAccount(anyString(), any())).thenReturn(Optional.of(new MetadataItemResult<>(
                new AccountSchemeMetadata("ACC-001", "IBAN", "MOB-123"),
                false
        )));

        MetadataItemResult<AccountSchemeMetadata> result = service.getAccountMetadata(new GetAccountMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                "ix-7"
        ));

        assertThat(result.cacheHit()).isTrue();
        assertThat(result.item().schemeName()).isEqualTo("IBAN");
        verify(accountReadPort, never()).findByAccountId(anyString());
    }

    @Test
    void shouldRejectWhenConsentMissingOrExpiredOrTppMismatch() {
        when(consentPort.findById("CONS-MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccountMetadata(new GetAccountMetadataQuery(
                "CONS-MISSING",
                "TPP-001",
                "ACC-001",
                "ix-8"
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Consent not found");

        MetadataConsentContext expired = new MetadataConsentContext(
                "CONS-EXPIRED",
                "TPP-001",
                Set.of("READMETADATA"),
                Set.of("ACC-001"),
                Instant.parse("2020-01-01T00:00:00Z")
        );
        when(consentPort.findById("CONS-EXPIRED")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.getAccountMetadata(new GetAccountMetadataQuery(
                "CONS-EXPIRED",
                "TPP-001",
                "ACC-001",
                "ix-9"
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("expired");

        MetadataConsentContext otherTpp = new MetadataConsentContext(
                "CONS-OTHER-TPP",
                "TPP-XYZ",
                Set.of("READMETADATA"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
        when(consentPort.findById("CONS-OTHER-TPP")).thenReturn(Optional.of(otherTpp));

        assertThatThrownBy(() -> service.getAccountMetadata(new GetAccountMetadataQuery(
                "CONS-OTHER-TPP",
                "TPP-001",
                "ACC-001",
                "ix-10"
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("participant mismatch");
    }

    @Test
    void shouldReturnStandingOrdersWithPagination() {
        when(consentPort.findById("CONS-META-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getStandingOrders(anyString(), any())).thenReturn(Optional.empty());
        when(standingOrderReadPort.findByAccountId("ACC-001")).thenReturn(List.of(
                standingOrder("SO-001", "2026-01-01T00:00:00Z"),
                standingOrder("SO-002", "2026-02-01T00:00:00Z")
        ));

        MetadataQueryResult<StandingOrderMetadata> result = service.getStandingOrders(new GetStandingOrdersMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                "ix-11",
                1,
                1
        ));

        assertThat(result.cacheHit()).isFalse();
        assertThat(result.items()).hasSize(1);
        assertThat(result.totalRecords()).isEqualTo(2);
    }

    private static MetadataConsentContext fullConsent() {
        return new MetadataConsentContext(
                "CONS-META-001",
                "TPP-001",
                Set.of("READTRANSACTIONS", "READPARTIES", "READMETADATA", "READSTANDINGORDERS"),
                Set.of("ACC-001", "ACC-002"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
    }

    private static TransactionMetadata transaction(String transactionId, String bookingDateTime) {
        return new TransactionMetadata(
                transactionId,
                "ACC-001",
                Instant.parse(bookingDateTime),
                new BigDecimal("100.00"),
                new BigDecimal("2.50"),
                "AED",
                "Merchant",
                "5411",
                new GeoLocation(25.2048, 55.2708),
                new FxDetails(new BigDecimal("0.27290"), "AED", "USD")
        );
    }

    private static PartyMetadata party() {
        return new PartyMetadata(
                "ACC-001",
                "Al Tareq Trading LLC",
                "VERIFIED",
                Instant.parse("2018-05-01T00:00:00Z")
        );
    }

    private static StandingOrderMetadata standingOrder(String id, String firstPaymentDate) {
        return new StandingOrderMetadata(
                id,
                "ACC-001",
                "EvryMnth",
                Instant.parse(firstPaymentDate),
                Instant.parse("2026-12-01T00:00:00Z"),
                new BigDecimal("500.00"),
                "AED"
        );
    }
}
