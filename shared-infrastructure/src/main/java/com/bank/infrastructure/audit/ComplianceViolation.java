package com.bank.infrastructure.audit;

import java.time.LocalDateTime;

/**
 * Compliance Violation Record
 * 
 * Represents a violation of regulatory compliance requirements
 */
public class ComplianceViolation {
    
    private final String code;
    private final String description;
    private final Severity severity;
    private final LocalDateTime detectedAt;
    private final String regulation;
    
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    public ComplianceViolation(String code, String description, Severity severity) {
        this.code = code;
        this.description = description;
        this.severity = severity;
        this.detectedAt = LocalDateTime.now();
        this.regulation = extractRegulationFromCode(code);
    }
    
    private String extractRegulationFromCode(String code) {
        if (code.startsWith("SOX_")) return "SOX";
        if (code.startsWith("PCI_")) return "PCI-DSS";
        if (code.startsWith("GDPR_")) return "GDPR";
        if (code.startsWith("BASEL_")) return "Basel III";
        if (code.startsWith("AML_")) return "AML";
        if (code.startsWith("FAPI_")) return "FAPI2";
        return "GENERAL";
    }
    
    // Getters
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public Severity getSeverity() { return severity; }
    public LocalDateTime getDetectedAt() { return detectedAt; }
    public String getRegulation() { return regulation; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s (Severity: %s)", 
                           regulation, code, description, severity);
    }
}