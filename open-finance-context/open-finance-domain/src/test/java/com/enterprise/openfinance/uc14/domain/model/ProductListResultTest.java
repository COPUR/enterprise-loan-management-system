package com.enterprise.openfinance.uc14.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductListResultTest {

    @Test
    void shouldToggleCacheHitWithoutMutatingData() {
        OpenProduct product = new OpenProduct(
                "PRD-001",
                "Current Account",
                "PCA",
                "RETAIL",
                "AED",
                new BigDecimal("5.00"),
                new BigDecimal("0.99"),
                Instant.parse("2026-02-10T00:00:00Z")
        );

        ProductListResult miss = new ProductListResult(List.of(product), false);
        ProductListResult hit = miss.withCacheHit(true);

        assertThat(miss.cacheHit()).isFalse();
        assertThat(hit.cacheHit()).isTrue();
        assertThat(hit.products()).containsExactly(product);
    }
}
