package com.bank.ml.anomaly;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bank.ml.anomaly.model.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Transaction Pattern Analyzer
 * Detects unusual patterns in customer transaction behavior
 */
@Service
public class TransactionPatternAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(TransactionPatternAnalyzer.class);
    
    // Pattern detection thresholds
    private static final double FREQUENCY_THRESHOLD = 0.7;
    private static final double AMOUNT_VARIANCE_THRESHOLD = 0.8;
    private static final double VELOCITY_THRESHOLD = 0.75;
    private static final int MIN_TRANSACTIONS_FOR_PATTERN = 3;
    
    /**
     * Analyze transaction patterns for anomalies
     */
    public TransactionPatternAnalysis analyzePatterns(String customerId, List<TransactionData> transactions) {
        logger.debug("Analyzing transaction patterns for customer {}", customerId);
        
        try {
            if (transactions.size() < MIN_TRANSACTIONS_FOR_PATTERN) {
                return createNormalPattern(customerId, "INSUFFICIENT_DATA");
            }
            
            // Sort transactions by timestamp
            List<TransactionData> sortedTransactions = transactions.stream()
                .sorted(Comparator.comparing(TransactionData::getTimestamp))
                .collect(Collectors.toList());
            
            // Analyze different pattern types
            double frequencyScore = analyzeFrequencyPattern(sortedTransactions);
            double amountVarianceScore = analyzeAmountVariance(sortedTransactions);
            double velocityScore = analyzeVelocityPattern(sortedTransactions);
            double timingScore = analyzeTimingPattern(sortedTransactions);
            
            // Calculate overall anomaly score
            double overallScore = calculateOverallScore(frequencyScore, amountVarianceScore, velocityScore, timingScore);
            
            // Determine if pattern is anomalous
            boolean isAnomalous = overallScore > FREQUENCY_THRESHOLD;
            
            // Identify the primary pattern type
            String patternType = identifyPatternType(frequencyScore, amountVarianceScore, velocityScore, timingScore);
            
            // Identify specific anomaly features
            List<String> anomalyFeatures = identifyPatternAnomalies(
                frequencyScore, amountVarianceScore, velocityScore, timingScore, sortedTransactions
            );
            
            // Build metrics
            Map<String, Object> metrics = buildPatternMetrics(
                frequencyScore, amountVarianceScore, velocityScore, timingScore, sortedTransactions
            );
            
            logger.info("Pattern analysis completed for customer {}: anomalous={}, pattern_type={}, score={}", 
                customerId, isAnomalous, patternType, overallScore);
            
            return new TransactionPatternAnalysis(
                customerId,
                isAnomalous,
                overallScore,
                patternType,
                anomalyFeatures,
                metrics
            );
            
        } catch (Exception e) {
            logger.error("Error analyzing patterns for customer {}: {}", customerId, e.getMessage());
            throw new MLModelException("Pattern analysis failed", e);
        }
    }
    
    /**
     * Analyze frequency pattern (rapid consecutive transactions)
     */
    private double analyzeFrequencyPattern(List<TransactionData> transactions) {
        if (transactions.size() < 2) return 0.0;
        
        double totalScore = 0.0;
        int rapidTransactionCount = 0;
        
        for (int i = 1; i < transactions.size(); i++) {
            LocalDateTime prev = transactions.get(i - 1).getTimestamp();
            LocalDateTime current = transactions.get(i).getTimestamp();
            
            long minutesBetween = ChronoUnit.MINUTES.between(prev, current);
            
            // Score based on how rapid the transactions are
            if (minutesBetween < 5) {
                totalScore += 0.8;
                rapidTransactionCount++;
            } else if (minutesBetween < 15) {
                totalScore += 0.5;
                rapidTransactionCount++;
            } else if (minutesBetween < 60) {
                totalScore += 0.2;
            }
        }
        
        // Normalize score
        return Math.min(1.0, totalScore / (transactions.size() - 1));
    }
    
    /**
     * Analyze amount variance pattern
     */
    private double analyzeAmountVariance(List<TransactionData> transactions) {
        if (transactions.size() < 2) return 0.0;
        
        List<Double> amounts = transactions.stream()
            .map(TransactionData::getAmount)
            .collect(Collectors.toList());
        
        // Calculate mean and standard deviation
        double mean = amounts.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = amounts.stream()
            .mapToDouble(amount -> Math.pow(amount - mean, 2))
            .average()
            .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        double coefficientOfVariation = stdDev / mean;
        
        // Check for increasing/decreasing patterns
        boolean isIncreasing = isIncreasingPattern(amounts);
        boolean isDecreasing = isDecreasingPattern(amounts);
        
        double varianceScore = Math.min(1.0, coefficientOfVariation);
        
        // Boost score for clear patterns
        if (isIncreasing || isDecreasing) {
            varianceScore = Math.min(1.0, varianceScore + 0.3);
        }
        
        return varianceScore;
    }
    
    /**
     * Analyze velocity pattern (transaction speed)
     */
    private double analyzeVelocityPattern(List<TransactionData> transactions) {
        if (transactions.size() < 3) return 0.0;
        
        double totalVelocityScore = 0.0;
        
        for (int i = 2; i < transactions.size(); i++) {
            LocalDateTime first = transactions.get(i - 2).getTimestamp();
            LocalDateTime second = transactions.get(i - 1).getTimestamp();
            LocalDateTime third = transactions.get(i).getTimestamp();
            
            long interval1 = ChronoUnit.MINUTES.between(first, second);
            long interval2 = ChronoUnit.MINUTES.between(second, third);
            
            // Check for acceleration in transaction velocity
            if (interval2 < interval1 && interval2 < 30) {
                totalVelocityScore += 0.7;
            } else if (interval1 < 60 && interval2 < 60) {
                totalVelocityScore += 0.4;
            }
        }
        
        return Math.min(1.0, totalVelocityScore / (transactions.size() - 2));
    }
    
    /**
     * Analyze timing pattern (unusual hours)
     */
    private double analyzeTimingPattern(List<TransactionData> transactions) {
        int nightTransactions = 0;
        int weekendTransactions = 0;
        
        for (TransactionData transaction : transactions) {
            LocalDateTime timestamp = transaction.getTimestamp();
            
            // Check for night time transactions (10 PM to 6 AM)
            int hour = timestamp.getHour();
            if (hour >= 22 || hour <= 6) {
                nightTransactions++;
            }
            
            // Check for weekend transactions
            int dayOfWeek = timestamp.getDayOfWeek().getValue();
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                weekendTransactions++;
            }
        }
        
        double nightRatio = (double) nightTransactions / transactions.size();
        double weekendRatio = (double) weekendTransactions / transactions.size();
        
        // Calculate timing anomaly score
        double timingScore = 0.0;
        
        if (nightRatio > 0.5) {
            timingScore += nightRatio * 0.7;
        }
        
        if (weekendRatio > 0.6) {
            timingScore += weekendRatio * 0.5;
        }
        
        return Math.min(1.0, timingScore);
    }
    
    /**
     * Calculate overall anomaly score
     */
    private double calculateOverallScore(double frequencyScore, double amountVarianceScore, 
                                       double velocityScore, double timingScore) {
        
        // Weighted combination of different scores
        double weightedScore = 
            frequencyScore * 0.3 +
            amountVarianceScore * 0.25 +
            velocityScore * 0.25 +
            timingScore * 0.2;
        
        return Math.min(1.0, weightedScore);
    }
    
    /**
     * Identify primary pattern type
     */
    private String identifyPatternType(double frequencyScore, double amountVarianceScore, 
                                     double velocityScore, double timingScore) {
        
        double maxScore = Math.max(Math.max(frequencyScore, amountVarianceScore), 
                                  Math.max(velocityScore, timingScore));
        
        if (maxScore < FREQUENCY_THRESHOLD) {
            return "NORMAL";
        }
        
        if (frequencyScore == maxScore) {
            return "UNUSUAL_FREQUENCY";
        } else if (amountVarianceScore == maxScore) {
            return "UNUSUAL_AMOUNTS";
        } else if (velocityScore == maxScore) {
            return "UNUSUAL_VELOCITY";
        } else {
            return "UNUSUAL_TIMING";
        }
    }
    
    /**
     * Identify specific pattern anomalies
     */
    private List<String> identifyPatternAnomalies(double frequencyScore, double amountVarianceScore, 
                                                 double velocityScore, double timingScore, 
                                                 List<TransactionData> transactions) {
        
        List<String> anomalies = new ArrayList<>();
        
        if (frequencyScore > FREQUENCY_THRESHOLD) {
            anomalies.add("rapid_consecutive_transfers");
        }
        
        if (amountVarianceScore > AMOUNT_VARIANCE_THRESHOLD) {
            List<Double> amounts = transactions.stream()
                .map(TransactionData::getAmount)
                .collect(Collectors.toList());
            
            if (isIncreasingPattern(amounts)) {
                anomalies.add("increasing_amounts");
            } else if (isDecreasingPattern(amounts)) {
                anomalies.add("decreasing_amounts");
            } else {
                anomalies.add("irregular_amounts");
            }
        }
        
        if (velocityScore > VELOCITY_THRESHOLD) {
            anomalies.add("accelerating_velocity");
        }
        
        if (timingScore > 0.5) {
            anomalies.add("unusual_timing");
        }
        
        return anomalies;
    }
    
    /**
     * Build pattern analysis metrics
     */
    private Map<String, Object> buildPatternMetrics(double frequencyScore, double amountVarianceScore, 
                                                   double velocityScore, double timingScore, 
                                                   List<TransactionData> transactions) {
        
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("frequency_score", Math.round(frequencyScore * 100.0) / 100.0);
        metrics.put("amount_variance", Math.round(amountVarianceScore * 100.0) / 100.0);
        metrics.put("velocity_score", Math.round(velocityScore * 100.0) / 100.0);
        metrics.put("timing_score", Math.round(timingScore * 100.0) / 100.0);
        metrics.put("transaction_count", transactions.size());
        
        // Time span analysis
        if (transactions.size() > 1) {
            LocalDateTime start = transactions.get(0).getTimestamp();
            LocalDateTime end = transactions.get(transactions.size() - 1).getTimestamp();
            long timeSpanMinutes = ChronoUnit.MINUTES.between(start, end);
            metrics.put("time_span_minutes", timeSpanMinutes);
        }
        
        return metrics;
    }
    
    /**
     * Check if amounts follow increasing pattern
     */
    private boolean isIncreasingPattern(List<Double> amounts) {
        if (amounts.size() < 3) return false;
        
        int increasingCount = 0;
        for (int i = 1; i < amounts.size(); i++) {
            if (amounts.get(i) > amounts.get(i - 1)) {
                increasingCount++;
            }
        }
        
        return increasingCount >= (amounts.size() - 1) * 0.7;
    }
    
    /**
     * Check if amounts follow decreasing pattern
     */
    private boolean isDecreasingPattern(List<Double> amounts) {
        if (amounts.size() < 3) return false;
        
        int decreasingCount = 0;
        for (int i = 1; i < amounts.size(); i++) {
            if (amounts.get(i) < amounts.get(i - 1)) {
                decreasingCount++;
            }
        }
        
        return decreasingCount >= (amounts.size() - 1) * 0.7;
    }
    
    /**
     * Create normal pattern result
     */
    private TransactionPatternAnalysis createNormalPattern(String customerId, String reason) {
        return new TransactionPatternAnalysis(
            customerId,
            false,
            0.0,
            "NORMAL",
            List.of(),
            Map.of("reason", reason)
        );
    }
}