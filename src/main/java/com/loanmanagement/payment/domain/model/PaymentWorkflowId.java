package com.loanmanagement.payment.domain.model;

import lombok.Value;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Value object representing a payment workflow identifier.
 * Immutable and ensures valid workflow ID format.
 */
@Value
public class PaymentWorkflowId {
    
    String value;
    
    /**
     * Private constructor to enforce factory method usage.
     */
    private PaymentWorkflowId(String value) {
        Objects.requireNonNull(value, "Workflow ID value cannot be null");
        
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Workflow ID cannot be empty");
        }
        
        if (!isValidFormat(value)) {
            throw new IllegalArgumentException("Invalid workflow ID format: " + value);
        }
        
        this.value = value;
    }
    
    /**
     * Creates a workflow ID from a string value.
     */
    public static PaymentWorkflowId of(String value) {
        return new PaymentWorkflowId(value);
    }
    
    /**
     * Generates a new unique workflow ID.
     */
    public static PaymentWorkflowId generate() {
        return new PaymentWorkflowId(generateId());
    }
    
    /**
     * Creates a workflow ID for a specific type and reference.
     */
    public static PaymentWorkflowId forTypeAndReference(
            PaymentWorkflowType type,
            String reference) {
        
        String id = String.format("WF-%s-%s-%s",
                type.name(),
                reference,
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        
        return new PaymentWorkflowId(id);
    }
    
    /**
     * Generates a unique workflow ID.
     */
    private static String generateId() {
        return "WF-" + System.currentTimeMillis() + "-" + 
               UUID.randomUUID().toString().toUpperCase();
    }
    
    /**
     * Validates the workflow ID format.
     * Format: WF-<type>-<reference>-<unique> or WF-<timestamp>-<uuid>
     */
    private static boolean isValidFormat(String value) {
        if (!value.startsWith("WF-")) {
            return false;
        }
        
        String[] parts = value.split("-");
        return parts.length >= 3;
    }
    
    /**
     * Extracts the workflow type from the ID if possible.
     */
    public java.util.Optional<PaymentWorkflowType> extractType() {
        if (!value.contains("-")) {
            return Optional.empty();
        }
        
        String[] parts = value.split("-");
        if (parts.length >= 3) {
            try {
                return Optional.of(PaymentWorkflowType.valueOf(parts[1]));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets a shortened version of the ID for display.
     */
    public String toShortString() {
        if (value.length() <= 20) {
            return value;
        }
        
        String[] parts = value.split("-");
        if (parts.length >= 3) {
            return parts[0] + "-" + parts[1] + "-..." + 
                   value.substring(value.length() - 8);
        }
        
        return value.substring(0, 17) + "...";
    }
    
    @Override
    public String toString() {
        return value;
    }
}