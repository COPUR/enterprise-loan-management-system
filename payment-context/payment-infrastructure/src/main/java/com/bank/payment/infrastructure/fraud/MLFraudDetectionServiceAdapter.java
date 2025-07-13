package com.bank.payment.infrastructure.fraud;

import com.bank.payment.application.FraudDetectionService;
import com.bank.payment.application.FraudDetectedException;
import com.bank.payment.domain.Payment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Advanced ML-based Fraud Detection Service
 * 
 * Implements sophisticated fraud detection using machine learning algorithms:
 * - Real-time risk scoring with ensemble models
 * - Behavioral pattern analysis
 * - Velocity and anomaly detection
 * - Time-series analysis for transaction patterns
 * - Geographic and device fingerprinting
 * - Network analysis for suspicious connections
 */
@Service
public class MLFraudDetectionServiceAdapter implements FraudDetectionService {
    
    private final MLModelService mlModelService;
    private final TransactionHistoryService transactionHistoryService;
    private final BehavioralAnalysisService behavioralAnalysisService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final GeospatialAnalysisService geospatialAnalysisService;
    private final NetworkAnalysisService networkAnalysisService;
    
    // Configuration parameters
    private static final int RISK_THRESHOLD = 75;
    private static final BigDecimal DAILY_VELOCITY_LIMIT = BigDecimal.valueOf(50000);
    private static final BigDecimal HOURLY_VELOCITY_LIMIT = BigDecimal.valueOf(10000);
    private static final double ANOMALY_THRESHOLD = 0.95;
    
    // In-memory caches for real-time analysis
    private final Map<String, List<TransactionEvent>> customerTransactionCache = new ConcurrentHashMap<>();
    private final Map<String, DeviceProfile> deviceProfileCache = new ConcurrentHashMap<>();
    private final Map<String, GeolocationProfile> geolocationCache = new ConcurrentHashMap<>();
    
    public MLFraudDetectionServiceAdapter(
            MLModelService mlModelService,
            TransactionHistoryService transactionHistoryService,
            BehavioralAnalysisService behavioralAnalysisService,
            AnomalyDetectionService anomalyDetectionService,
            GeospatialAnalysisService geospatialAnalysisService,
            NetworkAnalysisService networkAnalysisService) {
        this.mlModelService = mlModelService;
        this.transactionHistoryService = transactionHistoryService;
        this.behavioralAnalysisService = behavioralAnalysisService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.geospatialAnalysisService = geospatialAnalysisService;
        this.networkAnalysisService = networkAnalysisService;
    }
    
    @Override
    public boolean isValidPayment(Payment payment) {
        try {
            // Comprehensive fraud check using ensemble approach
            FraudAnalysisResult result = performComprehensiveAnalysis(payment);
            
            // Log analysis for audit trail
            logFraudAnalysis(payment, result);
            
            // Throw exception if fraud detected
            if (result.isFraudulent()) {
                throw new FraudDetectedException(
                    "Payment blocked due to fraud detection",
                    result.getRiskScore(),
                    result.getRiskFactors()
                );
            }
            
            return true;
            
        } catch (Exception e) {
            // Log error and fail safe (block transaction)
            System.err.println("Fraud detection service error: " + e.getMessage());
            throw new FraudDetectedException(
                "Payment blocked due to fraud detection service error",
                100,
                List.of("Service unavailable")
            );
        }
    }
    
    @Override
    public int calculateRiskScore(Payment payment) {
        return performComprehensiveAnalysis(payment).getRiskScore();
    }
    
    @Override
    public boolean exceedsVelocityLimits(Payment payment) {
        return calculateVelocityScore(payment) > 0;
    }
    
    @Override
    public boolean isSuspiciousPattern(Payment payment) {
        return calculatePatternScore(payment) > 0;
    }
    
    /**
     * Perform comprehensive fraud analysis using multiple ML models
     */
    private FraudAnalysisResult performComprehensiveAnalysis(Payment payment) {
        List<String> riskFactors = new ArrayList<>();
        int totalRiskScore = 0;
        
        // 1. ML Model-based risk scoring
        int mlRiskScore = mlModelService.predictRiskScore(extractFeatures(payment));
        totalRiskScore += mlRiskScore;
        if (mlRiskScore > 60) {
            riskFactors.add("High ML model risk score: " + mlRiskScore);
        }
        
        // 2. Velocity analysis
        int velocityScore = calculateVelocityScore(payment);
        totalRiskScore += velocityScore;
        if (velocityScore > 0) {
            riskFactors.add("Velocity limits exceeded");
        }
        
        // 3. Behavioral pattern analysis
        int patternScore = calculatePatternScore(payment);
        totalRiskScore += patternScore;
        if (patternScore > 0) {
            riskFactors.add("Suspicious behavioral patterns");
        }
        
        // 4. Anomaly detection
        int anomalyScore = calculateAnomalyScore(payment);
        totalRiskScore += anomalyScore;
        if (anomalyScore > 0) {
            riskFactors.add("Transaction anomaly detected");
        }
        
        // 5. Geospatial analysis
        int geoScore = calculateGeospatialScore(payment);
        totalRiskScore += geoScore;
        if (geoScore > 0) {
            riskFactors.add("Suspicious geographic patterns");
        }
        
        // 6. Network analysis
        int networkScore = calculateNetworkScore(payment);
        totalRiskScore += networkScore;
        if (networkScore > 0) {
            riskFactors.add("Suspicious network connections");
        }
        
        // 7. Device fingerprinting
        int deviceScore = calculateDeviceScore(payment);
        totalRiskScore += deviceScore;
        if (deviceScore > 0) {
            riskFactors.add("Device fingerprint anomalies");
        }
        
        // Normalize total risk score (max 100)
        int finalRiskScore = Math.min(100, totalRiskScore);
        
        return new FraudAnalysisResult(
            finalRiskScore,
            finalRiskScore >= RISK_THRESHOLD,
            riskFactors,
            generateRecommendations(finalRiskScore, riskFactors)
        );
    }
    
    /**
     * Calculate velocity-based risk score
     */
    private int calculateVelocityScore(Payment payment) {
        String customerId = payment.getCustomerId();
        LocalDateTime now = LocalDateTime.now();
        
        // Get recent transactions for velocity analysis
        List<TransactionEvent> recentTransactions = getRecentTransactions(customerId, now.minusDays(1));
        
        // Calculate daily velocity
        BigDecimal dailyAmount = recentTransactions.stream()
            .filter(tx -> tx.getTimestamp().isAfter(now.minusDays(1)))
            .map(TransactionEvent::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        // Calculate hourly velocity
        BigDecimal hourlyAmount = recentTransactions.stream()
            .filter(tx -> tx.getTimestamp().isAfter(now.minusHours(1)))
            .map(TransactionEvent::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int score = 0;
        
        // Check daily limit
        if (dailyAmount.add(payment.getAmount()).compareTo(DAILY_VELOCITY_LIMIT) > 0) {
            score += 30;
        }
        
        // Check hourly limit
        if (hourlyAmount.add(payment.getAmount()).compareTo(HOURLY_VELOCITY_LIMIT) > 0) {
            score += 25;
        }
        
        // Check frequency patterns
        long transactionCount = recentTransactions.stream()
            .filter(tx -> tx.getTimestamp().isAfter(now.minusHours(1)))
            .count();
            
        if (transactionCount > 10) {
            score += 20;
        }
        
        return score;
    }
    
    /**
     * Calculate pattern-based risk score
     */
    private int calculatePatternScore(Payment payment) {
        return behavioralAnalysisService.analyzeBehavioralPatterns(
            payment.getCustomerId(),
            extractTransactionContext(payment)
        );
    }
    
    /**
     * Calculate anomaly-based risk score
     */
    private int calculateAnomalyScore(Payment payment) {
        double anomalyScore = anomalyDetectionService.calculateAnomalyScore(
            extractFeatures(payment)
        );
        
        return anomalyScore > ANOMALY_THRESHOLD ? (int)((anomalyScore - ANOMALY_THRESHOLD) * 100) : 0;
    }
    
    /**
     * Calculate geospatial risk score
     */
    private int calculateGeospatialScore(Payment payment) {
        return geospatialAnalysisService.analyzeGeospatialRisk(
            payment.getCustomerId(),
            extractGeolocationData(payment)
        );
    }
    
    /**
     * Calculate network-based risk score
     */
    private int calculateNetworkScore(Payment payment) {
        return networkAnalysisService.analyzeNetworkRisk(
            extractNetworkContext(payment)
        );
    }
    
    /**
     * Calculate device-based risk score
     */
    private int calculateDeviceScore(Payment payment) {
        String deviceFingerprint = extractDeviceFingerprint(payment);
        DeviceProfile profile = deviceProfileCache.get(payment.getCustomerId());
        
        if (profile == null) {
            // New device for customer
            return 15;
        }
        
        return profile.isKnownDevice(deviceFingerprint) ? 0 : 20;
    }
    
    /**
     * Extract ML features from payment
     */
    private MLFeatures extractFeatures(Payment payment) {
        return MLFeatures.builder()
            .customerId(payment.getCustomerId())
            .amount(payment.getAmount())
            .paymentType(payment.getPaymentType().name())
            .timestamp(LocalDateTime.now())
            .dayOfWeek(LocalDateTime.now().getDayOfWeek().getValue())
            .hourOfDay(LocalDateTime.now().getHour())
            .fromAccountType(getAccountType(payment.getFromAccountId()))
            .toAccountType(getAccountType(payment.getToAccountId()))
            .description(payment.getDescription())
            .build();
    }
    
    /**
     * Extract transaction context for behavioral analysis
     */
    private TransactionContext extractTransactionContext(Payment payment) {
        return TransactionContext.builder()
            .amount(payment.getAmount())
            .paymentType(payment.getPaymentType())
            .timestamp(LocalDateTime.now())
            .fromAccount(payment.getFromAccountId())
            .toAccount(payment.getToAccountId())
            .description(payment.getDescription())
            .build();
    }
    
    /**
     * Extract geolocation data from payment
     */
    private GeolocationData extractGeolocationData(Payment payment) {
        // This would typically come from request headers or session data
        return GeolocationData.builder()
            .ipAddress("192.168.1.1") // Mock data
            .country("US")
            .city("New York")
            .latitude(40.7128)
            .longitude(-74.0060)
            .build();
    }
    
    /**
     * Extract network context from payment
     */
    private NetworkContext extractNetworkContext(Payment payment) {
        return NetworkContext.builder()
            .ipAddress("192.168.1.1")
            .userAgent("MockUserAgent")
            .sessionId("mock-session-id")
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    /**
     * Extract device fingerprint from payment
     */
    private String extractDeviceFingerprint(Payment payment) {
        // This would typically come from browser fingerprinting or mobile device ID
        return "mock-device-fingerprint-" + payment.getCustomerId();
    }
    
    /**
     * Get recent transactions for customer
     */
    private List<TransactionEvent> getRecentTransactions(String customerId, LocalDateTime since) {
        return customerTransactionCache.getOrDefault(customerId, new ArrayList<>())
            .stream()
            .filter(tx -> tx.getTimestamp().isAfter(since))
            .collect(Collectors.toList());
    }
    
    /**
     * Get account type for risk analysis
     */
    private String getAccountType(String accountId) {
        // Mock implementation - would integrate with account service
        return accountId.startsWith("CHECKING") ? "CHECKING" : "SAVINGS";
    }
    
    /**
     * Generate risk mitigation recommendations
     */
    private List<String> generateRecommendations(int riskScore, List<String> riskFactors) {
        List<String> recommendations = new ArrayList<>();
        
        if (riskScore > 80) {
            recommendations.add("Block transaction immediately");
            recommendations.add("Notify customer security team");
            recommendations.add("Require additional authentication");
        } else if (riskScore > 60) {
            recommendations.add("Require step-up authentication");
            recommendations.add("Add transaction monitoring");
            recommendations.add("Request manual review");
        } else if (riskScore > 40) {
            recommendations.add("Increase monitoring for this customer");
            recommendations.add("Consider transaction limits");
        }
        
        if (riskFactors.contains("Velocity limits exceeded")) {
            recommendations.add("Implement temporary velocity controls");
        }
        
        if (riskFactors.contains("Suspicious geographic patterns")) {
            recommendations.add("Verify customer location");
        }
        
        return recommendations;
    }
    
    /**
     * Log fraud analysis for audit and compliance
     */
    private void logFraudAnalysis(Payment payment, FraudAnalysisResult result) {
        System.out.println(String.format(
            "FRAUD_ANALYSIS: PaymentId=%s CustomerId=%s RiskScore=%d Fraudulent=%s Factors=%s",
            payment.getPaymentId(),
            payment.getCustomerId(),
            result.getRiskScore(),
            result.isFraudulent(),
            String.join(", ", result.getRiskFactors())
        ));
    }
    
    /**
     * Fraud analysis result holder
     */
    private static class FraudAnalysisResult {
        private final int riskScore;
        private final boolean fraudulent;
        private final List<String> riskFactors;
        private final List<String> recommendations;
        
        public FraudAnalysisResult(int riskScore, boolean fraudulent, 
                                 List<String> riskFactors, List<String> recommendations) {
            this.riskScore = riskScore;
            this.fraudulent = fraudulent;
            this.riskFactors = riskFactors;
            this.recommendations = recommendations;
        }
        
        public int getRiskScore() { return riskScore; }
        public boolean isFraudulent() { return fraudulent; }
        public List<String> getRiskFactors() { return riskFactors; }
        public List<String> getRecommendations() { return recommendations; }
    }
    
    /**
     * Transaction event for velocity analysis
     */
    private static class TransactionEvent {
        private final BigDecimal amount;
        private final LocalDateTime timestamp;
        private final String type;
        
        public TransactionEvent(BigDecimal amount, LocalDateTime timestamp, String type) {
            this.amount = amount;
            this.timestamp = timestamp;
            this.type = type;
        }
        
        public BigDecimal getAmount() { return amount; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getType() { return type; }
    }
    
    /**
     * Device profile for device fingerprinting
     */
    private static class DeviceProfile {
        private final Set<String> knownFingerprints;
        
        public DeviceProfile() {
            this.knownFingerprints = new HashSet<>();
        }
        
        public boolean isKnownDevice(String fingerprint) {
            return knownFingerprints.contains(fingerprint);
        }
        
        public void addFingerprint(String fingerprint) {
            knownFingerprints.add(fingerprint);
        }
    }
    
    /**
     * Geolocation profile for geographic analysis
     */
    private static class GeolocationProfile {
        private final List<String> frequentLocations;
        
        public GeolocationProfile() {
            this.frequentLocations = new ArrayList<>();
        }
        
        public List<String> getFrequentLocations() { return frequentLocations; }
        public void addLocation(String location) { frequentLocations.add(location); }
    }
}