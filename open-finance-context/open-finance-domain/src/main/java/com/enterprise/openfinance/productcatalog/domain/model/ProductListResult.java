package com.enterprise.openfinance.productcatalog.domain.model;

import java.util.List;

public record ProductListResult(
        List<OpenProduct> products,
        boolean cacheHit
) {

    public ProductListResult {
        if (products == null) {
            throw new IllegalArgumentException("products is required");
        }
        products = List.copyOf(products);
    }

    public ProductListResult withCacheHit(boolean cacheHitValue) {
        return new ProductListResult(products, cacheHitValue);
    }
}
