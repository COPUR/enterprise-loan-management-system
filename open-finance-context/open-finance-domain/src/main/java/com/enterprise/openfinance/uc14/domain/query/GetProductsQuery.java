package com.enterprise.openfinance.uc14.domain.query;

import java.util.Optional;
import java.util.regex.Pattern;

public record GetProductsQuery(
        String interactionId,
        String type,
        String segment
) {

    private static final Pattern FILTER_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,30}$");

    public GetProductsQuery {
        interactionId = requireNotBlank(interactionId, "interactionId");
        type = normalizeFilter(type, "type");
        segment = normalizeFilter(segment, "segment");
    }

    public Optional<String> normalizedType() {
        return Optional.ofNullable(type);
    }

    public Optional<String> normalizedSegment() {
        return Optional.ofNullable(segment);
    }

    public String cacheKeySuffix() {
        return "type=" + normalizedType().orElse("ALL") + "|segment=" + normalizedSegment().orElse("ALL");
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String normalizeFilter(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (!FILTER_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(field + " contains invalid characters");
        }
        return normalized;
    }
}
