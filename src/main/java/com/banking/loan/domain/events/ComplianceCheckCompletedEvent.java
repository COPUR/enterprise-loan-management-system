package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.BaseDomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.util.List;

/**
 * Domain event fired when compliance check is completed
 */
public class ComplianceCheckCompletedEvent extends BaseDomainEvent {
    
    private final String loanId;
    private final String checkId;
    private final String complianceStatus;
    private final List<String> violations;
    private final List<String> warnings;
    private final String recommendation;
    
    public ComplianceCheckCompletedEvent(String aggregateId, Long version, String triggeredBy, 
                                       String correlationId, String tenantId, EventMetadata metadata,
                                       String loanId, String checkId, String complianceStatus,
                                       List<String> violations, List<String> warnings, String recommendation) {
        super(aggregateId, version, triggeredBy, correlationId, tenantId, metadata);
        this.loanId = loanId;
        this.checkId = checkId;
        this.complianceStatus = complianceStatus;
        this.violations = violations;
        this.warnings = warnings;
        this.recommendation = recommendation;
    }
    
    @Override
    public String getEventType() {
        return "ComplianceCheckCompleted";
    }
    
    public String getLoanId() {
        return loanId;
    }
    
    public String getCheckId() {
        return checkId;
    }
    
    public String getComplianceStatus() {
        return complianceStatus;
    }
    
    public List<String> getViolations() {
        return violations;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public String getRecommendation() {
        return recommendation;
    }
}