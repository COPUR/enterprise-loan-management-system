package com.enterprise.openfinance.atmdata.domain.model;

import java.time.Instant;
import java.util.List;

public record AtmLocation(
        String atmId,
        String name,
        AtmStatus status,
        double latitude,
        double longitude,
        String address,
        String city,
        String country,
        String accessibility,
        List<String> services,
        Instant updatedAt
) {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public AtmLocation {
        atmId = requireNotBlank(atmId, "atmId");
        name = requireNotBlank(name, "name");
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        validateLatitude(latitude);
        validateLongitude(longitude);
        address = requireNotBlank(address, "address");
        city = requireNotBlank(city, "city");
        country = requireNotBlank(country, "country").toUpperCase();
        accessibility = requireNotBlank(accessibility, "accessibility");
        services = normalizeServices(services);
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt is required");
        }
    }

    public boolean isWithinRadiusKm(double centerLat, double centerLon, double radiusKm) {
        double distance = distanceKm(latitude, longitude, centerLat, centerLon);
        return distance <= radiusKm;
    }

    private static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(latRad1) * Math.cos(latRad2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
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

    private static List<String> normalizeServices(List<String> services) {
        if (services == null || services.isEmpty()) {
            throw new IllegalArgumentException("services is required");
        }
        List<String> normalized = services.stream()
                .map(value -> requireNotBlank(value, "service").trim())
                .toList();
        return List.copyOf(normalized);
    }

    private static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
