package com.bank.infrastructure.idempotency;

import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Idempotency Service for duplicate request detection and handling
 * 
 * Implements idempotency pattern for financial operations:
 * - Stores request/response pairs with expiration
 * - Detects duplicate requests by idempotency key
 * - Returns cached responses for duplicates
 * - Supports distributed caching with Redis
 * - Handles concurrent requests safely
 */
@Service
@Transactional
public class IdempotencyService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    // Default TTL for idempotency records (24 hours)
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    
    // Key prefix for idempotency records
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    
    // Key prefix for processing locks
    private static final String PROCESSING_LOCK_PREFIX = "processing:";
    
    // Lock TTL to prevent deadlocks (5 minutes)
    private static final Duration LOCK_TTL = Duration.ofMinutes(5);
    
    public IdempotencyService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Check if a request is duplicate and return cached response if exists
     */
    public <T> Optional<IdempotencyResult<T>> checkIdempotency(String idempotencyKey, 
                                                               String operationId,
                                                               Class<T> responseType) {
        String redisKey = buildIdempotencyKey(idempotencyKey, operationId);
        
        try {
            String cachedValue = redisTemplate.opsForValue().get(redisKey);
            if (cachedValue != null) {
                IdempotencyRecord record = objectMapper.readValue(cachedValue, IdempotencyRecord.class);
                
                if (record.isCompleted()) {
                    // Request already processed, return cached response
                    T response = objectMapper.convertValue(record.getResponse(), responseType);
                    return Optional.of(IdempotencyResult.duplicate(response, record.getHttpStatus()));
                } else {
                    // Request is still being processed
                    return Optional.of(IdempotencyResult.processing());
                }
            }
            
            return Optional.empty();
            
        } catch (JsonProcessingException e) {
            throw new IdempotencyException("Failed to deserialize cached idempotency record", e);
        }
    }
    
    /**
     * Acquire processing lock for the idempotency key
     */
    public boolean acquireProcessingLock(String idempotencyKey, String operationId) {
        String lockKey = buildProcessingLockKey(idempotencyKey, operationId);
        String lockValue = Instant.now().toString();
        
        // Use SET NX EX for atomic lock acquisition
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
            lockKey, 
            lockValue, 
            LOCK_TTL.toSeconds(), 
            TimeUnit.SECONDS
        );
        
        if (Boolean.TRUE.equals(acquired)) {
            // Also create a processing record
            createProcessingRecord(idempotencyKey, operationId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Store successful response in idempotency cache
     */
    public <T> void storeSuccessResponse(String idempotencyKey, 
                                       String operationId,
                                       T response, 
                                       int httpStatus) {
        storeResponse(idempotencyKey, operationId, response, httpStatus, null, true);
    }
    
    /**
     * Store error response in idempotency cache
     */
    public void storeErrorResponse(String idempotencyKey, 
                                 String operationId,
                                 Object errorResponse, 
                                 int httpStatus, 
                                 String errorMessage) {
        storeResponse(idempotencyKey, operationId, errorResponse, httpStatus, errorMessage, true);
    }
    
    /**
     * Release processing lock
     */
    public void releaseProcessingLock(String idempotencyKey, String operationId) {
        String lockKey = buildProcessingLockKey(idempotencyKey, operationId);
        redisTemplate.delete(lockKey);
    }
    
    /**
     * Clean up failed processing attempt
     */
    public void cleanupFailedProcessing(String idempotencyKey, String operationId) {
        String idempotencyRedisKey = buildIdempotencyKey(idempotencyKey, operationId);
        String lockKey = buildProcessingLockKey(idempotencyKey, operationId);
        
        // Remove both the processing record and lock
        redisTemplate.delete(idempotencyRedisKey);
        redisTemplate.delete(lockKey);
    }
    
    /**
     * Get processing status for an idempotency key
     */
    public IdempotencyStatus getProcessingStatus(String idempotencyKey, String operationId) {
        String redisKey = buildIdempotencyKey(idempotencyKey, operationId);
        
        try {
            String cachedValue = redisTemplate.opsForValue().get(redisKey);
            if (cachedValue == null) {
                return IdempotencyStatus.NOT_FOUND;
            }
            
            IdempotencyRecord record = objectMapper.readValue(cachedValue, IdempotencyRecord.class);
            return record.isCompleted() ? IdempotencyStatus.COMPLETED : IdempotencyStatus.PROCESSING;
            
        } catch (JsonProcessingException e) {
            throw new IdempotencyException("Failed to check processing status", e);
        }
    }
    
    /**
     * Create initial processing record
     */
    private void createProcessingRecord(String idempotencyKey, String operationId) {
        IdempotencyRecord record = IdempotencyRecord.builder()
            .idempotencyKey(idempotencyKey)
            .operationId(operationId)
            .createdAt(Instant.now())
            .completed(false)
            .build();
        
        storeRecord(idempotencyKey, operationId, record, DEFAULT_TTL);
    }
    
    /**
     * Store response in idempotency cache
     */
    private <T> void storeResponse(String idempotencyKey, 
                                 String operationId,
                                 T response, 
                                 int httpStatus, 
                                 String errorMessage, 
                                 boolean completed) {
        IdempotencyRecord record = IdempotencyRecord.builder()
            .idempotencyKey(idempotencyKey)
            .operationId(operationId)
            .createdAt(Instant.now())
            .completedAt(completed ? Instant.now() : null)
            .completed(completed)
            .httpStatus(httpStatus)
            .response(response)
            .errorMessage(errorMessage)
            .build();
        
        storeRecord(idempotencyKey, operationId, record, DEFAULT_TTL);
        
        // Release processing lock if completed
        if (completed) {
            releaseProcessingLock(idempotencyKey, operationId);
        }
    }
    
    /**
     * Store idempotency record in Redis
     */
    private void storeRecord(String idempotencyKey, String operationId, 
                           IdempotencyRecord record, Duration ttl) {
        try {
            String redisKey = buildIdempotencyKey(idempotencyKey, operationId);
            String serializedRecord = objectMapper.writeValueAsString(record);
            
            redisTemplate.opsForValue().set(
                redisKey, 
                serializedRecord, 
                ttl.toSeconds(), 
                TimeUnit.SECONDS
            );
            
        } catch (JsonProcessingException e) {
            throw new IdempotencyException("Failed to serialize idempotency record", e);
        }
    }
    
    /**
     * Build Redis key for idempotency record
     */
    private String buildIdempotencyKey(String idempotencyKey, String operationId) {
        return IDEMPOTENCY_KEY_PREFIX + operationId + ":" + idempotencyKey;
    }
    
    /**
     * Build Redis key for processing lock
     */
    private String buildProcessingLockKey(String idempotencyKey, String operationId) {
        return PROCESSING_LOCK_PREFIX + operationId + ":" + idempotencyKey;
    }
    
    /**
     * Idempotency record stored in cache
     */
    @lombok.Builder
    @lombok.Data
    public static class IdempotencyRecord {
        private String idempotencyKey;
        private String operationId;
        private Instant createdAt;
        private Instant completedAt;
        private boolean completed;
        private int httpStatus;
        private Object response;
        private String errorMessage;
    }
    
    /**
     * Result of idempotency check
     */
    public static class IdempotencyResult<T> {
        private final boolean isDuplicate;
        private final boolean isProcessing;
        private final T response;
        private final int httpStatus;
        
        private IdempotencyResult(boolean isDuplicate, boolean isProcessing, T response, int httpStatus) {
            this.isDuplicate = isDuplicate;
            this.isProcessing = isProcessing;
            this.response = response;
            this.httpStatus = httpStatus;
        }
        
        public static <T> IdempotencyResult<T> duplicate(T response, int httpStatus) {
            return new IdempotencyResult<>(true, false, response, httpStatus);
        }
        
        public static <T> IdempotencyResult<T> processing() {
            return new IdempotencyResult<>(false, true, null, 0);
        }
        
        public boolean isDuplicate() { return isDuplicate; }
        public boolean isProcessing() { return isProcessing; }
        public T getResponse() { return response; }
        public int getHttpStatus() { return httpStatus; }
    }
    
    /**
     * Processing status enumeration
     */
    public enum IdempotencyStatus {
        NOT_FOUND,
        PROCESSING,
        COMPLETED
    }
    
    /**
     * Idempotency service exception
     */
    public static class IdempotencyException extends RuntimeException {
        public IdempotencyException(String message) {
            super(message);
        }
        
        public IdempotencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}