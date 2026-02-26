package com.enterprise.openfinance.productcatalog.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public record OpenProduct(
        String productId,
        String name,
        String type,
        String segment,
        String currency,
        BigDecimal monthlyFee,
        BigDecimal annualRate,
        Instant updatedAt
) {

    public OpenProduct {
        productId = requireNotBlank(productId, "productId");
        name = requireNotBlank(name, "name");
        type = normalizeCode(type, "type");
        segment = normalizeCode(segment, "segment");
        currency = normalizeCode(currency, "currency");
        if (monthlyFee == null || monthlyFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("monthlyFee must be >= 0");
        }
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("annualRate must be >= 0");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
    }

    public boolean matches(Optional<String> typeFilter, Optional<String> segmentFilter) {
        boolean typeMatches = typeFilter.map(value -> value.equals(type)).orElse(true);
        boolean segmentMatches = segmentFilter.map(value -> value.equals(segment)).orElse(true);
        return typeMatches && segmentMatches;
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizeCode(String value, String field) {
        String normalized = requireNotBlank(value, field);
        return normalized.toUpperCase();
    }
}
