package com.enterprise.openfinance.uc15.domain.query;

import java.util.Locale;
import java.util.Optional;

public record GetAtmsQuery(
        String interactionId,
        Double latitude,
        Double longitude,
        Double radiusKm
) {

    private static final double DEFAULT_RADIUS_KM = 5.0;
    private static final double MAX_RADIUS_KM = 50.0;

    public GetAtmsQuery {
        interactionId = requireNotBlank(interactionId, "interactionId");
        boolean hasLat = latitude != null;
        boolean hasLon = longitude != null;
        if (hasLat ^ hasLon) {
            throw new IllegalArgumentException("latitude and longitude must be provided together");
        }
        if (hasLat) {
            validateLatitude(latitude);
            validateLongitude(longitude);
            radiusKm = normalizeRadius(radiusKm);
        } else {
            radiusKm = null;
        }
    }

    public Optional<Double> normalizedLatitude() {
        return Optional.ofNullable(latitude);
    }

    public Optional<Double> normalizedLongitude() {
        return Optional.ofNullable(longitude);
    }

    public Optional<Double> normalizedRadiusKm() {
        return Optional.ofNullable(radiusKm);
    }

    public boolean hasLocationFilter() {
        return latitude != null && longitude != null;
    }

    public String cacheKeySuffix() {
        if (!hasLocationFilter()) {
            return "ALL";
        }
        return "lat=" + format(latitude) + "|lon=" + format(longitude) + "|radius=" + format(radiusKm);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static double normalizeRadius(Double radius) {
        double resolved = radius == null ? DEFAULT_RADIUS_KM : radius;
        if (resolved <= 0 || resolved > MAX_RADIUS_KM) {
            throw new IllegalArgumentException("radius must be between 0 and " + MAX_RADIUS_KM + " km");
        }
        return resolved;
    }

    private static void validateLatitude(double latitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
    }

    private static void validateLongitude(double longitude) {
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
