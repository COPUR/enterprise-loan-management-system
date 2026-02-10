package com.enterprise.openfinance.uc14.domain.port.out;

import com.enterprise.openfinance.uc14.domain.model.ProductListResult;

import java.time.Instant;
import java.util.Optional;

public interface ProductCachePort {

    Optional<ProductListResult> getProducts(String key, Instant now);

    void putProducts(String key, ProductListResult result, Instant expiresAt);
}
