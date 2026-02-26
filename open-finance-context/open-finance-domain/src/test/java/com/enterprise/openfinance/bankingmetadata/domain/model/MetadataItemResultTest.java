package com.enterprise.openfinance.bankingmetadata.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataItemResultTest {

    @Test
    void shouldExposeItemAndCacheFlag() {
        MetadataItemResult<String> result = new MetadataItemResult<>("value", false);

        assertThat(result.item()).isEqualTo("value");
        assertThat(result.cacheHit()).isFalse();
        assertThat(result.withCacheHit(true).cacheHit()).isTrue();
    }

    @Test
    void shouldRejectNullItem() {
        assertThatThrownBy(() -> new MetadataItemResult<>(null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("item");
    }
}
