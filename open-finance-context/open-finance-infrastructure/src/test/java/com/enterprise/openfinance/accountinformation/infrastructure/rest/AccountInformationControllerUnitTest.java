package com.enterprise.openfinance.accountinformation.infrastructure.rest;

import com.enterprise.openfinance.accountinformation.domain.model.AccountListResult;
import com.enterprise.openfinance.accountinformation.domain.model.AccountSnapshot;
import com.enterprise.openfinance.accountinformation.domain.model.BalanceListResult;
import com.enterprise.openfinance.accountinformation.domain.model.BalanceSnapshot;
import com.enterprise.openfinance.accountinformation.domain.model.PagedResult;
import com.enterprise.openfinance.accountinformation.domain.model.TransactionSnapshot;
import com.enterprise.openfinance.accountinformation.domain.port.in.AccountInformationUseCase;
import com.enterprise.openfinance.accountinformation.domain.query.GetTransactionsQuery;
import com.enterprise.openfinance.accountinformation.infrastructure.rest.dto.AccountsResponse;
import com.enterprise.openfinance.accountinformation.infrastructure.rest.dto.TransactionsResponse;
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
class AccountInformationControllerUnitTest {

    @Test
    void shouldReturnAccountsWithCacheHeader() {
        AccountInformationUseCase useCase = Mockito.mock(AccountInformationUseCase.class);
        AccountInformationController controller = new AccountInformationController(useCase);
        Mockito.when(useCase.listAccounts(Mockito.any())).thenReturn(new AccountListResult(
                List.of(account("ACC-001")),
                true
        ));

        ResponseEntity<AccountsResponse> response = controller.getAccounts(
                "DPoP token",
                "proof",
                "ix-account-information-1",
                "CONS-AIS-001",
                "TPP-001"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-OF-Cache")).isEqualTo("HIT");
        assertThat(response.getHeaders().getFirst("X-FAPI-Interaction-ID")).isEqualTo("ix-account-information-1");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().accounts()).hasSize(1);
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        AccountInformationUseCase useCase = Mockito.mock(AccountInformationUseCase.class);
        AccountInformationController controller = new AccountInformationController(useCase);

        assertThatThrownBy(() -> controller.getAccounts(
                "Basic token",
                "proof",
                "ix-account-information-1",
                "CONS-AIS-001",
                "TPP-001"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    @Test
    void shouldReturnAccountAndBalances() {
        AccountInformationUseCase useCase = Mockito.mock(AccountInformationUseCase.class);
        AccountInformationController controller = new AccountInformationController(useCase);
        Mockito.when(useCase.getAccount(Mockito.any())).thenReturn(account("ACC-001"));
        Mockito.when(useCase.getBalances(Mockito.any())).thenReturn(new BalanceListResult(List.of(
                new BalanceSnapshot("ACC-001", "InterimAvailable", new BigDecimal("100.00"), "AED", Instant.parse("2026-02-09T00:00:00Z"))
        ), false));

        assertThat(controller.getAccount(
                "DPoP token",
                "proof",
                "ix-account-information-2",
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001"
        ).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(controller.getBalances(
                "DPoP token",
                "proof",
                "ix-account-information-3",
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001"
        ).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnTransactionsAndGenerateEtag() {
        AccountInformationUseCase useCase = Mockito.mock(AccountInformationUseCase.class);
        AccountInformationController controller = new AccountInformationController(useCase);
        PagedResult<TransactionSnapshot> page = new PagedResult<>(
                List.of(new TransactionSnapshot(
                        "TXN-1",
                        "ACC-001",
                        new BigDecimal("10.00"),
                        "AED",
                        Instant.parse("2026-01-10T00:00:00Z"),
                        Instant.parse("2026-01-10T00:00:00Z"),
                        "Debit",
                        "Booked",
                        "Merchant"
                )),
                1,
                100,
                1
        );
        Mockito.when(useCase.getTransactions(Mockito.any())).thenReturn(page);

        ResponseEntity<TransactionsResponse> response = controller.getTransactions(
                "DPoP token",
                "proof",
                "ix-account-information-4",
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001",
                null,
                null,
                1,
                100,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isNotBlank();
        assertThat(response.getBody()).isNotNull();

        ArgumentCaptor<GetTransactionsQuery> queryCaptor = ArgumentCaptor.forClass(GetTransactionsQuery.class);
        Mockito.verify(useCase).getTransactions(queryCaptor.capture());
        assertThat(queryCaptor.getValue().accountId()).isEqualTo("ACC-001");
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
}
