package com.enterprise.openfinance.uc14.infrastructure.persistence;

import com.enterprise.openfinance.uc14.domain.model.OpenProduct;
import com.enterprise.openfinance.uc14.domain.port.out.ProductCatalogPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class InMemoryProductCatalogAdapter implements ProductCatalogPort {

    private static final List<OpenProduct> PRODUCTS = List.of(
            product("PRD-PCA-001", "Personal Current Basic", "PCA", "RETAIL", "AED", "5.00", "0.50"),
            product("PRD-PCA-002", "Personal Current Plus", "PCA", "RETAIL", "AED", "15.00", "0.75"),
            product("PRD-SME-001", "SME Working Capital", "SME_LOAN", "SME", "AED", "0.00", "6.50"),
            product("PRD-SME-002", "SME Current Account", "SME_ACCOUNT", "SME", "AED", "25.00", "0.00"),
            product("PRD-RET-001", "Retail Credit Card", "CREDIT_CARD", "RETAIL", "AED", "0.00", "18.99")
    );

    @Override
    public List<OpenProduct> findProducts(Optional<String> type, Optional<String> segment) {
        return PRODUCTS.stream()
                .filter(product -> product.matches(type, segment))
                .toList();
    }

    private static OpenProduct product(String productId,
                                       String name,
                                       String type,
                                       String segment,
                                       String currency,
                                       String monthlyFee,
                                       String annualRate) {
        return new OpenProduct(
                productId,
                name,
                type,
                segment,
                currency,
                new BigDecimal(monthlyFee),
                new BigDecimal(annualRate),
                Instant.parse("2026-02-10T00:00:00Z")
        );
    }
}
