package com.amanahfi.platform.shared.idempotence;

import lombok.Value;

import java.util.Objects;
import java.util.UUID;

/**
 * Idempotency Key Value Object for AmanahFi Platform
 * 
 * This value object represents a unique identifier used to ensure idempotent
 * operations across the Islamic finance platform. It prevents duplicate
 * financial transactions, double Sukuk mints, and broken Sharia audit trails.
 * 
 * Key Features:
 * - Immutable value object for thread safety
 * - Strong typing to prevent mixing with other identifiers
 * - Support for both UUID and string-based keys
 * - Validation to ensure non-null, non-empty keys
 * 
 * Usage Context:
 * - API Gateway idempotency (Open Finance, mobile apps)
 * - Command bus deduplication
 * - Event consumer replay protection
 * - Saga orchestrator compensation safety
 * - External integration duplicate prevention
 * 
 * Islamic Finance Considerations:
 * - Prevents duplicate Sharia-compliant transactions
 * - Ensures audit trail integrity for regulatory compliance
 * - Protects against duplicate Zakat calculations
 * - Maintains transaction purity for religious compliance
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
public class IdempotencyKey {

    String value;

    /**
     * Creates an idempotency key from a string value
     * 
     * @param value The string value for the key
     * @throws IllegalArgumentException if value is null or empty
     */
    public IdempotencyKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key cannot be null or empty");
        }
        this.value = value.trim();
    }

    /**
     * Creates an idempotency key from a UUID
     * 
     * @param uuid The UUID for the key
     * @throws IllegalArgumentException if uuid is null
     */
    public IdempotencyKey(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("Idempotency key UUID cannot be null");
        }
        this.value = uuid.toString();
    }

    /**
     * Generates a new random idempotency key
     * 
     * @return New idempotency key with random UUID
     */
    public static IdempotencyKey generate() {
        return new IdempotencyKey(UUID.randomUUID());
    }

    /**
     * Creates an idempotency key from a string value
     * 
     * @param value The string value
     * @return Idempotency key instance
     */
    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    /**
     * Creates an idempotency key from a UUID
     * 
     * @param uuid The UUID value
     * @return Idempotency key instance
     */
    public static IdempotencyKey of(UUID uuid) {
        return new IdempotencyKey(uuid);
    }

    /**
     * Returns the string representation of the key
     * 
     * @return The key value as string
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Checks equality based on the string value
     * 
     * @param other The object to compare with
     * @return true if the values are equal, false otherwise
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        IdempotencyKey that = (IdempotencyKey) other;
        return Objects.equals(value, that.value);
    }

    /**
     * Returns hash code based on the string value
     * 
     * @return Hash code of the value
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Validates the key format (basic validation)
     * 
     * @return true if the key format is valid
     */
    public boolean isValidFormat() {
        // Basic validation - can be extended for specific formats
        return value != null && 
               value.length() >= 8 && 
               value.length() <= 128 &&
               value.matches("^[a-zA-Z0-9\\-_]+$");
    }

    /**
     * Checks if the key looks like a UUID
     * 
     * @return true if the key is in UUID format
     */
    public boolean isUuidFormat() {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Gets the key as a UUID if it's in UUID format
     * 
     * @return UUID representation of the key
     * @throws IllegalStateException if key is not in UUID format
     */
    public UUID asUuid() {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Idempotency key is not in UUID format: " + value);
        }
    }
}