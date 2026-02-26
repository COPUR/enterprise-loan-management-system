package com.enterprise.openfinance.payeeverification.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountStatusTest {

    @Test
    void shouldAllowPaymentsOnlyForActiveStatus() {
        assertThat(AccountStatus.ACTIVE.canReceivePayments()).isTrue();
        assertThat(AccountStatus.CLOSED.canReceivePayments()).isFalse();
        assertThat(AccountStatus.DECEASED.canReceivePayments()).isFalse();
        assertThat(AccountStatus.UNKNOWN.canReceivePayments()).isFalse();
    }
}
