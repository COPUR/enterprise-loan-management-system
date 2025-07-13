package com.bank.payment.infrastructure.fraud;

/**
 * Geolocation Data for Fraud Analysis
 */
public class GeolocationData {
    
    private final String ipAddress;
    private final String country;
    private final String city;
    private final double latitude;
    private final double longitude;
    
    private GeolocationData(Builder builder) {
        this.ipAddress = builder.ipAddress;
        this.country = builder.country;
        this.city = builder.city;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
    }
    
    // Getters
    public String getIpAddress() { return ipAddress; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String ipAddress;
        private String country;
        private String city;
        private double latitude;
        private double longitude;
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder country(String country) {
            this.country = country;
            return this;
        }
        
        public Builder city(String city) {
            this.city = city;
            return this;
        }
        
        public Builder latitude(double latitude) {
            this.latitude = latitude;
            return this;
        }
        
        public Builder longitude(double longitude) {
            this.longitude = longitude;
            return this;
        }
        
        public GeolocationData build() {
            return new GeolocationData(this);
        }
    }
}