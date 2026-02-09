package com.enterprise.openfinance.uc04.domain.model;

import java.util.List;

public record MetadataListResult<T>(
        List<T> items,
        boolean cacheHit
) {

    public MetadataListResult {
        if (items == null) {
            throw new IllegalArgumentException("items is required");
        }
        items = List.copyOf(items);
    }

    public MetadataListResult<T> withCacheHit(boolean value) {
        return new MetadataListResult<>(items, value);
    }
}
