package com.enterprise.openfinance.uc14.infrastructure.cache;

import com.enterprise.openfinance.uc14.domain.model.OpenProduct;
import com.enterprise.openfinance.uc14.domain.model.ProductListResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryProductCacheAdapterTest {

    @Test
    void shouldReturnCacheEntryBeforeExpiryAndEvictAfterExpiry() {
        InMemoryProductCacheAdapter adapter = new InMemoryProductCacheAdapter();
        ProductListResult value = new ProductListResult(List.of(product("PRD-001")), false);

        adapter.putProducts("key-1", value, Instant.parse("2026-02-10T10:05:00Z"));

        assertThat(adapter.getProducts("key-1", Instant.parse("2026-02-10T10:00:00Z"))).isPresent();
        assertThat(adapter.getProducts("key-1", Instant.parse("2026-02-10T10:06:00Z"))).isEmpty();
    }

    private static OpenProduct product(String id) {
        return new OpenProduct(
                id,
                "Product " + id,
                "PCA",
                "RETAIL",
                "AED",
                new BigDecimal("0.00"),
                new BigDecimal("1.99"),
                Instant.parse("2026-02-10T00:00:00Z")
        );
    }
}
