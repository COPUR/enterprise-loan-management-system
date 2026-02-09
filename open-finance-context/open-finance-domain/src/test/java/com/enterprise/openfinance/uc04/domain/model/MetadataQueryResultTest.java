package com.enterprise.openfinance.uc04.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataQueryResultTest {

    @Test
    void shouldExposePaginationAndNavigation() {
        MetadataQueryResult<String> result = new MetadataQueryResult<>(List.of("a", "b"), 1, 2, 5, true);

        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextPage()).contains(2);
        assertThat(result.cacheHit()).isTrue();
    }

    @Test
    void shouldRejectInvalidPagingValues() {
        assertThatThrownBy(() -> new MetadataQueryResult<>(List.of(), 0, 100, 0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }
}
