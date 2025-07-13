package com.bank.infrastructure.audit;

/**
 * Compliance Metric for Regulatory Reporting
 * 
 * Represents a measurable compliance indicator
 */
public class ComplianceMetric {
    
    private final String name;
    private final String unit;
    private double value;
    private double target;
    private String status;
    
    public ComplianceMetric(String name, String unit) {
        this.name = name;
        this.unit = unit;
        this.value = 0.0;
        this.target = 100.0; // Default target
        this.status = "UNKNOWN";
    }
    
    public void setValue(double value) {
        this.value = value;
        updateStatus();
    }
    
    public void setTarget(double target) {
        this.target = target;
        updateStatus();
    }
    
    private void updateStatus() {
        if (value >= target) {
            status = "COMPLIANT";
        } else if (value >= target * 0.9) {
            status = "WARNING";
        } else {
            status = "NON_COMPLIANT";
        }
    }
    
    // Getters
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public double getValue() { return value; }
    public double getTarget() { return target; }
    public String getStatus() { return status; }
}