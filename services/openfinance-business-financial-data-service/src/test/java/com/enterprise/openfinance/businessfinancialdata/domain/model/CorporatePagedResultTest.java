package com.enterprise.openfinance.businessfinancialdata.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorporatePagedResultTest {

    @Test
    void shouldExposePaginationAndNavigation() {
        CorporatePagedResult<String> page = new CorporatePagedResult<>(List.of("a", "b"), 1, 2, 5, true);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextPage()).contains(2);
        assertThat(page.cacheHit()).isTrue();
    }

    @Test
    void shouldRejectInvalidPageInput() {
        assertThatThrownBy(() -> new CorporatePagedResult<>(List.of(), 0, 100, 0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }
}
