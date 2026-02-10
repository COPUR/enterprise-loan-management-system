package com.enterprise.openfinance.uc14.infrastructure.rest.dto;

import com.enterprise.openfinance.uc14.domain.model.OpenProduct;
import com.enterprise.openfinance.uc14.domain.model.ProductListResult;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProductsResponse(
        @JsonProperty("Data") Data data,
        @JsonProperty("Links") Links links,
        @JsonProperty("Meta") Meta meta
) {

    public static ProductsResponse from(ProductListResult result, String self) {
        List<ProductData> products = result.products().stream()
                .map(ProductData::from)
                .toList();
        return new ProductsResponse(
                new Data(products),
                new Links(self),
                new Meta(products.size())
        );
    }

    public record Data(
            @JsonProperty("Product") List<ProductData> product
    ) {
    }

    public record Links(
            @JsonProperty("Self") String self
    ) {
    }

    public record Meta(
            @JsonProperty("TotalRecords") int totalRecords
    ) {
    }

    public record ProductData(
            @JsonProperty("ProductId") String productId,
            @JsonProperty("Name") String name,
            @JsonProperty("Type") String type,
            @JsonProperty("Segment") String segment,
            @JsonProperty("Currency") String currency,
            @JsonProperty("MonthlyFee") String monthlyFee,
            @JsonProperty("AnnualRate") String annualRate,
            @JsonProperty("UpdatedAt") String updatedAt
    ) {

        static ProductData from(OpenProduct product) {
            return new ProductData(
                    product.productId(),
                    product.name(),
                    product.type(),
                    product.segment(),
                    product.currency(),
                    product.monthlyFee().toPlainString(),
                    product.annualRate().toPlainString(),
                    product.updatedAt().toString()
            );
        }
    }
}
