package com.enterprise.openfinance.uc02.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagedResultTest {

    @Test
    void shouldExposePaginationMetadata() {
        PagedResult<String> page = new PagedResult<>(List.of("a", "b"), 2, 2, 5);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextPage()).contains(3);
    }

    @Test
    void shouldHandleEmptyPagedResult() {
        PagedResult<String> page = new PagedResult<>(List.of(), 1, 100, 0);

        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.nextPage()).isEmpty();
    }

    @Test
    void shouldRejectInvalidPagingValues() {
        assertThatThrownBy(() -> new PagedResult<>(List.of(), 0, 100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");

        assertThatThrownBy(() -> new PagedResult<>(List.of(), 1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageSize");

        assertThatThrownBy(() -> new PagedResult<>(null, 1, 100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("items");

        assertThatThrownBy(() -> new PagedResult<>(List.of(), 1, 100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalRecords");
    }
}
