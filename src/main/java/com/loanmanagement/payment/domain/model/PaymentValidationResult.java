package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Value object representing payment validation result.
 */
@Value
@Builder
@With
public class PaymentValidationResult {
    
    boolean valid;
    List<PaymentViolation> violations;
    Map<String, String> metadata;

    public static class PaymentValidationResultBuilder {
        public PaymentValidationResult build() {
            if (violations == null) {
                this.violations = List.of();
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            boolean hasErrors = violations.stream().anyMatch(PaymentViolation::isError);
            if (valid && hasErrors) {
                throw new IllegalStateException("Cannot be valid when there are error violations");
            }
            
            return new PaymentValidationResult(valid, violations, metadata);
        }
    }

    public boolean hasViolations() {
        return violations != null && !violations.isEmpty();
    }

    public boolean hasErrors() {
        return violations != null && violations.stream().anyMatch(PaymentViolation::isError);
    }

    public boolean hasWarnings() {
        return violations != null && violations.stream().anyMatch(PaymentViolation::isWarning);
    }

    public List<PaymentViolation> getErrors() {
        return violations != null ? 
                violations.stream()
                    .filter(PaymentViolation::isError)
                    .collect(Collectors.toList()) : 
                List.of();
    }

    public List<PaymentViolation> getWarnings() {
        return violations != null ? 
                violations.stream()
                    .filter(PaymentViolation::isWarning)
                    .collect(Collectors.toList()) : 
                List.of();
    }

    public static PaymentValidationResult success() {
        return PaymentValidationResult.builder()
                .valid(true)
                .violations(List.of())
                .build();
    }

    public static PaymentValidationResult failure(List<PaymentViolation> violations) {
        return PaymentValidationResult.builder()
                .valid(false)
                .violations(violations)
                .build();
    }

    public static PaymentValidationResult failure(PaymentViolation violation) {
        return PaymentValidationResult.builder()
                .valid(false)
                .violations(List.of(violation))
                .build();
    }
}