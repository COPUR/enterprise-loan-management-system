package com.bank.ml.anomaly;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bank.ml.anomaly.model.*;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Incident Prediction Service
 * Predicts potential system incidents based on historical patterns and ML models
 */
@Service
public class IncidentPredictionService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentPredictionService.class);
    
    // Prediction thresholds
    private static final double HIGH_PROBABILITY_THRESHOLD = 0.8;
    private static final double MEDIUM_PROBABILITY_THRESHOLD = 0.5;
    private static final double LOW_PROBABILITY_THRESHOLD = 0.3;
    
    // Time windows for prediction
    private static final int PREDICTION_WINDOW_HOURS = 24;
    private static final int PATTERN_ANALYSIS_DAYS = 30;
    
    /**
     * Predict incidents based on historical data and patterns
     */
    public List<IncidentPrediction> predictIncidents(HistoricalData historicalData) {
        logger.info("Predicting incidents for period: {} to {}", 
            historicalData.getStartTime(), historicalData.getEndTime());
        
        try {
            List<IncidentPrediction> predictions = new ArrayList<>();
            
            // Analyze different types of potential incidents
            predictions.addAll(predictPaymentSystemIncidents(historicalData));
            predictions.addAll(predictLoanProcessingIncidents(historicalData));
            predictions.addAll(predictFraudDetectionIncidents(historicalData));
            predictions.addAll(predictSystemPerformanceIncidents(historicalData));
            predictions.addAll(predictComplianceIncidents(historicalData));
            
            // Sort by probability (highest first)
            predictions.sort((p1, p2) -> Double.compare(p2.getProbability(), p1.getProbability()));
            
            logger.info("Generated {} incident predictions", predictions.size());
            
            return predictions;
            
        } catch (Exception e) {
            logger.error("Error predicting incidents: {}", e.getMessage());
            throw new MLModelException("Incident prediction failed", e);
        }
    }
    
    /**
     * Predict payment system incidents
     */
    private List<IncidentPrediction> predictPaymentSystemIncidents(HistoricalData historicalData) {
        List<IncidentPrediction> predictions = new ArrayList<>();
        
        // Analyze payment volume patterns
        double paymentVolumeScore = analyzePaymentVolumePattern(historicalData);
        
        // Predict payment system overload
        if (paymentVolumeScore > MEDIUM_PROBABILITY_THRESHOLD) {
            LocalDateTime predictedTime = calculatePredictionTime(historicalData, "payment_peak");
            
            predictions.add(new IncidentPrediction(
                generatePredictionId("PAYMENT_OVERLOAD"),
                predictedTime,
                "PAYMENT_SYSTEM_OVERLOAD",
                paymentVolumeScore,
                determineSeverity(paymentVolumeScore),
                List.of("payment_volume_spike", "processing_delay_pattern"),
                Map.of(
                    "probability", paymentVolumeScore,
                    "time_window", 2.0,
                    "impact_score", calculateImpactScore(paymentVolumeScore),
                    "confidence_interval", 0.85
                )
            ));
        }
        
        // Predict payment processing delays
        double processingDelayScore = analyzeProcessingDelayPattern(historicalData);
        
        if (processingDelayScore > MEDIUM_PROBABILITY_THRESHOLD) {
            LocalDateTime predictedTime = calculatePredictionTime(historicalData, "processing_delay");
            
            predictions.add(new IncidentPrediction(
                generatePredictionId("PAYMENT_DELAY"),
                predictedTime,
                "PAYMENT_PROCESSING_DELAY",
                processingDelayScore,
                determineSeverity(processingDelayScore),
                List.of("queue_backlog", "resource_contention"),
                Map.of(
                    "probability", processingDelayScore,
                    "time_window", 1.5,
                    "impact_score", calculateImpactScore(processingDelayScore)
                )
            ));
        }
        
        return predictions;
    }
    
    /**
     * Predict loan processing incidents
     */
    private List<IncidentPrediction> predictLoanProcessingIncidents(HistoricalData historicalData) {
        List<IncidentPrediction> predictions = new ArrayList<>();
        
        // Analyze loan application patterns
        double loanApplicationScore = analyzeLoanApplicationPattern(historicalData);
        
        if (loanApplicationScore > MEDIUM_PROBABILITY_THRESHOLD) {
            LocalDateTime predictedTime = calculatePredictionTime(historicalData, "loan_peak");
            
            predictions.add(new IncidentPrediction(
                generatePredictionId("LOAN_BACKLOG"),
                predictedTime,
                "LOAN_PROCESSING_BACKLOG",
                loanApplicationScore,
                determineSeverity(loanApplicationScore),
                List.of("application_volume_spike", "underwriter_capacity"),
                Map.of(
                    "probability", loanApplicationScore,
                    "time_window", 4.0,
                    "impact_score", calculateImpactScore(loanApplicationScore)
                )
            ));
        }
        
        return predictions;
    }
    
    /**
     * Predict fraud detection incidents
     */
    private List<IncidentPrediction> predictFraudDetectionIncidents(HistoricalData historicalData) {
        List<IncidentPrediction> predictions = new ArrayList<>();
        
        // Analyze fraud pattern trends
        double fraudPatternScore = analyzeFraudPatternTrends(historicalData);
        
        if (fraudPatternScore > MEDIUM_PROBABILITY_THRESHOLD) {
            LocalDateTime predictedTime = calculatePredictionTime(historicalData, "fraud_spike");
            
            predictions.add(new IncidentPrediction(
                generatePredictionId("FRAUD_SPIKE"),
                predictedTime,
                "FRAUD_DETECTION_SPIKE",
                fraudPatternScore,
                determineSeverity(fraudPatternScore),
                List.of("fraud_pattern_evolution", "attack_campaign"),
                Map.of(
                    "probability", fraudPatternScore,
                    "time_window", 3.0,
                    "impact_score", calculateImpactScore(fraudPatternScore)
                )
            ));
        }
        
        return predictions;
    }
    
    /**
     * Predict system performance incidents
     */
    private List<IncidentPrediction> predictSystemPerformanceIncidents(HistoricalData historicalData) {
        List<IncidentPrediction> predictions = new ArrayList<>();
        
        // Analyze system load patterns
        double systemLoadScore = analyzeSystemLoadPattern(historicalData);
        
        if (systemLoadScore > MEDIUM_PROBABILITY_THRESHOLD) {
            LocalDateTime predictedTime = calculatePredictionTime(historicalData, "system_load");
            
            predictions.add(new IncidentPrediction(
                generatePredictionId("SYSTEM_OVERLOAD"),
                predictedTime,
                "SYSTEM_PERFORMANCE_DEGRADATION",
                systemLoadScore,
                determineSeverity(systemLoadScore),
                List.of("cpu_memory_pressure", "response_time_degradation"),
                Map.of(
                    "probability", systemLoadScore,
                    "time_window", 1.0,
                    "impact_score", calculateImpactScore(systemLoadScore)
                )
            ));
        }
        
        return predictions;
    }
    
    /**
     * Predict compliance incidents
     */
    private List<IncidentPrediction> predictComplianceIncidents(HistoricalData historicalData) {
        List<IncidentPrediction> predictions = new ArrayList<>();
        
        // Analyze compliance violation patterns
        double complianceScore = analyzeCompliancePattern(historicalData);
        
        if (complianceScore > MEDIUM_PROBABILITY_THRESHOLD) {
            LocalDateTime predictedTime = calculatePredictionTime(historicalData, "compliance_check");
            
            predictions.add(new IncidentPrediction(
                generatePredictionId("COMPLIANCE_VIOLATION"),
                predictedTime,
                "COMPLIANCE_VIOLATION",
                complianceScore,
                determineSeverity(complianceScore),
                List.of("regulatory_deadline", "audit_preparation"),
                Map.of(
                    "probability", complianceScore,
                    "time_window", 8.0,
                    "impact_score", calculateImpactScore(complianceScore)
                )
            ));
        }
        
        return predictions;
    }
    
    /**
     * Analyze payment volume pattern
     */
    private double analyzePaymentVolumePattern(HistoricalData historicalData) {
        // Simulate payment volume analysis
        Map<String, Object> patterns = historicalData.getPatterns();
        
        if (patterns.containsKey("patterns")) {
            @SuppressWarnings("unchecked")
            List<String> patternList = (List<String>) patterns.get("patterns");
            
            double score = 0.0;
            
            // Check for known patterns
            if (patternList.contains("weekend_peaks")) {
                score += 0.3;
            }
            if (patternList.contains("month_end_spikes")) {
                score += 0.4;
            }
            if (patternList.contains("holiday_surges")) {
                score += 0.5;
            }
            
            // Check current time context
            LocalDateTime now = LocalDateTime.now();
            if (isWeekend(now)) {
                score += 0.2;
            }
            if (isMonthEnd(now)) {
                score += 0.3;
            }
            
            return Math.min(1.0, score);
        }
        
        return 0.2; // Default low probability
    }
    
    /**
     * Analyze processing delay pattern
     */
    private double analyzeProcessingDelayPattern(HistoricalData historicalData) {
        // Simulate processing delay analysis
        Integer incidents = (Integer) historicalData.getPatterns().get("incidents");
        
        if (incidents != null && incidents > 10) {
            return 0.6;
        } else if (incidents != null && incidents > 5) {
            return 0.4;
        }
        
        return 0.1;
    }
    
    /**
     * Analyze loan application pattern
     */
    private double analyzeLoanApplicationPattern(HistoricalData historicalData) {
        // Simulate loan application analysis
        List<String> services = historicalData.getServices();
        
        if (services.contains("loan_processing")) {
            LocalDateTime now = LocalDateTime.now();
            
            // Higher probability during business hours
            if (now.getHour() >= 9 && now.getHour() <= 17) {
                return 0.55;
            }
            
            return 0.35;
        }
        
        return 0.15;
    }
    
    /**
     * Analyze fraud pattern trends
     */
    private double analyzeFraudPatternTrends(HistoricalData historicalData) {
        // Simulate fraud pattern analysis
        List<String> services = historicalData.getServices();
        
        if (services.contains("fraud_detection")) {
            return 0.45;
        }
        
        return 0.1;
    }
    
    /**
     * Analyze system load pattern
     */
    private double analyzeSystemLoadPattern(HistoricalData historicalData) {
        // Simulate system load analysis
        Integer incidents = (Integer) historicalData.getPatterns().get("incidents");
        
        if (incidents != null && incidents > 12) {
            return 0.7;
        } else if (incidents != null && incidents > 8) {
            return 0.5;
        }
        
        return 0.2;
    }
    
    /**
     * Analyze compliance pattern
     */
    private double analyzeCompliancePattern(HistoricalData historicalData) {
        // Simulate compliance analysis
        LocalDateTime now = LocalDateTime.now();
        
        // Higher probability at end of quarter
        if (isQuarterEnd(now)) {
            return 0.8;
        } else if (isMonthEnd(now)) {
            return 0.6;
        }
        
        return 0.25;
    }
    
    /**
     * Calculate prediction time based on pattern type
     */
    private LocalDateTime calculatePredictionTime(HistoricalData historicalData, String patternType) {
        LocalDateTime now = LocalDateTime.now();
        
        return switch (patternType) {
            case "payment_peak" -> now.plusHours(2);
            case "processing_delay" -> now.plusMinutes(90);
            case "loan_peak" -> now.plusHours(4);
            case "fraud_spike" -> now.plusHours(3);
            case "system_load" -> now.plusMinutes(60);
            case "compliance_check" -> now.plusHours(8);
            default -> now.plusHours(2);
        };
    }
    
    /**
     * Determine severity based on probability
     */
    private String determineSeverity(double probability) {
        if (probability >= HIGH_PROBABILITY_THRESHOLD) {
            return "HIGH";
        } else if (probability >= MEDIUM_PROBABILITY_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Calculate impact score
     */
    private double calculateImpactScore(double probability) {
        return Math.min(10.0, probability * 10.0);
    }
    
    /**
     * Generate unique prediction ID
     */
    private String generatePredictionId(String type) {
        return type + "_" + System.currentTimeMillis();
    }
    
    /**
     * Check if current time is weekend
     */
    private boolean isWeekend(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
    
    /**
     * Check if current time is month end
     */
    private boolean isMonthEnd(LocalDateTime dateTime) {
        return dateTime.getDayOfMonth() >= 28;
    }
    
    /**
     * Check if current time is quarter end
     */
    private boolean isQuarterEnd(LocalDateTime dateTime) {
        int month = dateTime.getMonthValue();
        return (month == 3 || month == 6 || month == 9 || month == 12) && 
               dateTime.getDayOfMonth() >= 28;
    }
}