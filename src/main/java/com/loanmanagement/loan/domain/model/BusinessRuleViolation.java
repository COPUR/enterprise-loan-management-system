package com.loanmanagement.loan.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Business Rule Violation Value Object
 * Represents a violation of a business rule during loan eligibility assessment
 */
@Value
@Builder(toBuilder = true)
public class BusinessRuleViolation {
    
    BusinessRuleType ruleType;
    String description;
    ViolationSeverity severity;
    Object actualValue;
    Object requiredValue;
    LocalDateTime timestamp;
    String ruleCode;
    String remediation;
    
    public BusinessRuleViolation(BusinessRuleType ruleType, String description, ViolationSeverity severity,
                                Object actualValue, Object requiredValue, LocalDateTime timestamp,
                                String ruleCode, String remediation) {
        
        // Validation
        Objects.requireNonNull(ruleType, "Rule type cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(severity, "Severity cannot be null");
        
        if (description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        
        this.ruleType = ruleType;
        this.description = description.trim();
        this.severity = severity;
        this.actualValue = actualValue;
        this.requiredValue = requiredValue;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.ruleCode = ruleCode != null ? ruleCode : ruleType.getCode();
        this.remediation = remediation;
    }
    
    /**
     * Create a violation with current timestamp
     */
    public static BusinessRuleViolation of(BusinessRuleType ruleType, String description, ViolationSeverity severity) {
        return BusinessRuleViolation.builder()
                .ruleType(ruleType)
                .description(description)
                .severity(severity)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create a violation with actual and required values
     */
    public static BusinessRuleViolation withValues(BusinessRuleType ruleType, String description, 
                                                  ViolationSeverity severity, Object actualValue, Object requiredValue) {
        return BusinessRuleViolation.builder()
                .ruleType(ruleType)
                .description(description)
                .severity(severity)
                .actualValue(actualValue)
                .requiredValue(requiredValue)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create an error violation
     */
    public static BusinessRuleViolation error(BusinessRuleType ruleType, String description) {
        return of(ruleType, description, ViolationSeverity.ERROR);
    }
    
    /**
     * Create a warning violation
     */
    public static BusinessRuleViolation warning(BusinessRuleType ruleType, String description) {
        return of(ruleType, description, ViolationSeverity.WARNING);
    }
    
    /**
     * Create an info violation
     */
    public static BusinessRuleViolation info(BusinessRuleType ruleType, String description) {
        return of(ruleType, description, ViolationSeverity.INFO);
    }
    
    /**
     * Check if this is a critical violation
     */
    public boolean isCritical() {
        return severity == ViolationSeverity.ERROR;
    }
    
    /**
     * Check if this is a warning violation
     */
    public boolean isWarning() {
        return severity == ViolationSeverity.WARNING;
    }
    
    /**
     * Check if this is an informational violation
     */
    public boolean isInfo() {
        return severity == ViolationSeverity.INFO;
    }
    
    /**
     * Check if this violation has actual and required values
     */
    public boolean hasComparisonValues() {
        return actualValue != null && requiredValue != null;
    }
    
    /**
     * Get formatted violation message
     */
    public String getFormattedMessage() {
        StringBuilder message = new StringBuilder();
        message.append(String.format("[%s] %s: %s", 
                severity.name(), ruleType.getDisplayName(), description));
        
        if (hasComparisonValues()) {
            message.append(String.format(" (Actual: %s, Required: %s)", actualValue, requiredValue));
        }
        
        return message.toString();
    }
    
    /**
     * Get short description suitable for UI display
     */
    public String getShortDescription() {
        if (description.length() <= 100) {
            return description;
        }
        return description.substring(0, 97) + "...";
    }
    
    /**
     * Check if this violation is related to a specific rule category
     */
    public boolean isRuleCategory(String category) {
        return ruleType.getCategory().equalsIgnoreCase(category);
    }
    
    /**
     * Get violation with remediation advice
     */
    public BusinessRuleViolation withRemediation(String remediation) {
        return this.toBuilder()
                .remediation(remediation)
                .build();
    }
    
    /**
     * Get violation priority for sorting
     */
    public int getPriority() {
        return switch (severity) {
            case ERROR -> 1;
            case WARNING -> 2;
            case INFO -> 3;
        };
    }
}