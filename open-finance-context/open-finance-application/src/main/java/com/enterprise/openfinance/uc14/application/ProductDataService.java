package com.enterprise.openfinance.uc14.application;

import com.enterprise.openfinance.uc14.domain.model.OpenProduct;
import com.enterprise.openfinance.uc14.domain.model.ProductDataSettings;
import com.enterprise.openfinance.uc14.domain.model.ProductListResult;
import com.enterprise.openfinance.uc14.domain.port.in.ProductDataUseCase;
import com.enterprise.openfinance.uc14.domain.port.out.ProductCachePort;
import com.enterprise.openfinance.uc14.domain.port.out.ProductCatalogPort;
import com.enterprise.openfinance.uc14.domain.query.GetProductsQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class ProductDataService implements ProductDataUseCase {

    private final ProductCatalogPort catalogPort;
    private final ProductCachePort cachePort;
    private final ProductDataSettings settings;
    private final Clock clock;

    public ProductDataService(ProductCatalogPort catalogPort,
                              ProductCachePort cachePort,
                              ProductDataSettings settings,
                              Clock clock) {
        this.catalogPort = catalogPort;
        this.cachePort = cachePort;
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    public ProductListResult listProducts(GetProductsQuery query) {
        Instant now = Instant.now(clock);
        String cacheKey = "products:" + query.cacheKeySuffix();

        var cached = cachePort.getProducts(cacheKey, now);
        if (cached.isPresent()) {
            return cached.orElseThrow().withCacheHit(true);
        }

        var products = catalogPort.findProducts(query.normalizedType(), query.normalizedSegment())
                .stream()
                .sorted(Comparator.comparing(OpenProduct::productId))
                .toList();

        ProductListResult result = new ProductListResult(products, false);
        cachePort.putProducts(cacheKey, result, now.plus(settings.cacheTtl()));
        return result;
    }
}
