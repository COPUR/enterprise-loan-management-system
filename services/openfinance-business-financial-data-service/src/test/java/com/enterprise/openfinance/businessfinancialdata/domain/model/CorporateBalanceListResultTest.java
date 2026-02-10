package com.enterprise.openfinance.businessfinancialdata.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorporateBalanceListResultTest {

    @Test
    void shouldExposeBalancesCacheAndMaskFlags() {
        CorporateBalanceListResult result = new CorporateBalanceListResult(List.of(), false, true);

        assertThat(result.balances()).isEmpty();
        assertThat(result.cacheHit()).isFalse();
        assertThat(result.masked()).isTrue();
        assertThat(result.withCacheHit(true).cacheHit()).isTrue();
    }

    @Test
    void shouldRejectNullList() {
        assertThatThrownBy(() -> new CorporateBalanceListResult(null, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balances");
    }
}
