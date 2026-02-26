package com.bank.payment.infrastructure.external;

import com.bank.payment.domain.AccountId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountValidationServiceAdapterTest {

    private final AccountValidationServiceAdapter adapter = new AccountValidationServiceAdapter();

    @Test
    void accountValidationAndCapabilitiesShouldReflectMockData() {
        AccountId active = AccountId.of("ACC-11111111");
        AccountId noSend = AccountId.of("ACC-33333333");
        AccountId invalid = AccountId.of("ACC-INVALID");
        AccountId missing = AccountId.of("ACC-MISSING");

        assertThat(adapter.validateAccount(active)).isTrue();
        assertThat(adapter.validateAccount(invalid)).isFalse();
        assertThat(adapter.validateAccount(missing)).isFalse();

        assertThat(adapter.canSendPayments(active)).isTrue();
        assertThat(adapter.canReceivePayments(active)).isTrue();
        assertThat(adapter.canSendPayments(noSend)).isFalse();
        assertThat(adapter.canReceivePayments(noSend)).isTrue();
    }

    @Test
    void balanceChecksShouldHandleKnownAndUnknownAccounts() {
        AccountId active = AccountId.of("ACC-11111111");
        AccountId missing = AccountId.of("ACC-MISSING");

        assertThat(adapter.hasBalance(active, Money.usd(new BigDecimal("1000.00")))).isTrue();
        assertThat(adapter.hasBalance(active, Money.usd(new BigDecimal("999999.00")))).isFalse();
        assertThat(adapter.hasBalance(missing, Money.usd(new BigDecimal("1.00")))).isFalse();

        assertThat(adapter.getAccountBalance(active).getAmount()).isEqualByComparingTo("25000.00");
        assertThat(adapter.getAccountBalance(missing).getAmount()).isEqualByComparingTo("0.00");
        assertThat(adapter.getAccountBalance(missing).getCurrency().getCurrencyCode()).isEqualTo("USD");
    }
}
