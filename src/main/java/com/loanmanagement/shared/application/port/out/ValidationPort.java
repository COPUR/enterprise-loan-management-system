package com.loanmanagement.shared.application.port.out;

import java.util.List;
import java.util.Set;

/**
 * Outbound Port for Validation
 * Abstracts validation framework from the application layer
 */
public interface ValidationPort {
    
    /**
     * Validate an object
     */
    <T> ValidationResult validate(T object);
    
    /**
     * Validate an object with specific validation groups
     */
    <T> ValidationResult validate(T object, Class<?>... groups);
    
    /**
     * Validate a property of an object
     */
    <T> ValidationResult validateProperty(T object, String propertyName);
    
    /**
     * Validation result containing violations
     */
    record ValidationResult(
            boolean valid,
            List<ValidationViolation> violations
    ) {
        public static ValidationResult valid() {
            return new ValidationResult(true, List.of());
        }
        
        public static ValidationResult invalid(List<ValidationViolation> violations) {
            return new ValidationResult(false, violations);
        }
        
        public boolean hasViolations() {
            return !violations.isEmpty();
        }
        
        public String getViolationMessages() {
            return violations.stream()
                    .map(ValidationViolation::message)
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
        }
    }
    
    /**
     * Individual validation violation
     */
    record ValidationViolation(
            String propertyPath,
            Object invalidValue,
            String message
    ) {}
}