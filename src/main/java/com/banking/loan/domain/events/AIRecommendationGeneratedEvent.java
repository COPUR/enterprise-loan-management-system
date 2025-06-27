package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.BaseDomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Domain event fired when AI generates a recommendation for a loan
 */
public class AIRecommendationGeneratedEvent extends BaseDomainEvent {
    
    private final String loanId;
    private final String recommendationType;
    private final BigDecimal recommendedAmount;
    private final BigDecimal recommendedRate;
    private final String riskAssessment;
    private final Map<String, Object> aiAnalysis;
    
    public AIRecommendationGeneratedEvent(String aggregateId, Long version, String triggeredBy, 
                                        String correlationId, String tenantId, EventMetadata metadata,
                                        String loanId, String recommendationType, 
                                        BigDecimal recommendedAmount, BigDecimal recommendedRate,
                                        String riskAssessment, Map<String, Object> aiAnalysis) {
        super(aggregateId, version, triggeredBy, correlationId, tenantId, metadata);
        this.loanId = loanId;
        this.recommendationType = recommendationType;
        this.recommendedAmount = recommendedAmount;
        this.recommendedRate = recommendedRate;
        this.riskAssessment = riskAssessment;
        this.aiAnalysis = aiAnalysis;
    }
    
    @Override
    public String getEventType() {
        return "AIRecommendationGenerated";
    }
    
    public String getLoanId() {
        return loanId;
    }
    
    public String getRecommendationType() {
        return recommendationType;
    }
    
    public BigDecimal getRecommendedAmount() {
        return recommendedAmount;
    }
    
    public BigDecimal getRecommendedRate() {
        return recommendedRate;
    }
    
    public String getRiskAssessment() {
        return riskAssessment;
    }
    
    public Map<String, Object> getAiAnalysis() {
        return aiAnalysis;
    }
}