package com.enterprise.openfinance.uc02.application;

import com.enterprise.openfinance.uc02.domain.exception.ForbiddenException;
import com.enterprise.openfinance.uc02.domain.model.AccountListResult;
import com.enterprise.openfinance.uc02.domain.model.AccountSnapshot;
import com.enterprise.openfinance.uc02.domain.model.AisConsentContext;
import com.enterprise.openfinance.uc02.domain.model.AisSettings;
import com.enterprise.openfinance.uc02.domain.model.BalanceListResult;
import com.enterprise.openfinance.uc02.domain.model.BalanceSnapshot;
import com.enterprise.openfinance.uc02.domain.model.PagedResult;
import com.enterprise.openfinance.uc02.domain.model.TransactionSnapshot;
import com.enterprise.openfinance.uc02.domain.port.out.AccountReadPort;
import com.enterprise.openfinance.uc02.domain.port.out.AisCachePort;
import com.enterprise.openfinance.uc02.domain.port.out.AisConsentPort;
import com.enterprise.openfinance.uc02.domain.port.out.BalanceReadPort;
import com.enterprise.openfinance.uc02.domain.port.out.TransactionReadPort;
import com.enterprise.openfinance.uc02.domain.query.GetAccountQuery;
import com.enterprise.openfinance.uc02.domain.query.GetBalancesQuery;
import com.enterprise.openfinance.uc02.domain.query.GetTransactionsQuery;
import com.enterprise.openfinance.uc02.domain.query.ListAccountsQuery;
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
class AccountInformationServiceTest {

    @Mock
    private AisConsentPort consentPort;

    @Mock
    private AccountReadPort accountReadPort;

    @Mock
    private BalanceReadPort balanceReadPort;

    @Mock
    private TransactionReadPort transactionReadPort;

    @Mock
    private AisCachePort cachePort;

    private Clock clock;
    private AccountInformationService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-02-09T10:15:30Z"), ZoneOffset.UTC);
        service = new AccountInformationService(
                consentPort,
                accountReadPort,
                balanceReadPort,
                transactionReadPort,
                cachePort,
                new AisSettings(Duration.ofSeconds(30), 100, 100),
                clock
        );
    }

    @Test
    void shouldReturnAccountsFromSourceAndPopulateCacheOnMiss() {
        AisConsentContext consent = fullConsent();
        when(consentPort.findById("CONS-AIS-001")).thenReturn(Optional.of(consent));
        when(cachePort.getAccounts(anyString(), any())).thenReturn(Optional.empty());
        when(accountReadPort.findByPsuId("PSU-001")).thenReturn(List.of(account("ACC-001"), account("ACC-002")));

        AccountListResult result = service.listAccounts(new ListAccountsQuery("CONS-AIS-001", "TPP-001", "ix-1"));

        assertThat(result.cacheHit()).isFalse();
        assertThat(result.accounts()).hasSize(2);
        verify(cachePort).putAccounts(anyString(), any(), any());
    }

    @Test
    void shouldReturnAccountsFromCacheOnHit() {
        AisConsentContext consent = fullConsent();
        when(consentPort.findById("CONS-AIS-001")).thenReturn(Optional.of(consent));
        when(cachePort.getAccounts(anyString(), any())).thenReturn(Optional.of(List.of(account("ACC-001"))));

        AccountListResult result = service.listAccounts(new ListAccountsQuery("CONS-AIS-001", "TPP-001", "ix-1"));

        assertThat(result.cacheHit()).isTrue();
        assertThat(result.accounts()).hasSize(1);
        verify(accountReadPort, never()).findByPsuId(anyString());
    }

    @Test
    void shouldReturnBalancesAndCache() {
        AisConsentContext consent = fullConsent();
        when(consentPort.findById("CONS-AIS-001")).thenReturn(Optional.of(consent));
        when(cachePort.getBalances(anyString(), any())).thenReturn(Optional.empty());
        when(balanceReadPort.findByAccountId("ACC-001")).thenReturn(List.of(
                new BalanceSnapshot("ACC-001", "InterimAvailable", new BigDecimal("100.00"), "AED", Instant.parse("2026-02-09T09:00:00Z"))
        ));

        BalanceListResult result = service.getBalances(new GetBalancesQuery("CONS-AIS-001", "TPP-001", "ACC-001", "ix-2"));

        assertThat(result.cacheHit()).isFalse();
        assertThat(result.balances()).hasSize(1);
    }

    @Test
    void shouldRejectTransactionAccessWhenScopeMissing() {
        AisConsentContext balanceOnly = new AisConsentContext(
                "CONS-BAL-ONLY",
                "TPP-001",
                "PSU-001",
                Set.of("READBALANCES", "READACCOUNTS"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
        when(consentPort.findById("CONS-BAL-ONLY")).thenReturn(Optional.of(balanceOnly));

        assertThatThrownBy(() -> service.getTransactions(new GetTransactionsQuery(
                "CONS-BAL-ONLY",
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
    void shouldRejectBolaAccessWhenAccountNotGrantedToConsent() {
        AisConsentContext consent = fullConsent();
        when(consentPort.findById("CONS-AIS-001")).thenReturn(Optional.of(consent));

        assertThatThrownBy(() -> service.getAccount(new GetAccountQuery("CONS-AIS-001", "TPP-001", "ACC-999", "ix-4")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Resource not linked to consent");
    }

    @Test
    void shouldRejectWhenConsentIsMissingOrParticipantMismatch() {
        when(consentPort.findById("CONS-MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listAccounts(new ListAccountsQuery("CONS-MISSING", "TPP-001", "ix-missing")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Consent not found");

        AisConsentContext wrongParticipant = new AisConsentContext(
                "CONS-WRONG-TPP",
                "TPP-XYZ",
                "PSU-001",
                Set.of("READACCOUNTS"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
        when(consentPort.findById("CONS-WRONG-TPP")).thenReturn(Optional.of(wrongParticipant));

        assertThatThrownBy(() -> service.listAccounts(new ListAccountsQuery("CONS-WRONG-TPP", "TPP-001", "ix-wrong")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("participant mismatch");
    }

    @Test
    void shouldRejectWhenConsentExpired() {
        AisConsentContext expiredConsent = new AisConsentContext(
                "CONS-EXPIRED",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS"),
                Set.of("ACC-001"),
                Instant.parse("2020-01-01T00:00:00Z")
        );
        when(consentPort.findById("CONS-EXPIRED")).thenReturn(Optional.of(expiredConsent));

        assertThatThrownBy(() -> service.listAccounts(new ListAccountsQuery("CONS-EXPIRED", "TPP-001", "ix-exp")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldReturnCachedBalancesOnHit() {
        AisConsentContext consent = fullConsent();
        when(consentPort.findById("CONS-AIS-001")).thenReturn(Optional.of(consent));
        when(cachePort.getBalances(anyString(), any())).thenReturn(Optional.of(List.of(
                new BalanceSnapshot("ACC-001", "InterimBooked", new BigDecimal("99.00"), "AED", Instant.parse("2026-02-09T09:00:00Z"))
        )));

        BalanceListResult result = service.getBalances(new GetBalancesQuery("CONS-AIS-001", "TPP-001", "ACC-001", "ix-cache"));

        assertThat(result.cacheHit()).isTrue();
        verify(balanceReadPort, never()).findByAccountId(anyString());
    }

    @Test
    void shouldRejectWhenAccountMissingOrOwnedByDifferentPsu() {
        AisConsentContext consent = fullConsent();
        when(consentPort.findById("CONS-AIS-001")).thenReturn(Optional.of(consent));
        when(accountReadPort.findById("ACC-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccount(new GetAccountQuery("CONS-AIS-001", "TPP-001", "ACC-001", "ix-nf")))
                .isInstanceOf(com.enterprise.openfinance.uc02.domain.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");

        when(accountReadPort.findById("ACC-001")).thenReturn(Optional.of(new AccountSnapshot(
                "ACC-001",
                "PSU-999",
                "AE210001000000123456789",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Foreign"
        )));

        assertThatThrownBy(() -> service.getAccount(new GetAccountQuery("CONS-AIS-001", "TPP-001", "ACC-001", "ix-own")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Resource not linked to consent");
    }

    @Test
    void shouldFilterAndPaginateTransactions() {
        AisConsentContext consent = fullConsent();
        when(consentPort.findById("CONS-AIS-001")).thenReturn(Optional.of(consent));
        when(transactionReadPort.findByAccountId("ACC-001")).thenReturn(List.of(
                transaction("TXN-1", "2026-01-10T00:00:00Z"),
                transaction("TXN-2", "2026-01-05T00:00:00Z"),
                transaction("TXN-3", "2025-12-30T00:00:00Z")
        ));

        PagedResult<TransactionSnapshot> result = service.getTransactions(new GetTransactionsQuery(
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001",
                "ix-5",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-31T00:00:00Z"),
                1,
                1
        ));

        assertThat(result.items()).extracting(TransactionSnapshot::transactionId).containsExactly("TXN-1");
        assertThat(result.totalRecords()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void shouldReturnEmptyTransactionsWhenPageIsOutOfRange() {
        AisConsentContext consent = fullConsent();
        when(consentPort.findById("CONS-AIS-001")).thenReturn(Optional.of(consent));
        when(transactionReadPort.findByAccountId("ACC-001")).thenReturn(List.of(
                transaction("TXN-1", "2026-01-10T00:00:00Z")
        ));

        PagedResult<TransactionSnapshot> result = service.getTransactions(new GetTransactionsQuery(
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001",
                "ix-6",
                null,
                null,
                5,
                100
        ));

        assertThat(result.items()).isEmpty();
    }

    private static AisConsentContext fullConsent() {
        return new AisConsentContext(
                "CONS-AIS-001",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS", "READBALANCES", "READTRANSACTIONS"),
                Set.of("ACC-001", "ACC-002"),
                Instant.parse("2099-01-01T00:00:00Z")
        );
    }

    private static AccountSnapshot account(String accountId) {
        return new AccountSnapshot(
                accountId,
                "PSU-001",
                "AE210001000000123456789",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Primary"
        );
    }

    private static TransactionSnapshot transaction(String transactionId, String bookingDateTime) {
        return new TransactionSnapshot(
                transactionId,
                "ACC-001",
                new BigDecimal("10.00"),
                "AED",
                Instant.parse(bookingDateTime),
                Instant.parse(bookingDateTime),
                "Debit",
                "Booked",
                "Merchant"
        );
    }
}
