package com.enterprise.openfinance.corporatetreasury.infrastructure.rest;

import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateAccountSnapshot;
import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateBalanceSnapshot;
import com.enterprise.openfinance.corporatetreasury.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.corporatetreasury.domain.port.in.CorporateTreasuryUseCase;
import com.enterprise.openfinance.corporatetreasury.domain.query.GetCorporateTransactionsQuery;
import com.enterprise.openfinance.corporatetreasury.infrastructure.rest.dto.CorporateAccountsResponse;
import com.enterprise.openfinance.corporatetreasury.infrastructure.rest.dto.CorporateBalancesResponse;
import com.enterprise.openfinance.corporatetreasury.infrastructure.rest.dto.CorporateTransactionsResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class CorporateTreasuryControllerUnitTest {

    @Test
    void shouldReturnAccountsAndBalancesWithCacheHeaders() {
        CorporateTreasuryUseCase useCase = Mockito.mock(CorporateTreasuryUseCase.class);
        CorporateTreasuryController controller = new CorporateTreasuryController(useCase);

        Mockito.when(useCase.listAccounts(Mockito.any())).thenReturn(new CorporateAccountListResult(
                List.of(account("ACC-M-001", null, false)),
                true
        ));
        Mockito.when(useCase.getBalances(Mockito.any())).thenReturn(new CorporateBalanceListResult(
                List.of(balance("ACC-M-001", "InterimAvailable", "15000.00")),
                false,
                true
        ));

        ResponseEntity<CorporateAccountsResponse> accounts = controller.getAccounts(
                "DPoP token",
                "proof",
                "ix-corporate-treasury-1",
                "CONS-TRSY-001",
                "TPP-001",
                true,
                null
        );

        ResponseEntity<CorporateBalancesResponse> balances = controller.getBalances(
                "DPoP token",
                "proof",
                "ix-corporate-treasury-2",
                "CONS-TRSY-RESTRICTED",
                "TPP-001",
                "ACC-M-001"
        );

        assertThat(accounts.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accounts.getHeaders().getFirst("X-OF-Cache")).isEqualTo("HIT");
        assertThat(balances.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(balances.getHeaders().getFirst("X-OF-Entitlement")).isEqualTo("RESTRICTED");
    }

    @Test
    void shouldReturnTransactionsWithEtagAndSupportNotModified() {
        CorporateTreasuryUseCase useCase = Mockito.mock(CorporateTreasuryUseCase.class);
        CorporateTreasuryController controller = new CorporateTreasuryController(useCase);

        Mockito.when(useCase.getTransactions(Mockito.any())).thenReturn(new CorporatePagedResult<>(
                List.of(transaction("TX-001", "ACC-M-001", "2026-02-05T00:00:00Z", "SWEEP", "ZBA")),
                1,
                100,
                1,
                false
        ));

        ResponseEntity<CorporateTransactionsResponse> first = controller.getTransactions(
                "DPoP token",
                "proof",
                "ix-corporate-treasury-3",
                "CONS-TRSY-001",
                "TPP-001",
                "ACC-M-001",
                null,
                null,
                1,
                100,
                null
        );

        String etag = first.getHeaders().getETag();

        ResponseEntity<CorporateTransactionsResponse> second = controller.getTransactions(
                "DPoP token",
                "proof",
                "ix-corporate-treasury-3",
                "CONS-TRSY-001",
                "TPP-001",
                "ACC-M-001",
                null,
                null,
                1,
                100,
                etag
        );

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(etag).isNotBlank();
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);

        ArgumentCaptor<GetCorporateTransactionsQuery> queryCaptor = ArgumentCaptor.forClass(GetCorporateTransactionsQuery.class);
        Mockito.verify(useCase).getTransactions(queryCaptor.capture());
        assertThat(queryCaptor.getValue().accountId()).isEqualTo("ACC-M-001");
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        CorporateTreasuryUseCase useCase = Mockito.mock(CorporateTreasuryUseCase.class);
        CorporateTreasuryController controller = new CorporateTreasuryController(useCase);

        assertThatThrownBy(() -> controller.getAccounts(
                "Basic token",
                "proof",
                "ix-corporate-treasury-4",
                "CONS-TRSY-001",
                "TPP-001",
                true,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
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

    private static CorporateTransactionSnapshot transaction(String id,
                                                            String accountId,
                                                            String bookingDate,
                                                            String transactionCode,
                                                            String proprietaryCode) {
        return new CorporateTransactionSnapshot(
                id,
                accountId,
                new BigDecimal("100.00"),
                "AED",
                Instant.parse(bookingDate),
                transactionCode,
                proprietaryCode,
                "desc"
        );
    }
}
