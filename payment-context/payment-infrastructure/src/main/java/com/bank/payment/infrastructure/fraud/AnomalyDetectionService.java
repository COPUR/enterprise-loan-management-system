package com.bank.payment.infrastructure.fraud;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anomaly Detection Service for Fraud Detection
 * 
 * Implements unsupervised anomaly detection algorithms:
 * - Statistical outlier detection
 * - Time series anomaly detection
 * - Isolation forest simulation
 * - Local outlier factor analysis
 */
@Service
public class AnomalyDetectionService {
    
    // Historical feature distributions for anomaly detection
    private final Map<String, FeatureDistribution> featureDistributions = new ConcurrentHashMap<>();
    
    /**
     * Calculate anomaly score for given features
     */
    public double calculateAnomalyScore(MLFeatures features) {
        double[] scores = new double[]{
            calculateStatisticalAnomalyScore(features),
            calculateTimeSeriesAnomalyScore(features),
            calculateIsolationForestScore(features),
            calculateLocalOutlierScore(features)
        };
        
        // Return maximum anomaly score
        return Arrays.stream(scores).max().orElse(0.0);
    }
    
    /**
     * Statistical outlier detection using z-score
     */
    private double calculateStatisticalAnomalyScore(MLFeatures features) {
        double maxZScore = 0.0;
        
        // Check amount anomaly
        double amountZScore = calculateZScore("amount", features.getAmount().doubleValue());
        maxZScore = Math.max(maxZScore, amountZScore);
        
        // Check velocity anomaly
        double velocityZScore = calculateZScore("velocity", features.getVelocityScore());
        maxZScore = Math.max(maxZScore, velocityZScore);
        
        // Convert z-score to anomaly probability
        return Math.min(1.0, maxZScore / 3.0);
    }
    
    /**
     * Time series anomaly detection
     */
    private double calculateTimeSeriesAnomalyScore(MLFeatures features) {
        // Simulate time series anomaly detection
        int hour = features.getHourOfDay();
        int day = features.getDayOfWeek();
        
        double score = 0.0;
        
        // Unusual time patterns
        if (hour < 6 || hour > 22) {
            score += 0.3;
        }
        
        // Weekend patterns
        if (day == 6 || day == 7) {
            score += 0.2;
        }
        
        return Math.min(1.0, score);
    }
    
    /**
     * Isolation forest anomaly score
     */
    private double calculateIsolationForestScore(MLFeatures features) {
        // Simulate isolation forest path length calculation
        double[] featureVector = features.toVector();
        double pathLength = 0.0;
        
        for (double feature : featureVector) {
            // Simulate isolation path
            pathLength += Math.log(Math.abs(feature) + 1);
        }
        
        // Normalize path length to [0,1]
        double normalizedPath = pathLength / (featureVector.length * 5.0);
        
        // Shorter paths indicate anomalies
        return Math.max(0.0, 1.0 - normalizedPath);
    }
    
    /**
     * Local outlier factor calculation
     */
    private double calculateLocalOutlierScore(MLFeatures features) {
        // Simplified LOF calculation
        double[] featureVector = features.toVector();
        
        // Calculate distance to "neighbors" (simulated)
        double avgDistance = 0.0;
        for (int i = 0; i < 5; i++) { // Simulate 5 nearest neighbors
            double distance = calculateEuclideanDistance(featureVector, generateRandomNeighbor());
            avgDistance += distance;
        }
        avgDistance /= 5.0;
        
        // Higher average distance indicates outlier
        return Math.min(1.0, avgDistance / 2.0);
    }
    
    /**
     * Calculate z-score for feature
     */
    private double calculateZScore(String featureName, double value) {
        FeatureDistribution dist = featureDistributions.computeIfAbsent(
            featureName, k -> new FeatureDistribution()
        );
        
        dist.addValue(value);
        
        double mean = dist.getMean();
        double stdDev = dist.getStandardDeviation();
        
        return stdDev > 0 ? Math.abs((value - mean) / stdDev) : 0.0;
    }
    
    /**
     * Calculate Euclidean distance between feature vectors
     */
    private double calculateEuclideanDistance(double[] vector1, double[] vector2) {
        double sum = 0.0;
        for (int i = 0; i < Math.min(vector1.length, vector2.length); i++) {
            sum += Math.pow(vector1[i] - vector2[i], 2);
        }
        return Math.sqrt(sum);
    }
    
    /**
     * Generate random neighbor for LOF calculation
     */
    private double[] generateRandomNeighbor() {
        Random random = new Random();
        return new double[] {
            random.nextGaussian() * 0.3 + 0.5,
            random.nextGaussian() * 0.2 + 0.3,
            random.nextGaussian() * 0.1 + 0.4,
            random.nextGaussian() * 0.15 + 0.2,
            random.nextGaussian() * 0.1 + 0.1
        };
    }
    
    /**
     * Feature distribution tracker
     */
    private static class FeatureDistribution {
        private final List<Double> values = new ArrayList<>();
        private double sum = 0.0;
        private double sumSquares = 0.0;
        private int count = 0;
        
        public void addValue(double value) {
            values.add(value);
            sum += value;
            sumSquares += value * value;
            count++;
            
            // Keep only recent values
            if (values.size() > 1000) {
                double removed = values.remove(0);
                sum -= removed;
                sumSquares -= removed * removed;
                count--;
            }
        }
        
        public double getMean() {
            return count > 0 ? sum / count : 0.0;
        }
        
        public double getStandardDeviation() {
            if (count < 2) return 0.0;
            
            double mean = getMean();
            double variance = (sumSquares / count) - (mean * mean);
            return Math.sqrt(Math.max(0, variance));
        }
    }
}