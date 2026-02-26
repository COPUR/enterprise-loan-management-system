package com.enterprise.openfinance.productcatalog.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetProductsQueryTest {

    @Test
    void shouldNormalizeFiltersAndBuildCacheKey() {
        GetProductsQuery query = new GetProductsQuery("  ix-product-catalog-1 ", " pca ", " sme ");

        assertThat(query.interactionId()).isEqualTo("ix-product-catalog-1");
        assertThat(query.normalizedType()).contains("PCA");
        assertThat(query.normalizedSegment()).contains("SME");
        assertThat(query.cacheKeySuffix()).isEqualTo("type=PCA|segment=SME");
    }

    @Test
    void shouldHandleUnfilteredQuery() {
        GetProductsQuery query = new GetProductsQuery("ix-product-catalog-2", null, null);

        assertThat(query.normalizedType()).isEmpty();
        assertThat(query.normalizedSegment()).isEmpty();
        assertThat(query.cacheKeySuffix()).isEqualTo("type=ALL|segment=ALL");
    }

    @Test
    void shouldRejectInvalidInput() {
        assertThatThrownBy(() -> new GetProductsQuery(" ", "PCA", "SME"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");

        assertThatThrownBy(() -> new GetProductsQuery("ix-product-catalog", "PCA' OR '1'='1", "SME"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");

        assertThatThrownBy(() -> new GetProductsQuery("ix-product-catalog", "PCA", "SME;DROP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("segment");
    }
}
