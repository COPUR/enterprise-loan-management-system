package com.bank.loan.infrastructure.external;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerCreditServiceAdapterTest {

    private final CustomerCreditServiceAdapter adapter = new CustomerCreditServiceAdapter();

    @Test
    void hasAvailableCreditShouldReflectConfiguredCreditProfiles() {
        assertThat(adapter.hasAvailableCredit(
            CustomerId.of("CUST-12345678"),
            Money.usd(new BigDecimal("500.00"))
        )).isTrue();

        assertThat(adapter.hasAvailableCredit(
            CustomerId.of("CUST-11111111"),
            Money.usd(new BigDecimal("6000.00"))
        )).isFalse();

        assertThat(adapter.hasAvailableCredit(
            CustomerId.of("CUST-UNKNOWN"),
            Money.usd(new BigDecimal("1.00"))
        )).isFalse();
    }

    @Test
    void reserveAndReleaseCreditShouldUpdateAvailability() {
        CustomerId customerId = CustomerId.of("CUST-12345678");
        Money reserveAmount = Money.usd(new BigDecimal("2000.00"));

        assertThat(adapter.reserveCredit(customerId, reserveAmount)).isTrue();
        assertThat(adapter.getAvailableCredit(customerId).getAmount()).isEqualByComparingTo("98000.00");

        assertThat(adapter.releaseCredit(customerId, reserveAmount)).isTrue();
        assertThat(adapter.getAvailableCredit(customerId).getAmount()).isEqualByComparingTo("100000.00");
    }

    @Test
    void reserveReleaseAndAvailableCreditShouldHandleUnknownAndBounds() {
        CustomerId unknown = CustomerId.of("CUST-UNKNOWN");
        Money amount = Money.usd(new BigDecimal("100.00"));

        assertThat(adapter.reserveCredit(unknown, amount)).isFalse();
        assertThat(adapter.releaseCredit(unknown, amount)).isFalse();
        assertThat(adapter.getAvailableCredit(unknown).getAmount()).isEqualByComparingTo("0.00");
        assertThat(adapter.getAvailableCredit(unknown).getCurrency().getCurrencyCode()).isEqualTo("USD");

        CustomerId bounded = CustomerId.of("CUST-87654321");
        assertThat(adapter.reserveCredit(bounded, Money.usd(new BigDecimal("60000.00")))).isFalse();
        assertThat(adapter.releaseCredit(bounded, Money.usd(new BigDecimal("999999.00")))).isTrue();
        assertThat(adapter.getAvailableCredit(bounded).getAmount()).isEqualByComparingTo("50000.00");
    }
}
