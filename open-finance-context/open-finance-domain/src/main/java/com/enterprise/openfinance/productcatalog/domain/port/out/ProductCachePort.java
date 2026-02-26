package com.enterprise.openfinance.productcatalog.domain.port.out;

import com.enterprise.openfinance.productcatalog.domain.model.ProductListResult;

import java.time.Instant;
import java.util.Optional;

public interface ProductCachePort {

    Optional<ProductListResult> getProducts(String key, Instant now);

    void putProducts(String key, ProductListResult result, Instant expiresAt);
}
