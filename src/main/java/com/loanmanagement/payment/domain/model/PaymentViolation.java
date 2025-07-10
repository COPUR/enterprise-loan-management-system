package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

/**
 * Value object representing a payment validation violation.
 */
@Value
@Builder
@With
public class PaymentViolation {
    
    String field;
    String message;
    String code;
    Object rejectedValue;
    ViolationSeverity severity;

    public enum ViolationSeverity {
        ERROR, WARNING, INFO
    }

    public static class PaymentViolationBuilder {
        public PaymentViolation build() {
            if (field == null || field.trim().isEmpty()) {
                throw new IllegalArgumentException("Field is required for violation");
            }
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Message is required for violation");
            }
            if (severity == null) {
                this.severity = ViolationSeverity.ERROR;
            }
            
            return new PaymentViolation(field, message, code, rejectedValue, severity);
        }
    }

    public boolean isError() {
        return severity == ViolationSeverity.ERROR;
    }

    public boolean isWarning() {
        return severity == ViolationSeverity.WARNING;
    }

    public boolean isInfo() {
        return severity == ViolationSeverity.INFO;
    }

    public static PaymentViolation error(String field, String message) {
        return PaymentViolation.builder()
                .field(field)
                .message(message)
                .severity(ViolationSeverity.ERROR)
                .build();
    }

    public static PaymentViolation warning(String field, String message) {
        return PaymentViolation.builder()
                .field(field)
                .message(message)
                .severity(ViolationSeverity.WARNING)
                .build();
    }

    public static PaymentViolation info(String field, String message) {
        return PaymentViolation.builder()
                .field(field)
                .message(message)
                .severity(ViolationSeverity.INFO)
                .build();
    }
}