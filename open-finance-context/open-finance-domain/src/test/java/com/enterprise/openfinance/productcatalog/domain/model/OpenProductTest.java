package com.enterprise.openfinance.productcatalog.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenProductTest {

    @Test
    void shouldMatchTypeAndSegmentFilters() {
        OpenProduct product = product("PRD-001", "PCA", "RETAIL");

        assertThat(product.matches(Optional.empty(), Optional.empty())).isTrue();
        assertThat(product.matches(Optional.of("PCA"), Optional.empty())).isTrue();
        assertThat(product.matches(Optional.empty(), Optional.of("RETAIL"))).isTrue();
        assertThat(product.matches(Optional.of("SME_LOAN"), Optional.of("RETAIL"))).isFalse();
    }

    @Test
    void shouldRejectInvalidFields() {
        assertThatThrownBy(() -> new OpenProduct("", "Current Account", "PCA", "RETAIL", "AED",
                new BigDecimal("0.00"), new BigDecimal("0.00"), Instant.parse("2026-02-10T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productId");

        assertThatThrownBy(() -> new OpenProduct("PRD-001", "", "PCA", "RETAIL", "AED",
                new BigDecimal("0.00"), new BigDecimal("0.00"), Instant.parse("2026-02-10T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");

        assertThatThrownBy(() -> new OpenProduct("PRD-001", "Current Account", "PCA", "RETAIL", "AED",
                new BigDecimal("-1.00"), new BigDecimal("0.00"), Instant.parse("2026-02-10T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monthlyFee");

        assertThatThrownBy(() -> new OpenProduct("PRD-001", "Current Account", "PCA", "RETAIL", "AED",
                new BigDecimal("0.00"), new BigDecimal("-0.01"), Instant.parse("2026-02-10T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("annualRate");
    }

    private static OpenProduct product(String productId, String type, String segment) {
        return new OpenProduct(
                productId,
                "Product " + productId,
                type,
                segment,
                "AED",
                new BigDecimal("5.00"),
                new BigDecimal("0.99"),
                Instant.parse("2026-02-10T00:00:00Z")
        );
    }
}
