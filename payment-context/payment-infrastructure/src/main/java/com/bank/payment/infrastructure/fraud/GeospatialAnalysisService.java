package com.bank.payment.infrastructure.fraud;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geospatial Analysis Service for Fraud Detection
 * 
 * Analyzes geographic patterns for fraud detection:
 * - Location velocity analysis
 * - Impossible travel detection
 * - Geographic clustering
 * - Country/region risk scoring
 */
@Service
public class GeospatialAnalysisService {
    
    // Customer location history
    private final Map<String, List<LocationEvent>> customerLocations = new ConcurrentHashMap<>();
    
    // Country risk scores (simplified)
    private final Map<String, Integer> countryRiskScores = Map.of(
        "US", 5,
        "CA", 5,
        "GB", 10,
        "DE", 10,
        "FR", 10,
        "RU", 50,
        "CN", 30,
        "NG", 60,
        "PK", 55
    );
    
    /**
     * Analyze geospatial risk for transaction
     */
    public int analyzeGeospatialRisk(String customerId, GeolocationData currentLocation) {
        int riskScore = 0;
        
        // 1. Country risk assessment
        riskScore += assessCountryRisk(currentLocation);
        
        // 2. Location velocity analysis
        riskScore += analyzeLocationVelocity(customerId, currentLocation);
        
        // 3. Geographic clustering analysis
        riskScore += analyzeGeographicClustering(customerId, currentLocation);
        
        // 4. Impossible travel detection
        riskScore += detectImpossibleTravel(customerId, currentLocation);
        
        // Update customer location history
        updateLocationHistory(customerId, currentLocation);
        
        return Math.min(100, riskScore);
    }
    
    /**
     * Assess country-based risk
     */
    private int assessCountryRisk(GeolocationData location) {
        return countryRiskScores.getOrDefault(location.getCountry(), 25);
    }
    
    /**
     * Analyze location velocity patterns
     */
    private int analyzeLocationVelocity(String customerId, GeolocationData currentLocation) {
        List<LocationEvent> history = customerLocations.getOrDefault(customerId, new ArrayList<>());
        
        if (history.isEmpty()) {
            return 0; // No history to compare
        }
        
        // Check recent location changes
        long recentChanges = history.stream()
            .filter(event -> event.isRecent(24)) // Last 24 hours
            .map(LocationEvent::getLocation)
            .distinct()
            .count();
        
        if (recentChanges > 5) {
            return 20; // Too many location changes
        } else if (recentChanges > 3) {
            return 10;
        }
        
        return 0;
    }
    
    /**
     * Analyze geographic clustering
     */
    private int analyzeGeographicClustering(String customerId, GeolocationData currentLocation) {
        List<LocationEvent> history = customerLocations.getOrDefault(customerId, new ArrayList<>());
        
        if (history.size() < 3) {
            return 0; // Insufficient history
        }
        
        // Calculate distances from current location to historical locations
        double avgDistance = history.stream()
            .filter(event -> event.isRecent(720)) // Last 30 days
            .mapToDouble(event -> calculateDistance(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                event.getLocation().getLatitude(), event.getLocation().getLongitude()
            ))
            .average()
            .orElse(0.0);
        
        // If current location is very far from typical locations
        if (avgDistance > 5000) { // More than 5000 km
            return 25;
        } else if (avgDistance > 2000) { // More than 2000 km
            return 15;
        } else if (avgDistance > 500) { // More than 500 km
            return 10;
        }
        
        return 0;
    }
    
    /**
     * Detect impossible travel scenarios
     */
    private int detectImpossibleTravel(String customerId, GeolocationData currentLocation) {
        List<LocationEvent> history = customerLocations.getOrDefault(customerId, new ArrayList<>());
        
        if (history.isEmpty()) {
            return 0;
        }
        
        // Get most recent location
        LocationEvent lastEvent = history.get(history.size() - 1);
        
        // Calculate distance and time difference
        double distance = calculateDistance(
            currentLocation.getLatitude(), currentLocation.getLongitude(),
            lastEvent.getLocation().getLatitude(), lastEvent.getLocation().getLongitude()
        );
        
        long minutesDiff = java.time.Duration.between(
            lastEvent.getTimestamp(), 
            java.time.LocalDateTime.now()
        ).toMinutes();
        
        if (minutesDiff > 0) {
            // Calculate required speed (km/h)
            double requiredSpeed = (distance / minutesDiff) * 60;
            
            // Commercial flight speed is ~900 km/h
            if (requiredSpeed > 1200) {
                return 40; // Impossible travel
            } else if (requiredSpeed > 900) {
                return 20; // Very fast travel (possible but suspicious)
            } else if (requiredSpeed > 300) {
                return 10; // Fast travel (train/car)
            }
        }
        
        return 0;
    }
    
    /**
     * Update customer location history
     */
    private void updateLocationHistory(String customerId, GeolocationData location) {
        List<LocationEvent> history = customerLocations.computeIfAbsent(
            customerId, k -> new ArrayList<>()
        );
        
        history.add(new LocationEvent(location, java.time.LocalDateTime.now()));
        
        // Keep only recent history (last 100 locations)
        if (history.size() > 100) {
            history.remove(0);
        }
    }
    
    /**
     * Calculate distance between two geographic points using Haversine formula
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Earth's radius in kilometers
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * Location event for tracking customer geography
     */
    private static class LocationEvent {
        private final GeolocationData location;
        private final java.time.LocalDateTime timestamp;
        
        public LocationEvent(GeolocationData location, java.time.LocalDateTime timestamp) {
            this.location = location;
            this.timestamp = timestamp;
        }
        
        public GeolocationData getLocation() { return location; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        
        public boolean isRecent(int hours) {
            return timestamp.isAfter(java.time.LocalDateTime.now().minusHours(hours));
        }
    }
}