package com.bank.loanmanagement.loan.messaging.infrastructure.kafka;

import com.bank.loanmanagement.loan.messaging.infrastructure.kafka.KafkaSagaOrchestrator.SagaState;
import com.bank.loanmanagement.loan.messaging.infrastructure.kafka.KafkaSagaOrchestrator.SagaStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for SagaStateStore
 * Tests SAGA state persistence, retrieval, concurrency control, and cleanup
 * Ensures 85%+ test coverage for SAGA state management infrastructure
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SagaStateStore Tests")
class SagaStateStoreTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private ObjectMapper objectMapper;

    private SagaStateStore sagaStateStore;
    private SagaState testSagaState;
    private String sagaId;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        sagaStateStore = new SagaStateStore(redisTemplate, objectMapper);
        
        sagaId = "SAGA-123";
        testSagaState = createTestSagaState();
    }

    @Nested
    @DisplayName("SAGA State Persistence Tests")
    class SagaStatePersistenceTests {

        @Test
        @DisplayName("Should save SAGA state successfully")
        void shouldSaveSagaStateSuccessfully() throws Exception {
            // Given
            String stateJson = "{\"sagaId\":\"SAGA-123\",\"status\":\"ACTIVE\"}";
            when(objectMapper.writeValueAsString(testSagaState)).thenReturn(stateJson);
            when(valueOperations.setIfAbsent(anyString(), eq(stateJson), any(Duration.class))).thenReturn(true);

            // When
            SagaState result = sagaStateStore.saveSagaState(testSagaState);

            // Then
            assertThat(result).isEqualTo(testSagaState);
            verify(valueOperations).setIfAbsent(eq("saga:state:" + sagaId), eq(stateJson), eq(Duration.ofHours(24)));
            verify(redisTemplate).expire(eq("saga:state:" + sagaId), eq(24L), eq(TimeUnit.HOURS));
        }

        @Test
        @DisplayName("Should handle duplicate SAGA ID during save")
        void shouldHandleDuplicateSagaIdDuringSave() throws Exception {
            // Given
            String stateJson = "{\"sagaId\":\"SAGA-123\",\"status\":\"ACTIVE\"}";
            when(objectMapper.writeValueAsString(testSagaState)).thenReturn(stateJson);
            when(valueOperations.setIfAbsent(anyString(), eq(stateJson), any(Duration.class))).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> sagaStateStore.saveSagaState(testSagaState))
                .isInstanceOf(SagaStateStore.SagaStateConflictException.class)
                .hasMessageContaining("SAGA with ID " + sagaId + " already exists");
        }

        @Test
        @DisplayName("Should handle serialization failure during save")
        void shouldHandleSerializationFailureDuringSave() throws Exception {
            // Given
            when(objectMapper.writeValueAsString(testSagaState))
                .thenThrow(new RuntimeException("Serialization failed"));

            // When & Then
            assertThatThrownBy(() -> sagaStateStore.saveSagaState(testSagaState))
                .isInstanceOf(SagaStateStore.SagaStateException.class)
                .hasMessageContaining("Failed to serialize SAGA state");
        }
    }

    @Nested
    @DisplayName("SAGA State Retrieval Tests")
    class SagaStateRetrievalTests {

        @Test
        @DisplayName("Should retrieve existing SAGA state successfully")
        void shouldRetrieveExistingSagaStateSuccessfully() throws Exception {
            // Given
            String stateJson = "{\"sagaId\":\"SAGA-123\",\"status\":\"ACTIVE\"}";
            when(valueOperations.get("saga:state:" + sagaId)).thenReturn(stateJson);
            when(objectMapper.readValue(stateJson, SagaState.class)).thenReturn(testSagaState);

            // When
            Optional<SagaState> result = sagaStateStore.getSagaState(sagaId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(testSagaState);
            verify(valueOperations).get("saga:state:" + sagaId);
        }

        @Test
        @DisplayName("Should return empty for non-existent SAGA state")
        void shouldReturnEmptyForNonExistentSagaState() {
            // Given
            when(valueOperations.get("saga:state:" + sagaId)).thenReturn(null);

            // When
            Optional<SagaState> result = sagaStateStore.getSagaState(sagaId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should handle deserialization failure during retrieval")
        void shouldHandleDeserializationFailureDuringRetrieval() throws Exception {
            // Given
            String stateJson = "{invalid json}";
            when(valueOperations.get("saga:state:" + sagaId)).thenReturn(stateJson);
            when(objectMapper.readValue(stateJson, SagaState.class))
                .thenThrow(new RuntimeException("Deserialization failed"));

            // When & Then
            assertThatThrownBy(() -> sagaStateStore.getSagaState(sagaId))
                .isInstanceOf(SagaStateStore.SagaStateException.class)
                .hasMessageContaining("Failed to deserialize SAGA state");
        }
    }

    @Nested
    @DisplayName("SAGA State Update Tests")
    class SagaStateUpdateTests {

        @Test
        @DisplayName("Should update existing SAGA state successfully")
        void shouldUpdateExistingSagaStateSuccessfully() throws Exception {
            // Given
            SagaState updatedState = testSagaState.toBuilder()
                .status(SagaStatus.COMPLETED)
                .completedAt(OffsetDateTime.now())
                .build();
            String stateJson = "{\"sagaId\":\"SAGA-123\",\"status\":\"COMPLETED\"}";
            
            when(valueOperations.get("saga:state:" + sagaId)).thenReturn("existing-state");
            when(objectMapper.writeValueAsString(updatedState)).thenReturn(stateJson);
            when(valueOperations.set(anyString(), eq(stateJson), any(Duration.class))).thenReturn(null);

            // When
            SagaState result = sagaStateStore.updateSagaState(updatedState);

            // Then
            assertThat(result).isEqualTo(updatedState);
            verify(valueOperations).set(eq("saga:state:" + sagaId), eq(stateJson), eq(Duration.ofHours(24)));
        }

        @Test
        @DisplayName("Should handle update of non-existent SAGA state")
        void shouldHandleUpdateOfNonExistentSagaState() throws Exception {
            // Given
            when(valueOperations.get("saga:state:" + sagaId)).thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> sagaStateStore.updateSagaState(testSagaState))
                .isInstanceOf(SagaStateStore.SagaStateNotFoundException.class)
                .hasMessageContaining("SAGA state not found for ID: " + sagaId);
        }

        @Test
        @DisplayName("Should handle optimistic locking for concurrent updates")
        void shouldHandleOptimisticLockingForConcurrentUpdates() throws Exception {
            // Given
            SagaState stateWithOldVersion = testSagaState.toBuilder().version(1L).build();
            SagaState existingState = testSagaState.toBuilder().version(2L).build();
            
            when(valueOperations.get("saga:state:" + sagaId)).thenReturn("existing-state");
            when(objectMapper.readValue("existing-state", SagaState.class)).thenReturn(existingState);

            // When & Then
            assertThatThrownBy(() -> sagaStateStore.updateSagaState(stateWithOldVersion))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageContaining("SAGA state version mismatch");
        }
    }

    @Nested
    @DisplayName("SAGA State Query Tests")
    class SagaStateQueryTests {

        @Test
        @DisplayName("Should find active SAGAs successfully")
        void shouldFindActiveSagasSuccessfully() throws Exception {
            // Given
            Set<String> sagaKeys = Set.of("saga:state:SAGA-1", "saga:state:SAGA-2", "saga:state:SAGA-3");
            SagaState activeSaga1 = createSagaState("SAGA-1", SagaStatus.ACTIVE);
            SagaState activeSaga2 = createSagaState("SAGA-2", SagaStatus.ACTIVE);
            SagaState completedSaga = createSagaState("SAGA-3", SagaStatus.COMPLETED);
            
            when(redisTemplate.keys("saga:state:*")).thenReturn(sagaKeys);
            when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "{\"sagaId\":\"SAGA-1\",\"status\":\"ACTIVE\"}",
                "{\"sagaId\":\"SAGA-2\",\"status\":\"ACTIVE\"}",
                "{\"sagaId\":\"SAGA-3\",\"status\":\"COMPLETED\"}"
            ));
            when(objectMapper.readValue(contains("SAGA-1"), eq(SagaState.class))).thenReturn(activeSaga1);
            when(objectMapper.readValue(contains("SAGA-2"), eq(SagaState.class))).thenReturn(activeSaga2);
            when(objectMapper.readValue(contains("SAGA-3"), eq(SagaState.class))).thenReturn(completedSaga);

            // When
            List<SagaState> activeSagas = sagaStateStore.findActiveSagas();

            // Then
            assertThat(activeSagas).hasSize(2);
            assertThat(activeSagas).extracting(SagaState::getSagaId).containsExactlyInAnyOrder("SAGA-1", "SAGA-2");
        }

        @Test
        @DisplayName("Should find SAGAs by status successfully")
        void shouldFindSagasByStatusSuccessfully() throws Exception {
            // Given
            Set<String> sagaKeys = Set.of("saga:state:SAGA-1", "saga:state:SAGA-2");
            SagaState failedSaga1 = createSagaState("SAGA-1", SagaStatus.FAILED);
            SagaState failedSaga2 = createSagaState("SAGA-2", SagaStatus.FAILED);
            
            when(redisTemplate.keys("saga:state:*")).thenReturn(sagaKeys);
            when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "{\"sagaId\":\"SAGA-1\",\"status\":\"FAILED\"}",
                "{\"sagaId\":\"SAGA-2\",\"status\":\"FAILED\"}"
            ));
            when(objectMapper.readValue(contains("SAGA-1"), eq(SagaState.class))).thenReturn(failedSaga1);
            when(objectMapper.readValue(contains("SAGA-2"), eq(SagaState.class))).thenReturn(failedSaga2);

            // When
            List<SagaState> failedSagas = sagaStateStore.findSagasByStatus(SagaStatus.FAILED);

            // Then
            assertThat(failedSagas).hasSize(2);
            assertThat(failedSagas).allMatch(saga -> saga.getStatus() == SagaStatus.FAILED);
        }

        @Test
        @DisplayName("Should find expired SAGAs successfully")
        void shouldFindExpiredSagasSuccessfully() throws Exception {
            // Given
            OffsetDateTime now = OffsetDateTime.now();
            Set<String> sagaKeys = Set.of("saga:state:SAGA-1", "saga:state:SAGA-2");
            SagaState expiredSaga = createSagaState("SAGA-1", SagaStatus.ACTIVE)
                .toBuilder().createdAt(now.minusHours(25)).build();
            SagaState activeSaga = createSagaState("SAGA-2", SagaStatus.ACTIVE)
                .toBuilder().createdAt(now.minusHours(1)).build();
            
            when(redisTemplate.keys("saga:state:*")).thenReturn(sagaKeys);
            when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "{\"sagaId\":\"SAGA-1\",\"status\":\"ACTIVE\"}",
                "{\"sagaId\":\"SAGA-2\",\"status\":\"ACTIVE\"}"
            ));
            when(objectMapper.readValue(contains("SAGA-1"), eq(SagaState.class))).thenReturn(expiredSaga);
            when(objectMapper.readValue(contains("SAGA-2"), eq(SagaState.class))).thenReturn(activeSaga);

            // When
            List<SagaState> expiredSagas = sagaStateStore.findExpiredSagas(Duration.ofHours(24));

            // Then
            assertThat(expiredSagas).hasSize(1);
            assertThat(expiredSagas.get(0).getSagaId()).isEqualTo("SAGA-1");
        }
    }

    @Nested
    @DisplayName("SAGA State Statistics Tests")
    class SagaStateStatisticsTests {

        @Test
        @DisplayName("Should count SAGAs by status accurately")
        void shouldCountSagasByStatusAccurately() throws Exception {
            // Given
            Set<String> sagaKeys = Set.of("saga:state:SAGA-1", "saga:state:SAGA-2", "saga:state:SAGA-3", "saga:state:SAGA-4");
            when(redisTemplate.keys("saga:state:*")).thenReturn(sagaKeys);
            when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "{\"sagaId\":\"SAGA-1\",\"status\":\"ACTIVE\"}",
                "{\"sagaId\":\"SAGA-2\",\"status\":\"ACTIVE\"}",
                "{\"sagaId\":\"SAGA-3\",\"status\":\"COMPLETED\"}",
                "{\"sagaId\":\"SAGA-4\",\"status\":\"FAILED\"}"
            ));
            when(objectMapper.readValue(contains("SAGA-1"), eq(SagaState.class)))
                .thenReturn(createSagaState("SAGA-1", SagaStatus.ACTIVE));
            when(objectMapper.readValue(contains("SAGA-2"), eq(SagaState.class)))
                .thenReturn(createSagaState("SAGA-2", SagaStatus.ACTIVE));
            when(objectMapper.readValue(contains("SAGA-3"), eq(SagaState.class)))
                .thenReturn(createSagaState("SAGA-3", SagaStatus.COMPLETED));
            when(objectMapper.readValue(contains("SAGA-4"), eq(SagaState.class)))
                .thenReturn(createSagaState("SAGA-4", SagaStatus.FAILED));

            // When
            long activeCount = sagaStateStore.countSagasByStatus(SagaStatus.ACTIVE);
            long completedCount = sagaStateStore.countSagasByStatus(SagaStatus.COMPLETED);
            long failedCount = sagaStateStore.countSagasByStatus(SagaStatus.FAILED);

            // Then
            assertThat(activeCount).isEqualTo(2L);
            assertThat(completedCount).isEqualTo(1L);
            assertThat(failedCount).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should calculate average execution time accurately")
        void shouldCalculateAverageExecutionTimeAccurately() throws Exception {
            // Given
            OffsetDateTime now = OffsetDateTime.now();
            Set<String> sagaKeys = Set.of("saga:state:SAGA-1", "saga:state:SAGA-2");
            SagaState completedSaga1 = createSagaState("SAGA-1", SagaStatus.COMPLETED)
                .toBuilder()
                .createdAt(now.minusMinutes(30))
                .completedAt(now.minusMinutes(10))
                .build();
            SagaState completedSaga2 = createSagaState("SAGA-2", SagaStatus.COMPLETED)
                .toBuilder()
                .createdAt(now.minusMinutes(20))
                .completedAt(now)
                .build();
            
            when(redisTemplate.keys("saga:state:*")).thenReturn(sagaKeys);
            when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "{\"sagaId\":\"SAGA-1\",\"status\":\"COMPLETED\"}",
                "{\"sagaId\":\"SAGA-2\",\"status\":\"COMPLETED\"}"
            ));
            when(objectMapper.readValue(contains("SAGA-1"), eq(SagaState.class))).thenReturn(completedSaga1);
            when(objectMapper.readValue(contains("SAGA-2"), eq(SagaState.class))).thenReturn(completedSaga2);

            // When
            Duration averageExecutionTime = sagaStateStore.getAverageExecutionTime();

            // Then
            assertThat(averageExecutionTime).isEqualTo(Duration.ofMinutes(20)); // (20 + 20) / 2
        }

        @Test
        @DisplayName("Should return zero duration when no completed SAGAs exist")
        void shouldReturnZeroDurationWhenNoCompletedSagasExist() throws Exception {
            // Given
            Set<String> sagaKeys = Set.of("saga:state:SAGA-1");
            when(redisTemplate.keys("saga:state:*")).thenReturn(sagaKeys);
            when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "{\"sagaId\":\"SAGA-1\",\"status\":\"ACTIVE\"}"
            ));
            when(objectMapper.readValue(contains("SAGA-1"), eq(SagaState.class)))
                .thenReturn(createSagaState("SAGA-1", SagaStatus.ACTIVE));

            // When
            Duration averageExecutionTime = sagaStateStore.getAverageExecutionTime();

            // Then
            assertThat(averageExecutionTime).isEqualTo(Duration.ZERO);
        }
    }

    @Nested
    @DisplayName("SAGA State Cleanup Tests")
    class SagaStateCleanupTests {

        @Test
        @DisplayName("Should delete SAGA state successfully")
        void shouldDeleteSagaStateSuccessfully() {
            // Given
            when(redisTemplate.delete("saga:state:" + sagaId)).thenReturn(true);

            // When
            boolean result = sagaStateStore.deleteSagaState(sagaId);

            // Then
            assertThat(result).isTrue();
            verify(redisTemplate).delete("saga:state:" + sagaId);
        }

        @Test
        @DisplayName("Should handle deletion of non-existent SAGA state")
        void shouldHandleDeletionOfNonExistentSagaState() {
            // Given
            when(redisTemplate.delete("saga:state:" + sagaId)).thenReturn(false);

            // When
            boolean result = sagaStateStore.deleteSagaState(sagaId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should cleanup completed SAGAs older than retention period")
        void shouldCleanupCompletedSagasOlderThanRetentionPeriod() throws Exception {
            // Given
            OffsetDateTime now = OffsetDateTime.now();
            Duration retentionPeriod = Duration.ofDays(7);
            Set<String> sagaKeys = Set.of("saga:state:SAGA-1", "saga:state:SAGA-2", "saga:state:SAGA-3");
            
            SagaState oldCompletedSaga = createSagaState("SAGA-1", SagaStatus.COMPLETED)
                .toBuilder().completedAt(now.minusDays(10)).build();
            SagaState recentCompletedSaga = createSagaState("SAGA-2", SagaStatus.COMPLETED)
                .toBuilder().completedAt(now.minusDays(3)).build();
            SagaState activeSaga = createSagaState("SAGA-3", SagaStatus.ACTIVE);
            
            when(redisTemplate.keys("saga:state:*")).thenReturn(sagaKeys);
            when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "{\"sagaId\":\"SAGA-1\",\"status\":\"COMPLETED\"}",
                "{\"sagaId\":\"SAGA-2\",\"status\":\"COMPLETED\"}",
                "{\"sagaId\":\"SAGA-3\",\"status\":\"ACTIVE\"}"
            ));
            when(objectMapper.readValue(contains("SAGA-1"), eq(SagaState.class))).thenReturn(oldCompletedSaga);
            when(objectMapper.readValue(contains("SAGA-2"), eq(SagaState.class))).thenReturn(recentCompletedSaga);
            when(objectMapper.readValue(contains("SAGA-3"), eq(SagaState.class))).thenReturn(activeSaga);
            when(redisTemplate.delete("saga:state:SAGA-1")).thenReturn(true);

            // When
            int cleanedCount = sagaStateStore.cleanupCompletedSagas(retentionPeriod);

            // Then
            assertThat(cleanedCount).isEqualTo(1);
            verify(redisTemplate).delete("saga:state:SAGA-1");
            verify(redisTemplate, never()).delete("saga:state:SAGA-2");
            verify(redisTemplate, never()).delete("saga:state:SAGA-3");
        }

        @Test
        @DisplayName("Should cleanup failed SAGAs older than retention period")
        void shouldCleanupFailedSagasOlderThanRetentionPeriod() throws Exception {
            // Given
            OffsetDateTime now = OffsetDateTime.now();
            Duration retentionPeriod = Duration.ofDays(30);
            Set<String> sagaKeys = Set.of("saga:state:SAGA-1", "saga:state:SAGA-2");
            
            SagaState oldFailedSaga = createSagaState("SAGA-1", SagaStatus.FAILED)
                .toBuilder().failedAt(now.minusDays(35)).build();
            SagaState recentFailedSaga = createSagaState("SAGA-2", SagaStatus.FAILED)
                .toBuilder().failedAt(now.minusDays(15)).build();
            
            when(redisTemplate.keys("saga:state:*")).thenReturn(sagaKeys);
            when(valueOperations.multiGet(anyList())).thenReturn(List.of(
                "{\"sagaId\":\"SAGA-1\",\"status\":\"FAILED\"}",
                "{\"sagaId\":\"SAGA-2\",\"status\":\"FAILED\"}"
            ));
            when(objectMapper.readValue(contains("SAGA-1"), eq(SagaState.class))).thenReturn(oldFailedSaga);
            when(objectMapper.readValue(contains("SAGA-2"), eq(SagaState.class))).thenReturn(recentFailedSaga);
            when(redisTemplate.delete("saga:state:SAGA-1")).thenReturn(true);

            // When
            int cleanedCount = sagaStateStore.cleanupFailedSagas(retentionPeriod);

            // Then
            assertThat(cleanedCount).isEqualTo(1);
            verify(redisTemplate).delete("saga:state:SAGA-1");
            verify(redisTemplate, never()).delete("saga:state:SAGA-2");
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should report healthy when Redis is accessible")
        void shouldReportHealthyWhenRedisIsAccessible() {
            // Given
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            // When
            SagaStateStore.HealthStatus health = sagaStateStore.getHealth();

            // Then
            assertThat(health.isHealthy()).isTrue();
            assertThat(health.getStatusMessage()).contains("SAGA state store operating normally");
        }

        @Test
        @DisplayName("Should report unhealthy when Redis is not accessible")
        void shouldReportUnhealthyWhenRedisIsNotAccessible() {
            // Given
            when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

            // When
            SagaStateStore.HealthStatus health = sagaStateStore.getHealth();

            // Then
            assertThat(health.isHealthy()).isFalse();
            assertThat(health.getStatusMessage()).contains("Redis connection failed");
        }
    }

    // Helper methods

    private SagaState createTestSagaState() {
        return SagaState.builder()
            .sagaId(sagaId)
            .sagaType("LoanOriginationSaga")
            .status(SagaStatus.ACTIVE)
            .createdAt(OffsetDateTime.now())
            .version(1L)
            .completedSteps(new ArrayList<>())
            .stepResults(new HashMap<>())
            .build();
    }

    private SagaState createSagaState(String sagaId, SagaStatus status) {
        return SagaState.builder()
            .sagaId(sagaId)
            .sagaType("LoanOriginationSaga")
            .status(status)
            .createdAt(OffsetDateTime.now())
            .version(1L)
            .completedSteps(new ArrayList<>())
            .stepResults(new HashMap<>())
            .build();
    }
}