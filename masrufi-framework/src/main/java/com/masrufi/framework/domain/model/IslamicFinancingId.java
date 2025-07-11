package com.masrufi.framework.domain.model;

import lombok.Value;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing Islamic Financing ID
 * 
 * This immutable value object encapsulates the identity of an Islamic
 * financing arrangement following Domain-Driven Design principles.
 * 
 * Design Principles:
 * - Immutable by design
 * - Validates ID format
 * - Type-safe ID representation
 * - Self-validating
 * 
 * @author MasruFi Development Team
 * @version 1.0.0
 */
@Value
public class IslamicFinancingId {
    
    String value;

    private IslamicFinancingId(String value) {
        this.value = Objects.requireNonNull(value, "Islamic Financing ID cannot be null");
        validateId(value);
    }

    /**
     * Create Islamic Financing ID from string value
     * 
     * @param value The ID value
     * @return Islamic Financing ID instance
     * @throws IllegalArgumentException if value is invalid
     */
    public static IslamicFinancingId of(String value) {
        return new IslamicFinancingId(value);
    }

    /**
     * Generate a new unique Islamic Financing ID
     * 
     * @param type The Islamic financing type
     * @return New Islamic Financing ID
     */
    public static IslamicFinancingId generate(IslamicFinancing.IslamicFinancingType type) {
        String prefix = type.name();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return new IslamicFinancingId(prefix + "-" + uuid);
    }

    /**
     * Generate a new unique Islamic Financing ID with timestamp
     * 
     * @param type The Islamic financing type
     * @return New Islamic Financing ID with timestamp
     */
    public static IslamicFinancingId generateWithTimestamp(IslamicFinancing.IslamicFinancingType type) {
        String prefix = type.name();
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return new IslamicFinancingId(prefix + "-" + timestamp + "-" + uuid);
    }

    /**
     * Get the Islamic financing type from the ID
     * 
     * @return The Islamic financing type
     */
    public IslamicFinancing.IslamicFinancingType getType() {
        String prefix = value.split("-")[0];
        return IslamicFinancing.IslamicFinancingType.valueOf(prefix);
    }

    /**
     * Check if this ID represents a specific Islamic financing type
     * 
     * @param type The type to check
     * @return true if ID represents the specified type
     */
    public boolean isOfType(IslamicFinancing.IslamicFinancingType type) {
        return value.startsWith(type.name() + "-");
    }

    private void validateId(String value) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Islamic Financing ID cannot be empty");
        }
        
        if (value.length() < 10) {
            throw new IllegalArgumentException("Islamic Financing ID too short: " + value);
        }
        
        if (value.length() > 50) {
            throw new IllegalArgumentException("Islamic Financing ID too long: " + value);
        }
        
        // Validate format: TYPE-[timestamp-]identifier
        if (!value.matches("^[A-Z_]+(-[0-9]+)?-[A-Za-z0-9]+$")) {
            throw new IllegalArgumentException("Invalid Islamic Financing ID format: " + value);
        }
        
        // Validate that the prefix is a valid Islamic financing type
        String prefix = value.split("-")[0];
        try {
            IslamicFinancing.IslamicFinancingType.valueOf(prefix);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Islamic financing type in ID: " + prefix);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}