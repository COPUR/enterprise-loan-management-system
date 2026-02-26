package com.enterprise.openfinance.bankingmetadata.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataListResultTest {

    @Test
    void shouldExposeItemsAndCacheFlag() {
        MetadataListResult<String> result = new MetadataListResult<>(List.of("a", "b"), false);

        assertThat(result.items()).containsExactly("a", "b");
        assertThat(result.cacheHit()).isFalse();
        assertThat(result.withCacheHit(true).cacheHit()).isTrue();
    }

    @Test
    void shouldRejectNullItems() {
        assertThatThrownBy(() -> new MetadataListResult<>(null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("items");
    }
}
