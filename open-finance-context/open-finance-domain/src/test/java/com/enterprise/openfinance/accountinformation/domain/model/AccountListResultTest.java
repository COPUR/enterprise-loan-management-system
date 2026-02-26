package com.enterprise.openfinance.accountinformation.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountListResultTest {

    @Test
    void shouldCreateAccountListResult() {
        AccountListResult result = new AccountListResult(List.of(new AccountSnapshot(
                "ACC-001",
                "PSU-001",
                "AE210001000000123456789",
                "AED",
                "Current",
                "Enabled",
                "IBAN",
                "Primary"
        )), true);

        assertThat(result.accounts()).hasSize(1);
        assertThat(result.cacheHit()).isTrue();
    }

    @Test
    void shouldRejectNullAccounts() {
        assertThatThrownBy(() -> new AccountListResult(null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accounts");
    }
}
