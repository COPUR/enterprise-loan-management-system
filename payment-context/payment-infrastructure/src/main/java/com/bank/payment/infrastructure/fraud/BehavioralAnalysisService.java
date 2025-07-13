package com.bank.payment.infrastructure.fraud;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Behavioral Analysis Service for Fraud Detection
 * 
 * Analyzes customer behavioral patterns to detect anomalies:
 * - Transaction timing patterns
 * - Amount distribution analysis
 * - Payment type preferences
 * - Velocity pattern changes
 * - Seasonal behavior modeling
 */
@Service
public class BehavioralAnalysisService {
    
    // Customer behavior profiles cache
    private final Map<String, CustomerBehaviorProfile> behaviorProfiles = new ConcurrentHashMap<>();
    
    /**
     * Analyze behavioral patterns for fraud detection
     */
    public int analyzeBehavioralPatterns(String customerId, TransactionContext context) {
        CustomerBehaviorProfile profile = getOrCreateProfile(customerId);
        
        int riskScore = 0;
        
        // 1. Timing pattern analysis
        riskScore += analyzeTimingPatterns(profile, context);
        
        // 2. Amount pattern analysis
        riskScore += analyzeAmountPatterns(profile, context);
        
        // 3. Payment type pattern analysis
        riskScore += analyzePaymentTypePatterns(profile, context);
        
        // 4. Velocity pattern analysis
        riskScore += analyzeVelocityPatterns(profile, context);
        
        // 5. Frequency pattern analysis
        riskScore += analyzeFrequencyPatterns(profile, context);
        
        // Update profile with new transaction
        updateProfile(profile, context);
        
        return Math.min(100, riskScore);
    }
    
    /**
     * Analyze timing patterns for anomalies
     */
    private int analyzeTimingPatterns(CustomerBehaviorProfile profile, TransactionContext context) {
        int score = 0;
        
        // Check hour-of-day patterns
        Map<Integer, Integer> hourlyFrequency = profile.getHourlyFrequency();
        int currentHour = context.getTimestamp().getHour();
        
        if (hourlyFrequency.isEmpty()) {
            // New customer - slight risk increase
            score += 5;
        } else {
            double avgFrequency = hourlyFrequency.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(1.0);
            
            int currentHourFreq = hourlyFrequency.getOrDefault(currentHour, 0);
            
            // If current hour has very low historical frequency
            if (currentHourFreq < avgFrequency * 0.1) {
                score += 15;
            }
            
            // Very late night transactions (2-5 AM)
            if (currentHour >= 2 && currentHour <= 5 && currentHourFreq == 0) {
                score += 20;
            }
        }
        
        // Check day-of-week patterns
        Map<Integer, Integer> weeklyFrequency = profile.getWeeklyFrequency();
        int currentDay = context.getTimestamp().getDayOfWeek().getValue();
        
        if (!weeklyFrequency.isEmpty()) {
            double avgWeeklyFreq = weeklyFrequency.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(1.0);
            
            int currentDayFreq = weeklyFrequency.getOrDefault(currentDay, 0);
            
            if (currentDayFreq < avgWeeklyFreq * 0.2) {
                score += 10;
            }
        }
        
        return score;
    }
    
    /**
     * Analyze amount patterns for anomalies
     */
    private int analyzeAmountPatterns(CustomerBehaviorProfile profile, TransactionContext context) {
        int score = 0;
        List<BigDecimal> historicalAmounts = profile.getHistoricalAmounts();
        
        if (historicalAmounts.isEmpty()) {
            // New customer with large first transaction
            if (context.getAmount().compareTo(BigDecimal.valueOf(5000)) > 0) {
                score += 25;
            }
            return score;
        }
        
        // Calculate statistical measures
        double mean = historicalAmounts.stream()
            .mapToDouble(BigDecimal::doubleValue)
            .average()
            .orElse(0.0);
        
        double stdDev = calculateStandardDeviation(historicalAmounts, mean);
        double currentAmount = context.getAmount().doubleValue();
        
        // Z-score analysis
        double zScore = Math.abs((currentAmount - mean) / (stdDev + 1)); // +1 to avoid division by zero
        
        if (zScore > 3.0) {
            score += 30; // Very unusual amount
        } else if (zScore > 2.0) {
            score += 15; // Unusual amount
        }
        
        // Check for round number patterns (potential test transactions)
        if (isRoundNumber(context.getAmount()) && !hasRoundNumberHistory(historicalAmounts)) {
            score += 10;
        }
        
        // Check for micro-transaction testing
        if (context.getAmount().compareTo(BigDecimal.valueOf(1)) < 0 && mean > 100) {
            score += 20;
        }
        
        return score;
    }
    
    /**
     * Analyze payment type patterns
     */
    private int analyzePaymentTypePatterns(CustomerBehaviorProfile profile, TransactionContext context) {
        int score = 0;
        Map<String, Integer> typeFrequency = profile.getPaymentTypeFrequency();
        
        String currentType = context.getPaymentType().name();
        
        if (typeFrequency.isEmpty()) {
            // New customer using high-risk payment type
            if ("WIRE_TRANSFER".equals(currentType) || "INTERNATIONAL_TRANSFER".equals(currentType)) {
                score += 20;
            }
            return score;
        }
        
        // Check if this is a new payment type for the customer
        if (!typeFrequency.containsKey(currentType)) {
            switch (currentType) {
                case "WIRE_TRANSFER":
                    score += 25;
                    break;
                case "INTERNATIONAL_TRANSFER":
                    score += 30;
                    break;
                case "ACH_TRANSFER":
                    score += 10;
                    break;
                default:
                    score += 5;
            }
        }
        
        // Check for sudden type switching
        String mostFrequentType = typeFrequency.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");
        
        if (!currentType.equals(mostFrequentType) && 
            typeFrequency.getOrDefault(currentType, 0) < typeFrequency.get(mostFrequentType) * 0.1) {
            score += 15;
        }
        
        return score;
    }
    
    /**
     * Analyze velocity patterns
     */
    private int analyzeVelocityPatterns(CustomerBehaviorProfile profile, TransactionContext context) {
        int score = 0;
        List<TransactionContext> recentTransactions = profile.getRecentTransactions(24); // Last 24 hours
        
        if (recentTransactions.size() > profile.getAverageTransactionsPerDay() * 3) {
            score += 25; // Unusual transaction frequency
        }
        
        // Check for burst patterns
        List<TransactionContext> lastHour = profile.getRecentTransactions(1);
        if (lastHour.size() > 5) {
            score += 20; // Too many transactions in last hour
        }
        
        // Check for late night bursts
        int currentHour = context.getTimestamp().getHour();
        if ((currentHour < 6 || currentHour > 22) && lastHour.size() > 2) {
            score += 15;
        }
        
        return score;
    }
    
    /**
     * Analyze frequency patterns
     */
    private int analyzeFrequencyPatterns(CustomerBehaviorProfile profile, TransactionContext context) {
        int score = 0;
        
        // Check for regular interval patterns (potential automated attacks)
        List<TransactionContext> recent = profile.getRecentTransactions(6); // Last 6 hours
        if (recent.size() >= 3) {
            List<Long> intervals = calculateIntervals(recent);
            
            // Check for very regular intervals (within 10% variance)
            if (intervals.size() >= 2 && isRegularPattern(intervals, 0.1)) {
                score += 25;
            }
        }
        
        return score;
    }
    
    /**
     * Get or create customer behavior profile
     */
    private CustomerBehaviorProfile getOrCreateProfile(String customerId) {
        return behaviorProfiles.computeIfAbsent(customerId, k -> new CustomerBehaviorProfile(customerId));
    }
    
    /**
     * Update customer profile with new transaction
     */
    private void updateProfile(CustomerBehaviorProfile profile, TransactionContext context) {
        profile.addTransaction(context);
    }
    
    // Helper methods
    private double calculateStandardDeviation(List<BigDecimal> values, double mean) {
        double sumSquaredDiff = values.stream()
            .mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2))
            .sum();
        return Math.sqrt(sumSquaredDiff / values.size());
    }
    
    private boolean isRoundNumber(BigDecimal amount) {
        double value = amount.doubleValue();
        return value % 100 == 0 || value % 1000 == 0 || value % 10000 == 0;
    }
    
    private boolean hasRoundNumberHistory(List<BigDecimal> amounts) {
        return amounts.stream()
            .anyMatch(this::isRoundNumber);
    }
    
    private List<Long> calculateIntervals(List<TransactionContext> transactions) {
        List<Long> intervals = new ArrayList<>();
        
        for (int i = 1; i < transactions.size(); i++) {
            long interval = ChronoUnit.MINUTES.between(
                transactions.get(i-1).getTimestamp(),
                transactions.get(i).getTimestamp()
            );
            intervals.add(interval);
        }
        
        return intervals;
    }
    
    private boolean isRegularPattern(List<Long> intervals, double tolerance) {
        if (intervals.isEmpty()) return false;
        
        double avgInterval = intervals.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
        
        return intervals.stream()
            .allMatch(interval -> {
                double deviation = Math.abs(interval - avgInterval) / avgInterval;
                return deviation <= tolerance;
            });
    }
    
    /**
     * Customer behavior profile
     */
    public static class CustomerBehaviorProfile {
        private final String customerId;
        private final List<TransactionContext> transactions;
        private final Map<Integer, Integer> hourlyFrequency;
        private final Map<Integer, Integer> weeklyFrequency;
        private final Map<String, Integer> paymentTypeFrequency;
        private final List<BigDecimal> historicalAmounts;
        
        public CustomerBehaviorProfile(String customerId) {
            this.customerId = customerId;
            this.transactions = new ArrayList<>();
            this.hourlyFrequency = new HashMap<>();
            this.weeklyFrequency = new HashMap<>();
            this.paymentTypeFrequency = new HashMap<>();
            this.historicalAmounts = new ArrayList<>();
        }
        
        public void addTransaction(TransactionContext context) {
            transactions.add(context);
            
            // Update frequency maps
            int hour = context.getTimestamp().getHour();
            int day = context.getTimestamp().getDayOfWeek().getValue();
            String type = context.getPaymentType().name();
            
            hourlyFrequency.merge(hour, 1, Integer::sum);
            weeklyFrequency.merge(day, 1, Integer::sum);
            paymentTypeFrequency.merge(type, 1, Integer::sum);
            historicalAmounts.add(context.getAmount());
            
            // Keep only recent transactions (last 90 days)
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
            transactions.removeIf(tx -> tx.getTimestamp().isBefore(cutoff));
            
            // Keep only recent amounts (last 100 transactions)
            if (historicalAmounts.size() > 100) {
                historicalAmounts.remove(0);
            }
        }
        
        public List<TransactionContext> getRecentTransactions(int hours) {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
            return transactions.stream()
                .filter(tx -> tx.getTimestamp().isAfter(cutoff))
                .collect(Collectors.toList());
        }
        
        public double getAverageTransactionsPerDay() {
            if (transactions.isEmpty()) return 0;
            
            LocalDateTime earliest = transactions.stream()
                .map(TransactionContext::getTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
            
            long days = ChronoUnit.DAYS.between(earliest, LocalDateTime.now());
            return days > 0 ? (double) transactions.size() / days : transactions.size();
        }
        
        // Getters
        public String getCustomerId() { return customerId; }
        public Map<Integer, Integer> getHourlyFrequency() { return hourlyFrequency; }
        public Map<Integer, Integer> getWeeklyFrequency() { return weeklyFrequency; }
        public Map<String, Integer> getPaymentTypeFrequency() { return paymentTypeFrequency; }
        public List<BigDecimal> getHistoricalAmounts() { return new ArrayList<>(historicalAmounts); }
    }
}