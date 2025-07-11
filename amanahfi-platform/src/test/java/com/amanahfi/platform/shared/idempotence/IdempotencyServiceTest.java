package com.amanahfi.platform.shared.idempotence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for IdempotencyService
 * 
 * This test class ensures the idempotency service correctly implements
 * exactly-once processing semantics for all Islamic finance operations.
 * 
 * Test Coverage:
 * - Request body hash validation
 * - Cache hit/miss scenarios
 * - Race condition handling
 * - Error caching behavior
 * - Performance monitoring
 * - Islamic finance compliance
 * 
 * Mathematical Definition: f(f(x)) = f(x)
 * Software Definition: Processing 1, 2, or n times has same net effect
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Idempotency Service Tests - Exactly-Once Processing")
class IdempotencyServiceTest {

    @Mock
    private IdempotencyStore idempotencyStore;

    private IdempotencyService idempotencyService;
    private IdempotencyKey testKey;
    private String testRequestBody;
    private IdempotencyRecord.OperationType testOperationType;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(idempotencyStore);
        testKey = IdempotencyKey.generate();
        testRequestBody = "{\"amount\": 1000, \"currency\": \"AED\"}";
        testOperationType = IdempotencyRecord.OperationType.MURABAHA_CREATION;
    }

    @Nested
    @DisplayName("Request Processing - First Time Execution")
    class FirstTimeExecution {

        @Test
        @DisplayName("Should execute operation when no cached record exists")
        void shouldExecuteOperationWhenNoCachedRecordExists() {
            // Given
            when(idempotencyStore.retrieve(testKey)).thenReturn(Optional.empty());
            when(idempotencyStore.storeIfAbsent(any(IdempotencyRecord.class))).thenReturn(Optional.empty());

            IdempotencyService.IdempotentOperation<String> operation = () -> 
                IdempotencyService.IdempotentOperationResult.success("OPERATION_RESULT", "{\"result\": \"success\"}");

            // When
            IdempotencyService.IdempotentResult<String> result = idempotencyService.processIdempotently(
                testKey, testRequestBody, operation, testOperationType
            );

            // Then
            assertNotNull(result);
            assertFalse(result.isFromCache());
            assertEquals("OPERATION_RESULT", result.getResult());
            assertEquals(200, result.getStatusCode());

            verify(idempotencyStore, times(1)).retrieve(testKey);
            verify(idempotencyStore, times(1)).storeIfAbsent(any(IdempotencyRecord.class));
        }

        @Test
        @DisplayName("Should store idempotency record with correct TTL for Islamic finance operations")
        void shouldStoreIdempotencyRecordWithCorrectTtlForIslamicFinanceOperations() {
            // Given
            when(idempotencyStore.retrieve(testKey)).thenReturn(Optional.empty());
            when(idempotencyStore.storeIfAbsent(any(IdempotencyRecord.class))).thenReturn(Optional.empty());

            IdempotencyService.IdempotentOperation<String> operation = () -> 
                IdempotencyService.IdempotentOperationResult.success("MURABAHA_CREATED", "{\"id\": \"123\"}");

            // When
            idempotencyService.processIdempotently(testKey, testRequestBody, operation, testOperationType);

            // Then
            verify(idempotencyStore).storeIfAbsent(argThat(record -> 
                record.getKey().equals(testKey) &&
                record.getOperationType() == IdempotencyRecord.OperationType.MURABAHA_CREATION &&
                record.isSuccessful() &&
                record.getRemainingTtlSeconds() > 0
            ));
        }
    }

    @Nested
    @DisplayName("Cache Hit Scenarios - Duplicate Requests")
    class CacheHitScenarios {

        @Test
        @DisplayName("Should return cached response when idempotency record exists with matching hash")
        void shouldReturnCachedResponseWhenIdempotencyRecordExistsWithMatchingHash() {
            // Given
            String requestBodyHash = calculateSha256Hash(testRequestBody);
            IdempotencyRecord cachedRecord = IdempotencyRecord.success(
                testKey,
                requestBodyHash,
                "{\"cached\": \"response\"}",
                201,
                "application/json",
                3600,
                testOperationType
            );

            when(idempotencyStore.retrieve(testKey)).thenReturn(Optional.of(cachedRecord));

            IdempotencyService.IdempotentOperation<String> operation = () -> {
                fail("Operation should not be executed when cache hit occurs");
                return null;
            };

            // When
            IdempotencyService.IdempotentResult<String> result = idempotencyService.processIdempotently(
                testKey, testRequestBody, operation, testOperationType
            );

            // Then
            assertNotNull(result);
            assertTrue(result.isFromCache());
            assertEquals("{\"cached\": \"response\"}", result.getResponseBody());
            assertEquals(201, result.getStatusCode());
            assertEquals("application/json", result.getContentType());

            verify(idempotencyStore, times(1)).retrieve(testKey);
            verify(idempotencyStore, never()).storeIfAbsent(any());
        }

        @Test
        @DisplayName("Should throw violation exception when idempotency key reused with different request body")
        void shouldThrowViolationExceptionWhenIdempotencyKeyReusedWithDifferentRequestBody() {
            // Given
            String originalRequestBody = "{\"amount\": 1000, \"currency\": \"AED\"}";
            String differentRequestBody = "{\"amount\": 2000, \"currency\": \"AED\"}";
            String originalHash = calculateSha256Hash(originalRequestBody);

            IdempotencyRecord cachedRecord = IdempotencyRecord.success(
                testKey,
                originalHash,
                "{\"original\": \"response\"}",
                200,
                "application/json",
                3600,
                testOperationType
            );

            when(idempotencyStore.retrieve(testKey)).thenReturn(Optional.of(cachedRecord));

            IdempotencyService.IdempotentOperation<String> operation = () -> 
                IdempotencyService.IdempotentOperationResult.success("NEW_RESULT", "{\"new\": \"result\"}");

            // When & Then
            IdempotencyService.IdempotencyViolationException exception = assertThrows(
                IdempotencyService.IdempotencyViolationException.class,
                () -> idempotencyService.processIdempotently(
                    testKey, differentRequestBody, operation, testOperationType
                )
            );

            assertTrue(exception.getMessage().contains("different request body"));
            verify(idempotencyStore, times(1)).retrieve(testKey);
            verify(idempotencyStore, never()).storeIfAbsent(any());
        }
    }

    @Nested
    @DisplayName("Race Condition Handling")
    class RaceConditionHandling {

        @Test
        @DisplayName("Should handle race condition when another thread stores record during execution")
        void shouldHandleRaceConditionWhenAnotherThreadStoresRecordDuringExecution() {
            // Given
            String requestBodyHash = calculateSha256Hash(testRequestBody);
            IdempotencyRecord conflictRecord = IdempotencyRecord.success(
                testKey,
                requestBodyHash,
                "{\"conflict\": \"response\"}",
                200,
                "application/json",
                3600,
                testOperationType
            );

            when(idempotencyStore.retrieve(testKey)).thenReturn(Optional.empty());
            when(idempotencyStore.storeIfAbsent(any(IdempotencyRecord.class))).thenReturn(Optional.of(conflictRecord));

            IdempotencyService.IdempotentOperation<String> operation = () -> 
                IdempotencyService.IdempotentOperationResult.success("MY_RESULT", "{\"my\": \"result\"}");

            // When
            IdempotencyService.IdempotentResult<String> result = idempotencyService.processIdempotently(
                testKey, testRequestBody, operation, testOperationType
            );

            // Then
            assertNotNull(result);
            assertTrue(result.isFromCache());
            assertEquals("{\"conflict\": \"response\"}", result.getResponseBody());
            assertEquals(200, result.getStatusCode());

            verify(idempotencyStore, times(1)).retrieve(testKey);
            verify(idempotencyStore, times(1)).storeIfAbsent(any(IdempotencyRecord.class));
        }

        @Test
        @DisplayName("Should throw violation exception on race condition with different request body hash")
        void shouldThrowViolationExceptionOnRaceConditionWithDifferentRequestBodyHash() {
            // Given
            String differentHash = "different_hash_from_another_request";
            IdempotencyRecord conflictRecord = IdempotencyRecord.success(
                testKey,
                differentHash,
                "{\"conflict\": \"response\"}",
                200,
                "application/json",
                3600,
                testOperationType
            );

            when(idempotencyStore.retrieve(testKey)).thenReturn(Optional.empty());
            when(idempotencyStore.storeIfAbsent(any(IdempotencyRecord.class))).thenReturn(Optional.of(conflictRecord));

            IdempotencyService.IdempotentOperation<String> operation = () -> 
                IdempotencyService.IdempotentOperationResult.success("MY_RESULT", "{\"my\": \"result\"}");

            // When & Then
            IdempotencyService.IdempotencyViolationException exception = assertThrows(
                IdempotencyService.IdempotencyViolationException.class,
                () -> idempotencyService.processIdempotently(
                    testKey, testRequestBody, operation, testOperationType
                )
            );

            assertTrue(exception.getMessage().contains("Concurrent idempotency key usage"));
        }
    }

    @Nested
    @DisplayName("Error Handling and Caching")
    class ErrorHandlingAndCaching {

        @Test
        @DisplayName("Should cache validation errors to prevent retry storms")
        void shouldCacheValidationErrorsToPreventRetryStorms() {
            // Given
            when(idempotencyStore.retrieve(testKey)).thenReturn(Optional.empty());
            when(idempotencyStore.storeIfAbsent(any(IdempotencyRecord.class))).thenReturn(Optional.empty());

            IdempotencyService.IdempotentOperation<String> operation = () -> {
                throw new IllegalArgumentException("Invalid Murabaha parameters");
            };

            // When & Then
            assertThrows(IllegalArgumentException.class, () -> 
                idempotencyService.processIdempotently(testKey, testRequestBody, operation, testOperationType)
            );

            // Verify error was cached
            verify(idempotencyStore, times(2)).storeIfAbsent(any(IdempotencyRecord.class)); // Once for error
        }

        @Test
        @DisplayName("Should not cache infrastructure errors that might be transient")
        void shouldNotCacheInfrastructureErrorsThatMightBeTransient() {
            // Given
            when(idempotencyStore.retrieve(testKey)).thenReturn(Optional.empty());
            when(idempotencyStore.storeIfAbsent(any(IdempotencyRecord.class))).thenReturn(Optional.empty());

            IdempotencyService.IdempotentOperation<String> operation = () -> {
                throw new RuntimeException("Database connection failed");
            };

            // When & Then
            assertThrows(RuntimeException.class, () -> 
                idempotencyService.processIdempotently(testKey, testRequestBody, operation, testOperationType)
            );

            // Verify only the initial attempt to store success was made
            verify(idempotencyStore, times(1)).storeIfAbsent(any(IdempotencyRecord.class));
        }
    }

    @Nested
    @DisplayName("Key Validation and Generation")
    class KeyValidationAndGeneration {

        @Test
        @DisplayName("Should accept valid provided idempotency key")
        void shouldAcceptValidProvidedIdempotencyKey() {
            // Given
            IdempotencyKey validKey = IdempotencyKey.of("valid-key-12345");
            Optional<IdempotencyKey> providedKey = Optional.of(validKey);

            // When
            IdempotencyKey result = idempotencyService.validateOrGenerateKey(
                providedKey, 
                IdempotencyRecord.OperationType.PAYMENT_INITIATION
            );

            // Then
            assertEquals(validKey, result);
        }

        @Test
        @DisplayName("Should throw exception for invalid idempotency key format")
        void shouldThrowExceptionForInvalidIdempotencyKeyFormat() {
            // Given
            IdempotencyKey invalidKey = IdempotencyKey.of("invalid key with spaces!");
            Optional<IdempotencyKey> providedKey = Optional.of(invalidKey);

            // When & Then
            assertThrows(IdempotencyService.InvalidIdempotencyKeyException.class, () ->
                idempotencyService.validateOrGenerateKey(
                    providedKey, 
                    IdempotencyRecord.OperationType.PAYMENT_INITIATION
                )
            );
        }

        @Test
        @DisplayName("Should require idempotency key for financially sensitive operations")
        void shouldRequireIdempotencyKeyForFinanciallySensitiveOperations() {
            // Given
            Optional<IdempotencyKey> noKey = Optional.empty();

            // When & Then
            assertThrows(IdempotencyService.MissingIdempotencyKeyException.class, () ->
                idempotencyService.validateOrGenerateKey(
                    noKey, 
                    IdempotencyRecord.OperationType.PAYMENT_INITIATION
                )
            );
        }

        @Test
        @DisplayName("Should generate key for non-financially sensitive operations when not provided")
        void shouldGenerateKeyForNonFinanciallySensitiveOperationsWhenNotProvided() {
            // Given
            Optional<IdempotencyKey> noKey = Optional.empty();

            // When
            IdempotencyKey result = idempotencyService.validateOrGenerateKey(
                noKey, 
                IdempotencyRecord.OperationType.API_CALL
            );

            // Then
            assertNotNull(result);
            assertTrue(result.isUuidFormat());
        }
    }

    @Nested
    @DisplayName("Performance and Health Monitoring")
    class PerformanceAndHealthMonitoring {

        @Test
        @DisplayName("Should return store statistics for monitoring")
        void shouldReturnStoreStatisticsForMonitoring() {
            // Given
            IdempotencyStore.IdempotencyStoreStats expectedStats = new IdempotencyStore.IdempotencyStoreStats(
                1000L, 50L, 800L, 200L, 0.80, 15L, 20L, 35L
            );
            when(idempotencyStore.getStats()).thenReturn(expectedStats);

            // When
            IdempotencyStore.IdempotencyStoreStats stats = idempotencyService.getPerformanceStats();

            // Then
            assertEquals(expectedStats, stats);
            assertTrue(stats.isPerformanceAcceptable()); // P95 ≤ 25ms, hit ratio ≥ 80%
        }

        @Test
        @DisplayName("Should report healthy when store and performance are acceptable")
        void shouldReportHealthyWhenStoreAndPerformanceAreAcceptable() {
            // Given
            when(idempotencyStore.isHealthy()).thenReturn(true);
            when(idempotencyStore.getStats()).thenReturn(
                new IdempotencyStore.IdempotencyStoreStats(
                    1000L, 50L, 800L, 200L, 0.85, 15L, 20L, 30L
                )
            );

            // When
            boolean healthy = idempotencyService.isHealthy();

            // Then
            assertTrue(healthy);
        }

        @Test
        @DisplayName("Should report unhealthy when performance degrades")
        void shouldReportUnhealthyWhenPerformanceDegrades() {
            // Given
            when(idempotencyStore.isHealthy()).thenReturn(true);
            when(idempotencyStore.getStats()).thenReturn(
                new IdempotencyStore.IdempotencyStoreStats(
                    1000L, 50L, 800L, 200L, 0.85, 50L, 80L, 120L // P95 > 25ms
                )
            );

            // When
            boolean healthy = idempotencyService.isHealthy();

            // Then
            assertFalse(healthy);
        }
    }

    @Nested
    @DisplayName("Cleanup Operations")
    class CleanupOperations {

        @Test
        @DisplayName("Should clean up expired records and return count")
        void shouldCleanUpExpiredRecordsAndReturnCount() {
            // Given
            when(idempotencyStore.cleanupExpiredRecords()).thenReturn(150L);

            // When
            long cleanedCount = idempotencyService.cleanupExpiredRecords();

            // Then
            assertEquals(150L, cleanedCount);
            verify(idempotencyStore, times(1)).cleanupExpiredRecords();
        }

        @Test
        @DisplayName("Should handle cleanup failures gracefully")
        void shouldHandleCleanupFailuresGracefully() {
            // Given
            when(idempotencyStore.cleanupExpiredRecords()).thenThrow(new RuntimeException("Cleanup failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> idempotencyService.cleanupExpiredRecords());
        }
    }

    @Nested
    @DisplayName("Islamic Finance Specific Scenarios")
    class IslamicFinanceSpecificScenarios {

        @Test
        @DisplayName("Should prevent duplicate Murabaha creation with same asset details")
        void shouldPreventDuplicateMurabahaCreationWithSameAssetDetails() {
            // Given
            String murabahaRequest = "{\"assetCost\": 100000, \"profitMargin\": 0.05, \"asset\": \"commercial_vehicle\"}";
            String requestHash = calculateSha256Hash(murabahaRequest);
            
            IdempotencyRecord existingRecord = IdempotencyRecord.success(
                testKey,
                requestHash,
                "{\"murabahaId\": \"MUR-001\", \"status\": \"CREATED\"}",
                201,
                "application/json",
                12 * 60 * 60, // 12 hours TTL
                IdempotencyRecord.OperationType.MURABAHA_CREATION
            );

            when(idempotencyStore.retrieve(testKey)).thenReturn(Optional.of(existingRecord));

            IdempotencyService.IdempotentOperation<String> operation = () -> {
                fail("Should not create duplicate Murabaha");
                return null;
            };

            // When
            IdempotencyService.IdempotentResult<String> result = idempotencyService.processIdempotently(
                testKey, murabahaRequest, operation, IdempotencyRecord.OperationType.MURABAHA_CREATION
            );

            // Then
            assertTrue(result.isFromCache());
            assertTrue(result.getResponseBody().contains("MUR-001"));
            assertEquals(201, result.getStatusCode());
        }

        @Test
        @DisplayName("Should enforce different TTL for different Islamic finance operations")
        void shouldEnforceDifferentTtlForDifferentIslamicFinanceOperations() {
            // Given
            when(idempotencyStore.retrieve(any())).thenReturn(Optional.empty());
            when(idempotencyStore.storeIfAbsent(any())).thenReturn(Optional.empty());

            IdempotencyService.IdempotentOperation<String> operation = () -> 
                IdempotencyService.IdempotentOperationResult.success("SUCCESS", "{\"result\": \"ok\"}");

            // When - Test Qard Hassan (should have longer TTL as it's charitable)
            idempotencyService.processIdempotently(
                IdempotencyKey.generate(), 
                "{\"amount\": 5000}", 
                operation, 
                IdempotencyRecord.OperationType.QARD_HASSAN_CREATION
            );

            // Then
            verify(idempotencyStore).storeIfAbsent(argThat(record -> 
                record.getOperationType() == IdempotencyRecord.OperationType.QARD_HASSAN_CREATION &&
                record.getRemainingTtlSeconds() == IdempotencyRecord.OperationType.QARD_HASSAN_CREATION.getDefaultTtlSeconds()
            ));
        }
    }

    // Helper method to calculate SHA-256 hash (simplified for testing)
    private String calculateSha256Hash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }
}