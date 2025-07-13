package com.bank.shared.domain.exception;

/**
 * Exception thrown when a compliance violation is detected
 */
public class ComplianceViolationException extends BusinessException {
    
    private final String violationType;
    private final String complianceRule;
    private final String entityId;
    private final String severityLevel;
    
    public ComplianceViolationException(String violationType, String complianceRule, String entityId, String severityLevel) {
        super(String.format("Compliance violation detected - Type: %s, Rule: %s, Entity: %s, Severity: %s", 
            violationType, complianceRule, entityId, severityLevel));
        this.violationType = violationType;
        this.complianceRule = complianceRule;
        this.entityId = entityId;
        this.severityLevel = severityLevel;
    }
    
    public ComplianceViolationException(String message) {
        super(message);
        this.violationType = null;
        this.complianceRule = null;
        this.entityId = null;
        this.severityLevel = null;
    }
    
    public String getViolationType() {
        return violationType;
    }
    
    public String getComplianceRule() {
        return complianceRule;
    }
    
    public String getEntityId() {
        return entityId;
    }
    
    public String getSeverityLevel() {
        return severityLevel;
    }
}