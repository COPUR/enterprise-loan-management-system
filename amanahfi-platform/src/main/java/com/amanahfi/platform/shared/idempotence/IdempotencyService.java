package com.amanahfi.platform.shared.idempotence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Service for managing idempotency across the AmanahFi Platform
 * 
 * This service provides comprehensive idempotency management to ensure
 * exactly-once processing semantics for all financial operations.
 * 
 * Mathematical Definition: f(f(x)) = f(x)
 * Software Definition: Processing a request 1, 2, or n times has the same net effect
 * 
 * Key Features:
 * - Request body hash validation
 * - TTL-based cleanup
 * - Performance monitoring
 * - Islamic finance compliance
 * 
 * Design Guardrails:
 * 1. Every public side-effecting API requires idempotency key
 * 2. Hash validation prevents malicious payload changes
 * 3. TTL prevents unbounded growth (24h for payments)
 * 4. Atomic operations prevent race conditions
 * 
 * Performance Targets:
 * - Additional latency: ≤ 25ms P95
 * - API replay success: ≥ 99.99%
 * - Duplicate side-effects: 0 per EOD reconciliation
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyStore idempotencyStore;

    /**
     * Processes a request with idempotency protection
     * 
     * This method implements the core idempotency pattern:
     * 1. Check if key exists with matching body hash
     * 2. If exists, return cached response
     * 3. If not, execute operation and cache result
     * 
     * @param key The idempotency key
     * @param requestBody The request body to hash
     * @param operation The operation to execute if not cached
     * @param operationType The type of operation
     * @return IdempotentResult containing response and cache status
     */
    public <T> IdempotentResult<T> processIdempotently(
            IdempotencyKey key,
            String requestBody,
            IdempotentOperation<T> operation,
            IdempotencyRecord.OperationType operationType) {

        log.debug("Processing idempotent operation with key: {} type: {}", key, operationType);

        // Calculate request body hash for validation
        String requestBodyHash = calculateHash(requestBody);

        // Check for existing record
        Optional<IdempotencyRecord> existingRecord = idempotencyStore.retrieve(key);
        
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            
            // Validate request body hash matches
            if (!record.matchesRequestHash(requestBodyHash)) {
                log.warn("Idempotency key {} reused with different request body", key);
                throw new IdempotencyViolationException(
                    "Idempotency key reused with different request body: " + key
                );
            }

            // Return cached response
            log.debug("Returning cached response for idempotency key: {}", key);
            return IdempotentResult.fromCache(
                record.getCachedResponse(),
                record.getStatusCode(),
                record.getContentType()
            );
        }

        // Execute the operation
        try {
            log.debug("Executing new operation for idempotency key: {}", key);
            IdempotentOperationResult<T> result = operation.execute();

            // Store the result for future requests
            IdempotencyRecord record = IdempotencyRecord.success(
                key,
                requestBodyHash,
                result.getResponseBody(),
                result.getStatusCode(),
                result.getContentType(),
                operationType.getDefaultTtlSeconds(),
                operationType
            );

            // Attempt to store (atomic operation)
            Optional<IdempotencyRecord> conflictRecord = idempotencyStore.storeIfAbsent(record);
            if (conflictRecord.isPresent()) {
                // Race condition: another thread stored a record while we were executing
                log.info("Race condition detected for key: {}, returning conflicting result", key);
                IdempotencyRecord conflict = conflictRecord.get();
                
                if (!conflict.matchesRequestHash(requestBodyHash)) {
                    throw new IdempotencyViolationException(
                        "Concurrent idempotency key usage with different request body: " + key
                    );
                }
                
                return IdempotentResult.fromCache(
                    conflict.getCachedResponse(),
                    conflict.getStatusCode(),
                    conflict.getContentType()
                );
            }

            log.debug("Successfully stored idempotency record for key: {}", key);
            return IdempotentResult.fromExecution(result.getResult());

        } catch (Exception e) {
            log.error("Operation failed for idempotency key: {}", key, e);
            
            // Store error response to prevent retries of failed operations
            if (shouldCacheError(e)) {
                IdempotencyRecord errorRecord = IdempotencyRecord.failure(
                    key,
                    requestBodyHash,
                    e.getMessage(),
                    500, // Internal Server Error
                    "application/json",
                    operationType.getDefaultTtlSeconds() / 4, // Shorter TTL for errors
                    operationType
                );
                
                idempotencyStore.storeIfAbsent(errorRecord);
            }
            
            throw e;
        }
    }

    /**
     * Validates an idempotency key format and generates if needed
     * 
     * @param providedKey Optional provided key
     * @param operationType Type of operation
     * @return Valid idempotency key
     */
    public IdempotencyKey validateOrGenerateKey(
            Optional<IdempotencyKey> providedKey,
            IdempotencyRecord.OperationType operationType) {

        if (providedKey.isPresent()) {
            IdempotencyKey key = providedKey.get();
            if (!key.isValidFormat()) {
                throw new InvalidIdempotencyKeyException(
                    "Invalid idempotency key format: " + key.getValue()
                );
            }
            return key;
        }

        // Generate key for operations that require one
        if (operationType.isFinanciallySensitive()) {
            throw new MissingIdempotencyKeyException(
                "Idempotency key required for financially sensitive operation: " + operationType
            );
        }

        return IdempotencyKey.generate();
    }

    /**
     * Cleans up expired idempotency records
     * 
     * This method should be called by a scheduled job to prevent
     * unbounded growth of the idempotency store.
     * 
     * @return Number of records cleaned up
     */
    public long cleanupExpiredRecords() {
        log.info("Starting idempotency store cleanup");
        
        try {
            long cleanedCount = idempotencyStore.cleanupExpiredRecords();
            log.info("Cleaned up {} expired idempotency records", cleanedCount);
            return cleanedCount;
        } catch (Exception e) {
            log.error("Failed to cleanup expired idempotency records", e);
            throw e;
        }
    }

    /**
     * Gets performance statistics for monitoring
     * 
     * @return Idempotency store statistics
     */
    public IdempotencyStore.IdempotencyStoreStats getPerformanceStats() {
        return idempotencyStore.getStats();
    }

    /**
     * Performs health check on idempotency infrastructure
     * 
     * @return true if healthy
     */
    public boolean isHealthy() {
        try {
            boolean storeHealthy = idempotencyStore.isHealthy();
            IdempotencyStore.IdempotencyStoreStats stats = getPerformanceStats();
            boolean performanceHealthy = stats.isPerformanceAcceptable();
            
            return storeHealthy && performanceHealthy;
        } catch (Exception e) {
            log.error("Health check failed for idempotency service", e);
            return false;
        }
    }

    /**
     * Calculates SHA-256 hash of request body for validation
     * 
     * @param requestBody The request body to hash
     * @return Hex string representation of hash
     */
    private String calculateHash(String requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(requestBody.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Determines if an error should be cached to prevent retries
     * 
     * @param error The error that occurred
     * @return true if error should be cached
     */
    private boolean shouldCacheError(Exception error) {
        // Cache validation errors and business rule violations
        // Don't cache infrastructure failures that might be transient
        return error instanceof IllegalArgumentException ||
               error instanceof IdempotencyViolationException ||
               error.getClass().getSimpleName().contains("Validation") ||
               error.getClass().getSimpleName().contains("Business");
    }

    // Supporting interfaces and classes

    /**
     * Functional interface for idempotent operations
     */
    @FunctionalInterface
    public interface IdempotentOperation<T> {
        IdempotentOperationResult<T> execute() throws Exception;
    }

    /**
     * Result of an idempotent operation
     */
    public static class IdempotentOperationResult<T> {
        private final T result;
        private final String responseBody;
        private final int statusCode;
        private final String contentType;

        public IdempotentOperationResult(T result, String responseBody, int statusCode, String contentType) {
            this.result = result;
            this.responseBody = responseBody;
            this.statusCode = statusCode;
            this.contentType = contentType;
        }

        public static <T> IdempotentOperationResult<T> success(T result, String responseBody) {
            return new IdempotentOperationResult<>(result, responseBody, 200, "application/json");
        }

        public T getResult() { return result; }
        public String getResponseBody() { return responseBody; }
        public int getStatusCode() { return statusCode; }
        public String getContentType() { return contentType; }
    }

    /**
     * Result wrapper indicating cache status
     */
    public static class IdempotentResult<T> {
        private final T result;
        private final String responseBody;
        private final int statusCode;
        private final String contentType;
        private final boolean fromCache;

        private IdempotentResult(T result, String responseBody, int statusCode, String contentType, boolean fromCache) {
            this.result = result;
            this.responseBody = responseBody;
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.fromCache = fromCache;
        }

        public static <T> IdempotentResult<T> fromCache(String responseBody, int statusCode, String contentType) {
            return new IdempotentResult<>(null, responseBody, statusCode, contentType, true);
        }

        public static <T> IdempotentResult<T> fromExecution(T result) {
            return new IdempotentResult<>(result, null, 200, "application/json", false);
        }

        public T getResult() { return result; }
        public String getResponseBody() { return responseBody; }
        public int getStatusCode() { return statusCode; }
        public String getContentType() { return contentType; }
        public boolean isFromCache() { return fromCache; }
    }

    // Exception classes

    public static class IdempotencyViolationException extends RuntimeException {
        public IdempotencyViolationException(String message) {
            super(message);
        }
    }

    public static class InvalidIdempotencyKeyException extends IllegalArgumentException {
        public InvalidIdempotencyKeyException(String message) {
            super(message);
        }
    }

    public static class MissingIdempotencyKeyException extends IllegalArgumentException {
        public MissingIdempotencyKeyException(String message) {
            super(message);
        }
    }
}