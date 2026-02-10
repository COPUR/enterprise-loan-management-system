package com.enterprise.openfinance.uc14.application;

import com.enterprise.openfinance.uc14.domain.model.OpenProduct;
import com.enterprise.openfinance.uc14.domain.model.ProductDataSettings;
import com.enterprise.openfinance.uc14.domain.model.ProductListResult;
import com.enterprise.openfinance.uc14.domain.port.out.ProductCachePort;
import com.enterprise.openfinance.uc14.domain.port.out.ProductCatalogPort;
import com.enterprise.openfinance.uc14.domain.query.GetProductsQuery;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductDataServiceTest {

    private final ProductCatalogPort catalogPort = mock(ProductCatalogPort.class);
    private final ProductCachePort cachePort = mock(ProductCachePort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-02-10T10:00:00Z"), ZoneOffset.UTC);

    private final ProductDataService service = new ProductDataService(
            catalogPort,
            cachePort,
            new ProductDataSettings(Duration.ofMinutes(5)),
            clock
    );

    @Test
    void shouldReturnCachedResultOnHit() {
        ProductListResult cached = new ProductListResult(List.of(product("PRD-001", "PCA", "RETAIL")), false);
        when(cachePort.getProducts(eq("products:type=PCA|segment=ALL"), any())).thenReturn(Optional.of(cached));

        ProductListResult result = service.listProducts(new GetProductsQuery("ix-1", "PCA", null));

        assertThat(result.cacheHit()).isTrue();
        assertThat(result.products()).hasSize(1);
        verify(catalogPort, never()).findProducts(any(), any());
    }

    @Test
    void shouldReadAndCacheOnMiss() {
        when(cachePort.getProducts(eq("products:type=ALL|segment=SME"), any())).thenReturn(Optional.empty());
        when(catalogPort.findProducts(Optional.empty(), Optional.of("SME"))).thenReturn(List.of(
                product("PRD-002", "SME_LOAN", "SME"),
                product("PRD-001", "SME_ACCOUNT", "SME")
        ));

        ProductListResult result = service.listProducts(new GetProductsQuery("ix-2", null, "SME"));

        assertThat(result.cacheHit()).isFalse();
        assertThat(result.products()).extracting(OpenProduct::productId)
                .containsExactly("PRD-001", "PRD-002");
        verify(cachePort).putProducts(eq("products:type=ALL|segment=SME"), eq(result), eq(Instant.parse("2026-02-10T10:05:00Z")));
    }

    @Test
    void shouldPassNormalizedFiltersToCatalog() {
        when(cachePort.getProducts(any(), any())).thenReturn(Optional.empty());
        when(catalogPort.findProducts(any(), any())).thenReturn(List.of());

        service.listProducts(new GetProductsQuery("ix-3", "pca", "retail"));

        verify(catalogPort).findProducts(Optional.of("PCA"), Optional.of("RETAIL"));
    }

    private static OpenProduct product(String productId, String type, String segment) {
        return new OpenProduct(
                productId,
                "Product " + productId,
                type,
                segment,
                "AED",
                new BigDecimal("0.00"),
                new BigDecimal("1.99"),
                Instant.parse("2026-02-10T00:00:00Z")
        );
    }
}
