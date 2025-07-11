package com.amanahfi.platform.shared.idempotence;

import java.util.Optional;

/**
 * Port for storing and retrieving idempotency records
 * 
 * This interface defines the contract for idempotency storage,
 * supporting various backends (Redis, PostgreSQL, etc.) to ensure
 * exactly-once processing semantics across the AmanahFi Platform.
 * 
 * Implementation Requirements:
 * - Atomic store/retrieve operations
 * - TTL support for automatic cleanup
 * - High availability for financial operations
 * - ACID guarantees for critical paths
 * 
 * Performance Targets:
 * - Idempotency check latency: ≤ 25ms P95
 * - API replay success rate: ≥ 99.99%
 * - Duplicate financial side-effects: 0 per EOD reconciliation
 * 
 * Islamic Finance Considerations:
 * - Prevents duplicate Sharia-compliant transactions
 * - Maintains audit trail integrity for HSA compliance
 * - Ensures single economic effect per Islamic contract
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
public interface IdempotencyStore {

    /**
     * Stores an idempotency record atomically
     * 
     * This operation must be atomic to prevent race conditions.
     * If a record with the same key already exists, this operation
     * should fail or return the existing record based on implementation.
     * 
     * @param record The idempotency record to store
     * @return true if stored successfully, false if key already exists
     * @throws IdempotencyStoreException if storage operation fails
     */
    boolean store(IdempotencyRecord record);

    /**
     * Retrieves an idempotency record by key
     * 
     * @param key The idempotency key to lookup
     * @return Optional containing the record if found and not expired
     * @throws IdempotencyStoreException if retrieval operation fails
     */
    Optional<IdempotencyRecord> retrieve(IdempotencyKey key);

    /**
     * Stores a record only if the key doesn't exist (atomic put-if-absent)
     * 
     * This is the preferred method for idempotency enforcement as it
     * provides atomic semantics for the check-and-store operation.
     * 
     * @param record The idempotency record to store
     * @return Optional containing existing record if key exists, empty if stored
     * @throws IdempotencyStoreException if operation fails
     */
    Optional<IdempotencyRecord> storeIfAbsent(IdempotencyRecord record);

    /**
     * Removes an expired idempotency record
     * 
     * @param key The idempotency key to remove
     * @return true if record was removed, false if not found
     * @throws IdempotencyStoreException if removal operation fails
     */
    boolean remove(IdempotencyKey key);

    /**
     * Checks if a key exists in the store
     * 
     * @param key The idempotency key to check
     * @return true if key exists and record is not expired
     * @throws IdempotencyStoreException if check operation fails
     */
    boolean exists(IdempotencyKey key);

    /**
     * Updates the TTL of an existing record
     * 
     * @param key The idempotency key
     * @param ttlSeconds New TTL in seconds
     * @return true if TTL was updated, false if key not found
     * @throws IdempotencyStoreException if update operation fails
     */
    boolean updateTtl(IdempotencyKey key, long ttlSeconds);

    /**
     * Removes all expired records (cleanup operation)
     * 
     * This method should be called by a background job to prevent
     * unbounded growth of the idempotency store.
     * 
     * @return Number of records cleaned up
     * @throws IdempotencyStoreException if cleanup operation fails
     */
    long cleanupExpiredRecords();

    /**
     * Gets statistics about the idempotency store
     * 
     * @return Store statistics for monitoring
     * @throws IdempotencyStoreException if statistics retrieval fails
     */
    IdempotencyStoreStats getStats();

    /**
     * Performs a health check on the store
     * 
     * @return true if store is healthy and responsive
     */
    boolean isHealthy();

    /**
     * Exception thrown when idempotency store operations fail
     */
    class IdempotencyStoreException extends RuntimeException {
        
        public IdempotencyStoreException(String message) {
            super(message);
        }
        
        public IdempotencyStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Statistics about the idempotency store
     */
    record IdempotencyStoreStats(
        long totalRecords,
        long expiredRecords,
        long recentHits,
        long recentMisses,
        double hitRatio,
        long averageLatencyMs,
        long p95LatencyMs,
        long p99LatencyMs
    ) {
        
        /**
         * Checks if performance is within acceptable limits
         * 
         * @return true if performance metrics are acceptable
         */
        public boolean isPerformanceAcceptable() {
            return p95LatencyMs <= 25 && // Target: ≤ 25ms P95
                   hitRatio >= 0.80;      // Reasonable hit ratio
        }

        /**
         * Checks if the store needs cleanup
         * 
         * @return true if expired records ratio is high
         */
        public boolean needsCleanup() {
            if (totalRecords == 0) return false;
            double expiredRatio = (double) expiredRecords / totalRecords;
            return expiredRatio > 0.20; // More than 20% expired
        }
    }
}