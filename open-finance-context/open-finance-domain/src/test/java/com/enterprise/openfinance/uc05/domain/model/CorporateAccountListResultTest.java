package com.enterprise.openfinance.uc05.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorporateAccountListResultTest {

    @Test
    void shouldExposeAccountsAndCacheFlag() {
        CorporateAccountListResult result = new CorporateAccountListResult(List.of(), false);

        assertThat(result.accounts()).isEmpty();
        assertThat(result.cacheHit()).isFalse();
        assertThat(result.withCacheHit(true).cacheHit()).isTrue();
    }

    @Test
    void shouldRejectNullList() {
        assertThatThrownBy(() -> new CorporateAccountListResult(null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accounts");
    }
}
