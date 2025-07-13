package com.bank.payment.infrastructure.fraud;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ML Features for Fraud Detection
 * 
 * Represents feature vector for machine learning models used in fraud detection.
 * Features are carefully selected based on fraud detection literature and domain expertise.
 */
public class MLFeatures {
    
    private final String customerId;
    private final BigDecimal amount;
    private final String paymentType;
    private final LocalDateTime timestamp;
    private final int dayOfWeek;
    private final int hourOfDay;
    private final String fromAccountType;
    private final String toAccountType;
    private final String description;
    
    // Advanced features
    private final double velocityScore;
    private final double behavioralScore;
    private final double geospatialScore;
    private final double networkScore;
    private final double deviceScore;
    
    private MLFeatures(Builder builder) {
        this.customerId = builder.customerId;
        this.amount = builder.amount;
        this.paymentType = builder.paymentType;
        this.timestamp = builder.timestamp;
        this.dayOfWeek = builder.dayOfWeek;
        this.hourOfDay = builder.hourOfDay;
        this.fromAccountType = builder.fromAccountType;
        this.toAccountType = builder.toAccountType;
        this.description = builder.description;
        this.velocityScore = builder.velocityScore;
        this.behavioralScore = builder.behavioralScore;
        this.geospatialScore = builder.geospatialScore;
        this.networkScore = builder.networkScore;
        this.deviceScore = builder.deviceScore;
    }
    
    // Getters
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentType() { return paymentType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getDayOfWeek() { return dayOfWeek; }
    public int getHourOfDay() { return hourOfDay; }
    public String getFromAccountType() { return fromAccountType; }
    public String getToAccountType() { return toAccountType; }
    public String getDescription() { return description; }
    public double getVelocityScore() { return velocityScore; }
    public double getBehavioralScore() { return behavioralScore; }
    public double getGeospatialScore() { return geospatialScore; }
    public double getNetworkScore() { return networkScore; }
    public double getDeviceScore() { return deviceScore; }
    
    /**
     * Convert to feature vector for ML models
     */
    public double[] toVector() {
        return new double[] {
            normalizeAmount(amount),
            encodePaymentType(paymentType),
            normalizeTime(hourOfDay),
            normalizeDayOfWeek(dayOfWeek),
            encodeAccountType(fromAccountType),
            encodeAccountType(toAccountType),
            velocityScore,
            behavioralScore,
            geospatialScore,
            networkScore,
            deviceScore
        };
    }
    
    /**
     * Get feature names for interpretability
     */
    public String[] getFeatureNames() {
        return new String[] {
            "amount_normalized",
            "payment_type_encoded",
            "hour_normalized",
            "day_of_week_normalized", 
            "from_account_type_encoded",
            "to_account_type_encoded",
            "velocity_score",
            "behavioral_score",
            "geospatial_score",
            "network_score",
            "device_score"
        };
    }
    
    private double normalizeAmount(BigDecimal amount) {
        // Log transformation for amount normalization
        double logAmount = Math.log(amount.doubleValue() + 1);
        return Math.min(1.0, logAmount / 15.0);
    }
    
    private double encodePaymentType(String paymentType) {
        switch (paymentType) {
            case "BANK_TRANSFER": return 0.2;
            case "ACH_TRANSFER": return 0.4;
            case "WIRE_TRANSFER": return 0.6;
            case "INTERNATIONAL_TRANSFER": return 0.8;
            default: return 0.0;
        }
    }
    
    private double normalizeTime(int hour) {
        return hour / 24.0;
    }
    
    private double normalizeDayOfWeek(int dayOfWeek) {
        return (dayOfWeek - 1) / 6.0;
    }
    
    private double encodeAccountType(String accountType) {
        switch (accountType) {
            case "CHECKING": return 0.2;
            case "SAVINGS": return 0.4;
            case "BUSINESS": return 0.6;
            case "INVESTMENT": return 0.8;
            default: return 0.0;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String customerId;
        private BigDecimal amount;
        private String paymentType;
        private LocalDateTime timestamp;
        private int dayOfWeek;
        private int hourOfDay;
        private String fromAccountType;
        private String toAccountType;
        private String description;
        private double velocityScore = 0.0;
        private double behavioralScore = 0.0;
        private double geospatialScore = 0.0;
        private double networkScore = 0.0;
        private double deviceScore = 0.0;
        
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder paymentType(String paymentType) {
            this.paymentType = paymentType;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder dayOfWeek(int dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            return this;
        }
        
        public Builder hourOfDay(int hourOfDay) {
            this.hourOfDay = hourOfDay;
            return this;
        }
        
        public Builder fromAccountType(String fromAccountType) {
            this.fromAccountType = fromAccountType;
            return this;
        }
        
        public Builder toAccountType(String toAccountType) {
            this.toAccountType = toAccountType;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder velocityScore(double velocityScore) {
            this.velocityScore = velocityScore;
            return this;
        }
        
        public Builder behavioralScore(double behavioralScore) {
            this.behavioralScore = behavioralScore;
            return this;
        }
        
        public Builder geospatialScore(double geospatialScore) {
            this.geospatialScore = geospatialScore;
            return this;
        }
        
        public Builder networkScore(double networkScore) {
            this.networkScore = networkScore;
            return this;
        }
        
        public Builder deviceScore(double deviceScore) {
            this.deviceScore = deviceScore;
            return this;
        }
        
        public MLFeatures build() {
            return new MLFeatures(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "MLFeatures{customerId='%s', amount=%s, type='%s', hour=%d, day=%d, velocity=%.2f, behavioral=%.2f}",
            customerId, amount, paymentType, hourOfDay, dayOfWeek, velocityScore, behavioralScore
        );
    }
}