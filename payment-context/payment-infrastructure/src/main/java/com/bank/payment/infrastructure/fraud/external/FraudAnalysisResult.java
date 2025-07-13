package com.bank.payment.infrastructure.fraud.external;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprehensive fraud analysis result combining multiple sources
 */
@Data
@Builder
public class FraudAnalysisResult {
    
    private String paymentId;
    private String customerId;
    private LocalDateTime timestamp;
    
    // External service results
    private Integer externalRiskScore;
    private String externalRiskLevel;
    private List<String> externalReasons;
    private Boolean isBlockedByExternal;
    
    // Internal analysis results
    private Integer internalRiskScore;
    private String internalRiskLevel;
    private List<String> internalReasons;
    private Boolean isBlockedByInternal;
    
    // Combined results
    private Integer combinedRiskScore;
    private String finalRecommendation;
    private String primaryReason;
    
    // Analysis metadata
    private Boolean hasError;
    private String errorMessage;
    private Long processingTimeMs;
    private String analysisVersion;
}