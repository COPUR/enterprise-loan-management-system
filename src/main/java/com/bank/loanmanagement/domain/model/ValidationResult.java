package com.bank.loanmanagement.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain Model for Validation Results
 * Represents validation outcome with errors and warnings
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    
    private ValidationResult(Builder builder) {
        this.valid = builder.valid;
        this.errors = builder.errors;
        this.warnings = builder.warnings;
    }
    
    // Getters
    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean valid = true;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }
        
        public Builder addError(String error) {
            this.errors.add(error);
            return this;
        }
        
        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }
        
        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }
}