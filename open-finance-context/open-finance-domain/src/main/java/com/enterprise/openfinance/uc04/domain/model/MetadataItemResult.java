package com.enterprise.openfinance.uc04.domain.model;

public record MetadataItemResult<T>(
        T item,
        boolean cacheHit
) {

    public MetadataItemResult {
        if (item == null) {
            throw new IllegalArgumentException("item is required");
        }
    }

    public MetadataItemResult<T> withCacheHit(boolean value) {
        return new MetadataItemResult<>(item, value);
    }
}
