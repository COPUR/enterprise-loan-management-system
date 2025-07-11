package com.amanahfi.platform.shared.idempotence;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Idempotency Record for storing request/response pairs
 * 
 * This value object represents a stored idempotency record that tracks
 * processed requests to ensure exactly-once processing semantics.
 * 
 * Mathematical Definition: f(f(x)) = f(x)
 * Software Definition: Processing a request 1, 2, or n times has the same net effect
 * 
 * Storage Strategy:
 * - Store (idempotency-key, hash(request-body), response, ttl)
 * - On duplicate: return cached response if hash matches
 * - TTL: 24h for payments, varies by product type
 * 
 * Islamic Finance Context:
 * - Prevents duplicate debits in Islamic accounts
 * - Avoids double Sukuk minting
 * - Maintains Sharia audit trail integrity
 * - Ensures single economic effect per intention
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
@Builder
public class IdempotencyRecord {

    /**
     * The idempotency key for this record
     */
    IdempotencyKey key;

    /**
     * Hash of the original request body for validation
     */
    String requestBodyHash;

    /**
     * The cached response to return on duplicate requests
     */
    String cachedResponse;

    /**
     * HTTP status code of the original response
     */
    int statusCode;

    /**
     * Content type of the original response
     */
    String contentType;

    /**
     * When this record was created
     */
    Instant createdAt;

    /**
     * When this record expires (TTL)
     */
    Instant expiresAt;

    /**
     * Additional metadata for the record
     */
    String metadata;

    /**
     * The operation type that was performed
     */
    OperationType operationType;

    /**
     * Whether this record represents a successful operation
     */
    boolean successful;

    /**
     * Creates a new idempotency record for a successful operation
     * 
     * @param key The idempotency key
     * @param requestBodyHash Hash of the request body
     * @param cachedResponse The response to cache
     * @param statusCode HTTP status code
     * @param contentType Response content type
     * @param ttlSeconds Time to live in seconds
     * @param operationType Type of operation
     * @return New idempotency record
     */
    public static IdempotencyRecord success(
            IdempotencyKey key,
            String requestBodyHash,
            String cachedResponse,
            int statusCode,
            String contentType,
            long ttlSeconds,
            OperationType operationType) {
        
        Instant now = Instant.now();
        return IdempotencyRecord.builder()
                .key(key)
                .requestBodyHash(requestBodyHash)
                .cachedResponse(cachedResponse)
                .statusCode(statusCode)
                .contentType(contentType)
                .createdAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .operationType(operationType)
                .successful(true)
                .build();
    }

    /**
     * Creates a new idempotency record for a failed operation
     * 
     * @param key The idempotency key
     * @param requestBodyHash Hash of the request body
     * @param errorResponse The error response to cache
     * @param statusCode HTTP error status code
     * @param contentType Response content type
     * @param ttlSeconds Time to live in seconds
     * @param operationType Type of operation
     * @return New idempotency record
     */
    public static IdempotencyRecord failure(
            IdempotencyKey key,
            String requestBodyHash,
            String errorResponse,
            int statusCode,
            String contentType,
            long ttlSeconds,
            OperationType operationType) {
        
        Instant now = Instant.now();
        return IdempotencyRecord.builder()
                .key(key)
                .requestBodyHash(requestBodyHash)
                .cachedResponse(errorResponse)
                .statusCode(statusCode)
                .contentType(contentType)
                .createdAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .operationType(operationType)
                .successful(false)
                .build();
    }

    /**
     * Checks if this record has expired
     * 
     * @return true if the record has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if this record is still valid (not expired)
     * 
     * @return true if the record is valid
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Validates that the request body hash matches
     * 
     * @param requestBodyHash The hash to compare
     * @return true if hashes match
     */
    public boolean matchesRequestHash(String requestBodyHash) {
        return this.requestBodyHash != null && this.requestBodyHash.equals(requestBodyHash);
    }

    /**
     * Gets the age of this record in seconds
     * 
     * @return Age in seconds
     */
    public long getAgeInSeconds() {
        return java.time.Duration.between(createdAt, Instant.now()).getSeconds();
    }

    /**
     * Gets the remaining TTL in seconds
     * 
     * @return Remaining TTL, or 0 if expired
     */
    public long getRemainingTtlSeconds() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(Instant.now(), expiresAt).getSeconds();
    }

    /**
     * Types of operations that can be made idempotent
     */
    public enum OperationType {
        
        // Payment operations
        PAYMENT_INITIATION("Payment Initiation", 24 * 60 * 60), // 24 hours
        PAYMENT_CONFIRMATION("Payment Confirmation", 24 * 60 * 60),
        PAYMENT_CANCELLATION("Payment Cancellation", 24 * 60 * 60),
        
        // Islamic Finance operations  
        MURABAHA_CREATION("Murabaha Creation", 12 * 60 * 60), // 12 hours
        MUSHARAKAH_CREATION("Musharakah Creation", 12 * 60 * 60),
        IJARAH_CREATION("Ijarah Creation", 12 * 60 * 60),
        QARD_HASSAN_CREATION("Qard Hassan Creation", 12 * 60 * 60),
        
        // Product lifecycle operations
        PRODUCT_APPROVAL("Product Approval", 6 * 60 * 60), // 6 hours
        PRODUCT_ACTIVATION("Product Activation", 6 * 60 * 60),
        PRODUCT_DISBURSEMENT("Product Disbursement", 24 * 60 * 60),
        
        // CBDC operations
        CBDC_MINT("CBDC Mint", 24 * 60 * 60), // 24 hours
        CBDC_TRANSFER("CBDC Transfer", 24 * 60 * 60),
        CBDC_BURN("CBDC Burn", 24 * 60 * 60),
        
        // Regulatory operations
        REGULATORY_REPORT("Regulatory Report", 12 * 60 * 60), // 12 hours
        SHARIA_COMPLIANCE_CHECK("Sharia Compliance Check", 6 * 60 * 60),
        
        // Account operations
        ACCOUNT_CREATION("Account Creation", 12 * 60 * 60),
        ACCOUNT_UPDATE("Account Update", 6 * 60 * 60),
        ACCOUNT_CLOSURE("Account Closure", 24 * 60 * 60),
        
        // Generic operations
        COMMAND_EXECUTION("Command Execution", 6 * 60 * 60), // 6 hours
        EVENT_PROCESSING("Event Processing", 2 * 60 * 60), // 2 hours
        API_CALL("API Call", 1 * 60 * 60); // 1 hour

        private final String description;
        private final long defaultTtlSeconds;

        OperationType(String description, long defaultTtlSeconds) {
            this.description = description;
            this.defaultTtlSeconds = defaultTtlSeconds;
        }

        public String getDescription() {
            return description;
        }

        public long getDefaultTtlSeconds() {
            return defaultTtlSeconds;
        }

        /**
         * Checks if this operation type is financially sensitive
         * 
         * @return true if operation involves financial state changes
         */
        public boolean isFinanciallySensitive() {
            return this == PAYMENT_INITIATION ||
                   this == PAYMENT_CONFIRMATION ||
                   this == PRODUCT_DISBURSEMENT ||
                   this == CBDC_MINT ||
                   this == CBDC_TRANSFER ||
                   this == CBDC_BURN;
        }

        /**
         * Checks if this operation type requires Sharia compliance
         * 
         * @return true if operation must comply with Islamic principles
         */
        public boolean requiresShariaCompliance() {
            return this == MURABAHA_CREATION ||
                   this == MUSHARAKAH_CREATION ||
                   this == IJARAH_CREATION ||
                   this == QARD_HASSAN_CREATION ||
                   this == SHARIA_COMPLIANCE_CHECK;
        }

        /**
         * Checks if this operation type requires regulatory reporting
         * 
         * @return true if operation must be reported to regulators
         */
        public boolean requiresRegulatoryReporting() {
            return isFinanciallySensitive() ||
                   this == REGULATORY_REPORT ||
                   this == ACCOUNT_CREATION ||
                   this == ACCOUNT_CLOSURE;
        }
    }
}