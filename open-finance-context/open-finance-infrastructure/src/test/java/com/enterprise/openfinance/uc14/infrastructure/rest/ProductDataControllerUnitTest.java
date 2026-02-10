package com.enterprise.openfinance.uc14.infrastructure.rest;

import com.enterprise.openfinance.uc14.domain.model.OpenProduct;
import com.enterprise.openfinance.uc14.domain.model.ProductListResult;
import com.enterprise.openfinance.uc14.domain.port.in.ProductDataUseCase;
import com.enterprise.openfinance.uc14.infrastructure.rest.dto.ProductsResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class ProductDataControllerUnitTest {

    @Test
    void shouldListProductsWithCacheHeadersAndEtag() {
        ProductDataUseCase useCase = Mockito.mock(ProductDataUseCase.class);
        ProductDataController controller = new ProductDataController(useCase);

        Mockito.when(useCase.listProducts(Mockito.any()))
                .thenReturn(new ProductListResult(List.of(product("PRD-001", "PCA", "RETAIL")), false));

        ResponseEntity<ProductsResponse> response = controller.listProducts(
                "ix-uc14-1",
                null,
                "PCA",
                null,
                null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-OF-Cache")).isEqualTo("MISS");
        assertThat(response.getHeaders().getETag()).isNotBlank();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().product()).hasSize(1);
    }

    @Test
    void shouldReturnNotModifiedWhenEtagMatches() {
        ProductDataUseCase useCase = Mockito.mock(ProductDataUseCase.class);
        ProductDataController controller = new ProductDataController(useCase);

        Mockito.when(useCase.listProducts(Mockito.any()))
                .thenReturn(new ProductListResult(List.of(product("PRD-001", "PCA", "RETAIL")), false));

        ResponseEntity<ProductsResponse> first = controller.listProducts("ix-uc14-2", null, null, null, null);
        ResponseEntity<ProductsResponse> second = controller.listProducts(
                "ix-uc14-2",
                null,
                null,
                null,
                first.getHeaders().getETag()
        );

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
    }

    @Test
    void shouldRejectUnsupportedAuthorizationType() {
        ProductDataUseCase useCase = Mockito.mock(ProductDataUseCase.class);
        ProductDataController controller = new ProductDataController(useCase);

        assertThatThrownBy(() -> controller.listProducts("ix-uc14-3", "Basic invalid", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bearer or DPoP");
    }

    @Test
    void shouldRejectMaliciousFilterInput() {
        ProductDataUseCase useCase = Mockito.mock(ProductDataUseCase.class);
        ProductDataController controller = new ProductDataController(useCase);

        assertThatThrownBy(() -> controller.listProducts("ix-uc14-4", null, "PCA' OR '1'='1", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
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
