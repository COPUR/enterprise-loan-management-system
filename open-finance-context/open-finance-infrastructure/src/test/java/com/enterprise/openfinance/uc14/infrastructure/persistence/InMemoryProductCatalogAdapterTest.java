package com.enterprise.openfinance.uc14.infrastructure.persistence;

import com.enterprise.openfinance.uc14.domain.model.OpenProduct;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryProductCatalogAdapterTest {

    @Test
    void shouldFilterByTypeAndSegment() {
        InMemoryProductCatalogAdapter adapter = new InMemoryProductCatalogAdapter();

        List<OpenProduct> pca = adapter.findProducts(Optional.of("PCA"), Optional.empty());
        List<OpenProduct> sme = adapter.findProducts(Optional.empty(), Optional.of("SME"));

        assertThat(pca).isNotEmpty();
        assertThat(pca).allSatisfy(product -> assertThat(product.type()).isEqualTo("PCA"));

        assertThat(sme).isNotEmpty();
        assertThat(sme).allSatisfy(product -> assertThat(product.segment()).isEqualTo("SME"));
    }
}
