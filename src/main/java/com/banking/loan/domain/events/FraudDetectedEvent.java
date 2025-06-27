package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.BaseDomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.util.List;

/**
 * Domain event fired when fraud is detected in a loan
 */
public class FraudDetectedEvent extends BaseDomainEvent {
    
    private final String customerId;
    private final String riskLevel;
    private final List<String> fraudIndicators;
    
    public FraudDetectedEvent(String loanId, Long version, String flaggedBy, 
                            String correlationId, String tenantId, EventMetadata metadata,
                            String customerId, String riskLevel, List<String> fraudIndicators) {
        super(loanId, version, flaggedBy, correlationId, tenantId, metadata);
        this.customerId = customerId;
        this.riskLevel = riskLevel;
        this.fraudIndicators = fraudIndicators;
    }
    
    @Override
    public String getEventType() {
        return "FraudDetected";
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public String getRiskLevel() {
        return riskLevel;
    }
    
    public List<String> getFraudIndicators() {
        return fraudIndicators;
    }
}