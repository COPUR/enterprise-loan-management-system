package com.enterprise.openfinance.uc05.application;

import com.enterprise.openfinance.uc05.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc05.domain.exception.ResourceNotFoundException;
import com.enterprise.openfinance.uc05.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.uc05.domain.model.CorporateAccountSnapshot;
import com.enterprise.openfinance.uc05.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.uc05.domain.model.CorporateBalanceSnapshot;
import com.enterprise.openfinance.uc05.domain.model.CorporateConsentContext;
import com.enterprise.openfinance.uc05.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.uc05.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.uc05.domain.model.CorporateTreasurySettings;
import com.enterprise.openfinance.uc05.domain.port.out.CorporateAccountReadPort;
import com.enterprise.openfinance.uc05.domain.port.out.CorporateBalanceReadPort;
import com.enterprise.openfinance.uc05.domain.port.out.CorporateCachePort;
import com.enterprise.openfinance.uc05.domain.port.out.CorporateConsentPort;
import com.enterprise.openfinance.uc05.domain.port.out.CorporateTransactionReadPort;
import com.enterprise.openfinance.uc05.domain.query.GetCorporateBalancesQuery;
import com.enterprise.openfinance.uc05.domain.query.GetCorporateTransactionsQuery;
import com.enterprise.openfinance.uc05.domain.query.ListCorporateAccountsQuery;
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
class CorporateTreasuryServiceTest {

    @Mock
    private CorporateConsentPort consentPort;

    @Mock
    private CorporateAccountReadPort accountReadPort;

    @Mock
    private CorporateBalanceReadPort balanceReadPort;

    @Mock
    private CorporateTransactionReadPort transactionReadPort;

    @Mock
    private CorporateCachePort cachePort;

    private CorporateTreasuryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-02-09T10:15:30Z"), ZoneOffset.UTC);
        service = new CorporateTreasuryService(
                consentPort,
                accountReadPort,
                balanceReadPort,
                transactionReadPort,
                cachePort,
                new CorporateTreasurySettings(Duration.ofSeconds(30), 100, 100),
                clock
        );
    }

    @Test
    void shouldReturnMasterAccountsAndPopulateCacheOnMiss() {
        when(consentPort.findById("CONS-TRSY-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getAccounts(anyString(), any())).thenReturn(Optional.empty());
        when(accountReadPort.findByCorporateId("CORP-001")).thenReturn(List.of(
                account("ACC-M-001", null, false),
                account("ACC-V-101", "ACC-M-001", true)
        ));

        CorporateAccountListResult result = service.listAccounts(new ListCorporateAccountsQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ix-1",
                false,
                null
        ));

        assertThat(result.cacheHit()).isFalse();
        assertThat(result.accounts()).hasSize(1);
        assertThat(result.accounts().getFirst().accountId()).isEqualTo("ACC-M-001");
        verify(cachePort).putAccounts(anyString(), any(), any());
    }

    @Test
    void shouldReturnAccountsFromCacheOnHit() {
        when(consentPort.findById("CONS-TRSY-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getAccounts(anyString(), any())).thenReturn(Optional.of(new CorporateAccountListResult(
                List.of(account("ACC-M-001", null, false)),
                false
        )));

        CorporateAccountListResult result = service.listAccounts(new ListCorporateAccountsQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ix-2",
                false,
                null
        ));

        assertThat(result.cacheHit()).isTrue();
        verify(accountReadPort, never()).findByCorporateId(anyString());
    }

    @Test
    void shouldFilterVirtualAccountsByMasterId() {
        when(consentPort.findById("CONS-TRSY-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getAccounts(anyString(), any())).thenReturn(Optional.empty());
        when(accountReadPort.findByCorporateId("CORP-001")).thenReturn(List.of(
                account("ACC-M-001", null, false),
                account("ACC-V-101", "ACC-M-001", true),
                account("ACC-V-102", "ACC-M-001", true),
                account("ACC-M-002", null, false)
        ));

        CorporateAccountListResult result = service.listAccounts(new ListCorporateAccountsQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ix-3",
                true,
                "ACC-M-001"
        ));

        assertThat(result.accounts())
                .extracting(CorporateAccountSnapshot::accountId)
                .containsExactly("ACC-M-001", "ACC-V-101", "ACC-V-102");
    }

    @Test
    void shouldReturnMaskedBalancesForRestrictedEntitlement() {
        when(consentPort.findById("CONS-TRSY-RESTRICTED")).thenReturn(Optional.of(restrictedConsent()));
        when(cachePort.getBalances(anyString(), any())).thenReturn(Optional.empty());
        when(balanceReadPort.findByMasterAccountId("ACC-M-001")).thenReturn(List.of(
                balance("ACC-M-001", "InterimAvailable", "15000.00")
        ));

        CorporateBalanceListResult result = service.getBalances(new GetCorporateBalancesQuery(
                "CONS-TRSY-RESTRICTED",
                "TPP-001",
                "ACC-M-001",
                "ix-4"
        ));

        assertThat(result.masked()).isTrue();
        assertThat(result.cacheHit()).isFalse();
    }

    @Test
    void shouldReturnBalancesFromCacheOnHit() {
        when(consentPort.findById("CONS-TRSY-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getBalances(anyString(), any())).thenReturn(Optional.of(new CorporateBalanceListResult(
                List.of(balance("ACC-M-001", "InterimBooked", "12000.00")),
                false,
                false
        )));

        CorporateBalanceListResult result = service.getBalances(new GetCorporateBalancesQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ACC-M-001",
                "ix-5"
        ));

        assertThat(result.cacheHit()).isTrue();
        verify(balanceReadPort, never()).findByMasterAccountId(anyString());
    }

    @Test
    void shouldFilterAndPaginateTransactions() {
        when(consentPort.findById("CONS-TRSY-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getTransactions(anyString(), any())).thenReturn(Optional.empty());
        when(transactionReadPort.findByAccountIds(Set.of("ACC-M-001"))).thenReturn(List.of(
                transaction("TX-001", "ACC-M-001", "2026-02-05T00:00:00Z", "SWEEP", "ZBA"),
                transaction("TX-002", "ACC-M-001", "2026-02-03T00:00:00Z", "BOOK", null),
                transaction("TX-003", "ACC-M-001", "2025-12-20T00:00:00Z", "BOOK", null)
        ));

        CorporatePagedResult<CorporateTransactionSnapshot> result = service.getTransactions(new GetCorporateTransactionsQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ix-6",
                "ACC-M-001",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T00:00:00Z"),
                1,
                1
        ));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().transactionId()).isEqualTo("TX-001");
        assertThat(result.items().getFirst().isSweeping()).isTrue();
        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void shouldReturnTransactionsAcrossAllowedAccountsWhenAccountFilterMissing() {
        when(consentPort.findById("CONS-TRSY-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getTransactions(anyString(), any())).thenReturn(Optional.empty());
        when(transactionReadPort.findByAccountIds(Set.of("ACC-M-001", "ACC-V-101", "ACC-V-102"))).thenReturn(List.of(
                transaction("TX-001", "ACC-V-101", "2026-02-05T00:00:00Z", "SWEEP", "ZBA")
        ));

        CorporatePagedResult<CorporateTransactionSnapshot> result = service.getTransactions(new GetCorporateTransactionsQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ix-7",
                null,
                null,
                null,
                1,
                100
        ));

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().accountId()).isEqualTo("ACC-V-101");
    }

    @Test
    void shouldRejectWhenScopeMissingOrBolaDetected() {
        when(consentPort.findById("CONS-TRSY-ACCOUNTS")).thenReturn(Optional.of(accountsOnlyConsent()));

        assertThatThrownBy(() -> service.getBalances(new GetCorporateBalancesQuery(
                "CONS-TRSY-ACCOUNTS",
                "TPP-001",
                "ACC-M-001",
                "ix-8"
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("ReadBalances");

        when(consentPort.findById("CONS-TRSY-001")).thenReturn(Optional.of(fullConsent()));
        assertThatThrownBy(() -> service.getBalances(new GetCorporateBalancesQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ACC-M-999",
                "ix-9"
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Resource not linked to consent");
    }

    @Test
    void shouldRejectWhenConsentMissingExpiredOrTppMismatch() {
        when(consentPort.findById("CONS-MISSING")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.listAccounts(new ListCorporateAccountsQuery(
                "CONS-MISSING",
                "TPP-001",
                "ix-10",
                false,
                null
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Consent not found");

        CorporateConsentContext expired = new CorporateConsentContext(
                "CONS-EXPIRED",
                "TPP-001",
                "CORP-001",
                "FULL",
                Set.of("READACCOUNTS"),
                Set.of("ACC-M-001"),
                Instant.parse("2020-01-01T00:00:00Z")
        );
        when(consentPort.findById("CONS-EXPIRED")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.listAccounts(new ListCorporateAccountsQuery(
                "CONS-EXPIRED",
                "TPP-001",
                "ix-11",
                false,
                null
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("expired");

        CorporateConsentContext foreignTpp = new CorporateConsentContext(
                "CONS-OTHER-TPP",
                "TPP-XYZ",
                "CORP-001",
                "FULL",
                Set.of("READACCOUNTS"),
                Set.of("ACC-M-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
        when(consentPort.findById("CONS-OTHER-TPP")).thenReturn(Optional.of(foreignTpp));
        assertThatThrownBy(() -> service.listAccounts(new ListCorporateAccountsQuery(
                "CONS-OTHER-TPP",
                "TPP-001",
                "ix-12",
                false,
                null
        ))).isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("participant mismatch");
    }

    @Test
    void shouldThrowNotFoundWhenBalancesMissing() {
        when(consentPort.findById("CONS-TRSY-001")).thenReturn(Optional.of(fullConsent()));
        when(cachePort.getBalances(anyString(), any())).thenReturn(Optional.empty());
        when(balanceReadPort.findByMasterAccountId("ACC-M-001")).thenReturn(List.of());

        assertThatThrownBy(() -> service.getBalances(new GetCorporateBalancesQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ACC-M-001",
                "ix-13"
        ))).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Balances not found");
    }

    private static CorporateConsentContext fullConsent() {
        return new CorporateConsentContext(
                "CONS-TRSY-001",
                "TPP-001",
                "CORP-001",
                "FULL",
                Set.of("READACCOUNTS", "READBALANCES", "READTRANSACTIONS"),
                Set.of("ACC-M-001", "ACC-V-101", "ACC-V-102"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
    }

    private static CorporateConsentContext restrictedConsent() {
        return new CorporateConsentContext(
                "CONS-TRSY-RESTRICTED",
                "TPP-001",
                "CORP-001",
                "RESTRICTED",
                Set.of("READACCOUNTS", "READBALANCES", "READTRANSACTIONS"),
                Set.of("ACC-M-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
    }

    private static CorporateConsentContext accountsOnlyConsent() {
        return new CorporateConsentContext(
                "CONS-TRSY-ACCOUNTS",
                "TPP-001",
                "CORP-001",
                "FULL",
                Set.of("READACCOUNTS"),
                Set.of("ACC-M-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
    }

    private static CorporateAccountSnapshot account(String accountId, String masterAccountId, boolean virtual) {
        return new CorporateAccountSnapshot(
                accountId,
                "CORP-001",
                masterAccountId,
                "AE210001000000123456789",
                "AED",
                "Enabled",
                virtual ? "Virtual" : "Current",
                virtual
        );
    }

    private static CorporateBalanceSnapshot balance(String accountId, String type, String amount) {
        return new CorporateBalanceSnapshot(
                accountId,
                type,
                new BigDecimal(amount),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }

    private static CorporateTransactionSnapshot transaction(
            String transactionId,
            String accountId,
            String bookingDate,
            String transactionCode,
            String proprietaryCode
    ) {
        return new CorporateTransactionSnapshot(
                transactionId,
                accountId,
                new BigDecimal("100.00"),
                "AED",
                Instant.parse(bookingDate),
                transactionCode,
                proprietaryCode,
                "description"
        );
    }
}
