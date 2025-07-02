package com.bank.loanmanagement.loan.messaging.infrastructure.kafka;

import com.bank.loanmanagement.loan.saga.domain.SagaDefinition;
import com.bank.loanmanagement.loan.saga.domain.LoanOriginationSaga;
import com.bank.loanmanagement.loan.sharedkernel.domain.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive TDD tests for KafkaSagaOrchestrator
 * Tests SAGA orchestration, step coordination, compensation, and error handling
 * Ensures 85%+ test coverage for SAGA coordination infrastructure
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaSagaOrchestrator Tests")
class KafkaSagaOrchestratorTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private KafkaTopicResolver topicResolver;
    
    @Mock
    private SagaStateStore sagaStateStore;
    
    @Mock
    private SagaStepExecutor sagaStepExecutor;
    
    @Mock
    private SendResult<String, String> sendResult;

    private KafkaSagaOrchestrator sagaOrchestrator;
    private LoanOriginationSaga loanOriginationSaga;
    private String sagaId;
    private TestDomainEvent testEvent;
    private CompletableFuture<SendResult<String, String>> mockFuture;

    @BeforeEach
    void setUp() {
        sagaOrchestrator = new KafkaSagaOrchestrator(
            kafkaTemplate, objectMapper, topicResolver, sagaStateStore, sagaStepExecutor);
        
        loanOriginationSaga = new LoanOriginationSaga();
        sagaId = "SAGA-" + UUID.randomUUID().toString();
        testEvent = new TestDomainEvent("AGGREGATE-123", 1L, "test-data");
        mockFuture = CompletableFuture.completedFuture(sendResult);
    }

    @Nested
    @DisplayName("SAGA Initiation Tests")
    class SagaInitiationTests {

        @Test
        @DisplayName("Should initiate SAGA successfully with valid parameters")
        void shouldInitiateSagaSuccessfullyWithValidParameters() throws Exception {
            // Given
            Map<String, Object> sagaData = Map.of("loanAmount", 50000, "customerId", "CUST-001");
            String expectedTopic = "banking.consumer-loan.saga.loanoriginationsaga";
            
            when(topicResolver.resolveSagaTopicForEvent(any(), eq("LoanOriginationSaga")))
                .thenReturn(expectedTopic);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"sagaId\":\"" + sagaId + "\"}");
            when(kafkaTemplate.send(eq(expectedTopic), eq(sagaId), anyString()))
                .thenReturn(mockFuture);
            when(sagaStateStore.saveSagaState(any())).thenReturn(mockSagaState());

            // When
            CompletableFuture<String> result = sagaOrchestrator.initiateSaga(
                loanOriginationSaga, sagaId, sagaData, testEvent);

            // Then
            assertThat(result).isCompletedWithValue(sagaId);
            verify(sagaStateStore).saveSagaState(any(KafkaSagaOrchestrator.SagaState.class));
            verify(kafkaTemplate).send(expectedTopic, sagaId, anyString());
        }

        @Test
        @DisplayName("Should handle SAGA initiation failure gracefully")
        void shouldHandleSagaInitiationFailureGracefully() throws Exception {
            // Given
            Map<String, Object> sagaData = Map.of("loanAmount", 50000);
            
            when(topicResolver.resolveSagaTopicForEvent(any(), eq("LoanOriginationSaga")))
                .thenReturn("banking.consumer-loan.saga.loanoriginationsaga");
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"sagaId\":\"" + sagaId + "\"}");
            when(kafkaTemplate.send(anyString(), eq(sagaId), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka failure")));

            // When
            CompletableFuture<String> result = sagaOrchestrator.initiateSaga(
                loanOriginationSaga, sagaId, sagaData, testEvent);

            // Then
            assertThat(result).isCompletedExceptionally();
            result.handle((sagaIdResult, throwable) -> {
                assertThat(throwable).isInstanceOf(RuntimeException.class);
                assertThat(throwable.getMessage()).contains("Kafka failure");
                return null;
            });
        }

        @Test
        @DisplayName("Should generate unique SAGA ID when not provided")
        void shouldGenerateUniqueSagaIdWhenNotProvided() throws Exception {
            // Given
            Map<String, Object> sagaData = Map.of("loanAmount", 50000);
            
            when(topicResolver.resolveSagaTopicForEvent(any(), eq("LoanOriginationSaga")))
                .thenReturn("banking.consumer-loan.saga.loanoriginationsaga");
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"sagaId\":\"generated-id\"}");
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(mockFuture);
            when(sagaStateStore.saveSagaState(any())).thenReturn(mockSagaState());

            // When
            CompletableFuture<String> result = sagaOrchestrator.initiateSaga(
                loanOriginationSaga, sagaData, testEvent);

            // Then
            assertThat(result).isCompleted();
            String generatedSagaId = result.get();
            assertThat(generatedSagaId).isNotNull();
            assertThat(generatedSagaId).startsWith("SAGA-");
        }
    }

    @Nested
    @DisplayName("SAGA Step Execution Tests")
    class SagaStepExecutionTests {

        @Test
        @DisplayName("Should execute SAGA step successfully")
        void shouldExecuteSagaStepSuccessfully() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createActiveSagaState();
            String stepId = "validate-customer";
            Map<String, Object> stepData = Map.of("customerId", "CUST-001");
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));
            when(sagaStepExecutor.executeStep(any(), eq(stepId), eq(stepData)))
                .thenReturn(CompletableFuture.completedFuture(Map.of("validationResult", "APPROVED")));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"stepResult\":\"success\"}");
            when(kafkaTemplate.send(anyString(), eq(sagaId), anyString()))
                .thenReturn(mockFuture);

            // When
            CompletableFuture<Map<String, Object>> result = sagaOrchestrator.executeStep(
                sagaId, stepId, stepData);

            // Then
            assertThat(result).isCompleted();
            Map<String, Object> stepResult = result.get();
            assertThat(stepResult).containsEntry("validationResult", "APPROVED");
            
            verify(sagaStateStore).updateSagaState(any(KafkaSagaOrchestrator.SagaState.class));
            verify(sagaStepExecutor).executeStep(any(), eq(stepId), eq(stepData));
        }

        @Test
        @DisplayName("Should handle step execution failure with compensation")
        void shouldHandleStepExecutionFailureWithCompensation() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createActiveSagaState();
            String stepId = "assess-credit-risk";
            Map<String, Object> stepData = Map.of("customerId", "CUST-001");
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));
            when(sagaStepExecutor.executeStep(any(), eq(stepId), eq(stepData)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Credit check failed")));
            when(sagaStepExecutor.executeCompensation(any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(Map.of("compensated", true)));

            // When
            CompletableFuture<Map<String, Object>> result = sagaOrchestrator.executeStep(
                sagaId, stepId, stepData);

            // Then
            assertThat(result).isCompletedExceptionally();
            
            // Verify compensation was triggered
            verify(sagaStepExecutor).executeCompensation(any(), anyString());
            verify(sagaStateStore).updateSagaState(argThat(state -> 
                state.getStatus() == KafkaSagaOrchestrator.SagaStatus.COMPENSATING));
        }

        @Test
        @DisplayName("Should execute conditional steps based on previous results")
        void shouldExecuteConditionalStepsBasedOnPreviousResults() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createActiveSagaState();
            sagaState.getStepResults().put("assess-credit-risk", Map.of("creditRiskResult", "APPROVED"));
            String stepId = "grant-loan-approval";
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));
            when(sagaStepExecutor.executeStep(any(), eq(stepId), any()))
                .thenReturn(CompletableFuture.completedFuture(Map.of("approvalGranted", true)));

            // When
            CompletableFuture<Map<String, Object>> result = sagaOrchestrator.executeStep(
                sagaId, stepId, Map.of());

            // Then
            assertThat(result).isCompleted();
            verify(sagaStepExecutor).executeStep(any(), eq(stepId), any());
        }

        @Test
        @DisplayName("Should skip conditional steps when condition not met")
        void shouldSkipConditionalStepsWhenConditionNotMet() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createActiveSagaState();
            sagaState.getStepResults().put("assess-credit-risk", Map.of("creditRiskResult", "REJECTED"));
            String stepId = "grant-loan-approval";
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));

            // When
            CompletableFuture<Map<String, Object>> result = sagaOrchestrator.executeStep(
                sagaId, stepId, Map.of());

            // Then
            assertThat(result).isCompleted();
            Map<String, Object> stepResult = result.get();
            assertThat(stepResult).containsEntry("skipped", true);
            assertThat(stepResult).containsEntry("reason", "Condition not met");
            
            verify(sagaStepExecutor, never()).executeStep(any(), eq(stepId), any());
        }
    }

    @Nested
    @DisplayName("SAGA Compensation Tests")
    class SagaCompensationTests {

        @Test
        @DisplayName("Should execute compensation for all completed steps")
        void shouldExecuteCompensationForAllCompletedSteps() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createFailedSagaState();
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));
            when(sagaStepExecutor.executeCompensation(any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(Map.of("compensated", true)));
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"compensation\":\"success\"}");
            when(kafkaTemplate.send(anyString(), eq(sagaId), anyString()))
                .thenReturn(mockFuture);

            // When
            CompletableFuture<Void> result = sagaOrchestrator.compensateSaga(sagaId);

            // Then
            assertThat(result).isCompleted();
            
            // Verify compensation was executed for completed steps
            verify(sagaStepExecutor, times(2)).executeCompensation(any(), anyString());
            verify(sagaStateStore).updateSagaState(argThat(state -> 
                state.getStatus() == KafkaSagaOrchestrator.SagaStatus.COMPENSATED));
        }

        @Test
        @DisplayName("Should handle compensation failure gracefully")
        void shouldHandleCompensationFailureGracefully() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createFailedSagaState();
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));
            when(sagaStepExecutor.executeCompensation(any(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Compensation failed")));

            // When
            CompletableFuture<Void> result = sagaOrchestrator.compensateSaga(sagaId);

            // Then
            assertThat(result).isCompletedExceptionally();
            verify(sagaStateStore).updateSagaState(argThat(state -> 
                state.getStatus() == KafkaSagaOrchestrator.SagaStatus.COMPENSATION_FAILED));
        }

        @Test
        @DisplayName("Should execute partial compensation for specific steps")
        void shouldExecutePartialCompensationForSpecificSteps() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createActiveSagaState();
            List<String> stepsToCompensate = List.of("verify-accounts", "assess-credit-risk");
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));
            when(sagaStepExecutor.executeCompensation(any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(Map.of("compensated", true)));

            // When
            CompletableFuture<Void> result = sagaOrchestrator.compensateSteps(sagaId, stepsToCompensate);

            // Then
            assertThat(result).isCompleted();
            verify(sagaStepExecutor, times(2)).executeCompensation(any(), anyString());
        }
    }

    @Nested
    @DisplayName("SAGA State Management Tests")
    class SagaStateManagementTests {

        @Test
        @DisplayName("Should retrieve SAGA state successfully")
        void shouldRetrieveSagaStateSuccessfully() {
            // Given
            KafkaSagaOrchestrator.SagaState expectedState = createActiveSagaState();
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(expectedState));

            // When
            Optional<KafkaSagaOrchestrator.SagaState> result = sagaOrchestrator.getSagaState(sagaId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expectedState);
        }

        @Test
        @DisplayName("Should return empty when SAGA state not found")
        void shouldReturnEmptyWhenSagaStateNotFound() {
            // Given
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.empty());

            // When
            Optional<KafkaSagaOrchestrator.SagaState> result = sagaOrchestrator.getSagaState(sagaId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should update SAGA status correctly")
        void shouldUpdateSagaStatusCorrectly() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createActiveSagaState();
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));
            when(sagaStateStore.updateSagaState(any())).thenReturn(sagaState);

            // When
            sagaOrchestrator.updateSagaStatus(sagaId, KafkaSagaOrchestrator.SagaStatus.COMPLETED);

            // Then
            ArgumentCaptor<KafkaSagaOrchestrator.SagaState> stateCaptor = 
                ArgumentCaptor.forClass(KafkaSagaOrchestrator.SagaState.class);
            verify(sagaStateStore).updateSagaState(stateCaptor.capture());
            
            KafkaSagaOrchestrator.SagaState updatedState = stateCaptor.getValue();
            assertThat(updatedState.getStatus()).isEqualTo(KafkaSagaOrchestrator.SagaStatus.COMPLETED);
            assertThat(updatedState.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should add step result to SAGA state")
        void shouldAddStepResultToSagaState() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createActiveSagaState();
            String stepId = "validate-customer";
            Map<String, Object> stepResult = Map.of("validationStatus", "APPROVED");
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));
            when(sagaStateStore.updateSagaState(any())).thenReturn(sagaState);

            // When
            sagaOrchestrator.addStepResult(sagaId, stepId, stepResult);

            // Then
            ArgumentCaptor<KafkaSagaOrchestrator.SagaState> stateCaptor = 
                ArgumentCaptor.forClass(KafkaSagaOrchestrator.SagaState.class);
            verify(sagaStateStore).updateSagaState(stateCaptor.capture());
            
            KafkaSagaOrchestrator.SagaState updatedState = stateCaptor.getValue();
            assertThat(updatedState.getStepResults()).containsEntry(stepId, stepResult);
        }
    }

    @Nested
    @DisplayName("SAGA Timeout Handling Tests")
    class SagaTimeoutHandlingTests {

        @Test
        @DisplayName("Should handle SAGA timeout and trigger compensation")
        void shouldHandleSagaTimeoutAndTriggerCompensation() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState sagaState = createTimedOutSagaState();
            
            when(sagaStateStore.getSagaState(sagaId)).thenReturn(Optional.of(sagaState));
            when(sagaStepExecutor.executeCompensation(any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(Map.of("compensated", true)));

            // When
            CompletableFuture<Void> result = sagaOrchestrator.handleTimeout(sagaId, 
                new TimeoutException("SAGA timeout exceeded"));

            // Then
            assertThat(result).isCompleted();
            verify(sagaStateStore).updateSagaState(argThat(state -> 
                state.getStatus() == KafkaSagaOrchestrator.SagaStatus.TIMED_OUT));
            verify(sagaStepExecutor, atLeastOnce()).executeCompensation(any(), anyString());
        }

        @Test
        @DisplayName("Should detect expired SAGAs correctly")
        void shouldDetectExpiredSagasCorrectly() {
            // Given
            KafkaSagaOrchestrator.SagaState activeSagaState = createActiveSagaState();
            KafkaSagaOrchestrator.SagaState expiredSagaState = createExpiredSagaState();
            
            when(sagaStateStore.findActiveSagas()).thenReturn(List.of(activeSagaState, expiredSagaState));

            // When
            List<String> expiredSagaIds = sagaOrchestrator.findExpiredSagas();

            // Then
            assertThat(expiredSagaIds).hasSize(1);
            assertThat(expiredSagaIds).contains(expiredSagaState.getSagaId());
        }
    }

    @Nested
    @DisplayName("SAGA Monitoring and Statistics Tests")
    class SagaMonitoringAndStatisticsTests {

        @Test
        @DisplayName("Should provide accurate SAGA statistics")
        void shouldProvideAccurateSagaStatistics() {
            // Given
            when(sagaStateStore.countSagasByStatus(KafkaSagaOrchestrator.SagaStatus.ACTIVE))
                .thenReturn(5L);
            when(sagaStateStore.countSagasByStatus(KafkaSagaOrchestrator.SagaStatus.COMPLETED))
                .thenReturn(100L);
            when(sagaStateStore.countSagasByStatus(KafkaSagaOrchestrator.SagaStatus.FAILED))
                .thenReturn(3L);
            when(sagaStateStore.getAverageExecutionTime()).thenReturn(Duration.ofMinutes(15));

            // When
            KafkaSagaOrchestrator.SagaStatistics stats = sagaOrchestrator.getStatistics();

            // Then
            assertThat(stats.getActiveSagas()).isEqualTo(5L);
            assertThat(stats.getCompletedSagas()).isEqualTo(100L);
            assertThat(stats.getFailedSagas()).isEqualTo(3L);
            assertThat(stats.getTotalSagas()).isEqualTo(108L);
            assertThat(stats.getSuccessRate()).isEqualTo(92.59); // 100/108 * 100
            assertThat(stats.getAverageExecutionTime()).isEqualTo(Duration.ofMinutes(15));
        }

        @Test
        @DisplayName("Should provide SAGA health status")
        void shouldProvideSagaHealthStatus() {
            // Given
            when(sagaStateStore.countSagasByStatus(KafkaSagaOrchestrator.SagaStatus.ACTIVE))
                .thenReturn(2L);
            when(sagaStateStore.countSagasByStatus(KafkaSagaOrchestrator.SagaStatus.FAILED))
                .thenReturn(1L);
            when(sagaStateStore.countSagasByStatus(KafkaSagaOrchestrator.SagaStatus.TIMED_OUT))
                .thenReturn(0L);

            // When
            KafkaSagaOrchestrator.HealthStatus health = sagaOrchestrator.getHealth();

            // Then
            assertThat(health.isHealthy()).isTrue();
            assertThat(health.getStatusMessage()).contains("SAGA orchestrator operating normally");
            assertThat(health.getActiveSagaCount()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should report unhealthy when too many failed SAGAs")
        void shouldReportUnhealthyWhenTooManyFailedSagas() {
            // Given
            when(sagaStateStore.countSagasByStatus(KafkaSagaOrchestrator.SagaStatus.ACTIVE))
                .thenReturn(5L);
            when(sagaStateStore.countSagasByStatus(KafkaSagaOrchestrator.SagaStatus.FAILED))
                .thenReturn(10L);
            when(sagaStateStore.countSagasByStatus(KafkaSagaOrchestrator.SagaStatus.TIMED_OUT))
                .thenReturn(5L);

            // When
            KafkaSagaOrchestrator.HealthStatus health = sagaOrchestrator.getHealth();

            // Then
            assertThat(health.isHealthy()).isFalse();
            assertThat(health.getStatusMessage()).contains("High failure rate detected");
        }
    }

    @Nested
    @DisplayName("SAGA Event Publishing Tests")
    class SagaEventPublishingTests {

        @Test
        @DisplayName("Should publish SAGA completion event")
        void shouldPublishSagaCompletionEvent() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState completedState = createCompletedSagaState();
            String expectedTopic = "banking.consumer-loan.saga.loanoriginationsaga";
            
            when(topicResolver.resolveSagaTopicForEvent(any(), eq("LoanOriginationSaga")))
                .thenReturn(expectedTopic);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"sagaCompleted\":true}");
            when(kafkaTemplate.send(eq(expectedTopic), eq(sagaId), anyString()))
                .thenReturn(mockFuture);

            // When
            CompletableFuture<Void> result = sagaOrchestrator.publishSagaCompletionEvent(completedState);

            // Then
            assertThat(result).isCompleted();
            verify(kafkaTemplate).send(expectedTopic, sagaId, anyString());
        }

        @Test
        @DisplayName("Should publish SAGA failure event")
        void shouldPublishSagaFailureEvent() throws Exception {
            // Given
            KafkaSagaOrchestrator.SagaState failedState = createFailedSagaState();
            String expectedTopic = "banking.consumer-loan.saga.loanoriginationsaga";
            
            when(topicResolver.resolveSagaTopicForEvent(any(), eq("LoanOriginationSaga")))
                .thenReturn(expectedTopic);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"sagaFailed\":true}");
            when(kafkaTemplate.send(eq(expectedTopic), eq(sagaId), anyString()))
                .thenReturn(mockFuture);

            // When
            CompletableFuture<Void> result = sagaOrchestrator.publishSagaFailureEvent(
                failedState, new RuntimeException("SAGA execution failed"));

            // Then
            assertThat(result).isCompleted();
            verify(kafkaTemplate).send(expectedTopic, sagaId, anyString());
        }
    }

    // Helper methods and test data

    private KafkaSagaOrchestrator.SagaState mockSagaState() {
        return KafkaSagaOrchestrator.SagaState.builder()
            .sagaId(sagaId)
            .sagaType("LoanOriginationSaga")
            .status(KafkaSagaOrchestrator.SagaStatus.ACTIVE)
            .createdAt(OffsetDateTime.now())
            .build();
    }

    private KafkaSagaOrchestrator.SagaState createActiveSagaState() {
        KafkaSagaOrchestrator.SagaState state = mockSagaState();
        state.getCompletedSteps().addAll(List.of("validate-customer", "verify-accounts"));
        state.getStepResults().put("validate-customer", Map.of("validationStatus", "APPROVED"));
        state.getStepResults().put("verify-accounts", Map.of("accountStatus", "VERIFIED"));
        return state;
    }

    private KafkaSagaOrchestrator.SagaState createFailedSagaState() {
        KafkaSagaOrchestrator.SagaState state = createActiveSagaState();
        state.setStatus(KafkaSagaOrchestrator.SagaStatus.FAILED);
        state.setFailedAt(OffsetDateTime.now());
        state.setLastError("Credit assessment failed");
        return state;
    }

    private KafkaSagaOrchestrator.SagaState createCompletedSagaState() {
        KafkaSagaOrchestrator.SagaState state = createActiveSagaState();
        state.setStatus(KafkaSagaOrchestrator.SagaStatus.COMPLETED);
        state.setCompletedAt(OffsetDateTime.now());
        state.getCompletedSteps().addAll(List.of("assess-credit-risk", "create-loan-arrangement", 
            "grant-loan-approval", "setup-disbursement", "execute-fulfillment", 
            "execute-payment", "notify-completion"));
        return state;
    }

    private KafkaSagaOrchestrator.SagaState createTimedOutSagaState() {
        KafkaSagaOrchestrator.SagaState state = createActiveSagaState();
        state.setStatus(KafkaSagaOrchestrator.SagaStatus.TIMED_OUT);
        state.setCreatedAt(OffsetDateTime.now().minusHours(3)); // Exceeds 2-hour timeout
        return state;
    }

    private KafkaSagaOrchestrator.SagaState createExpiredSagaState() {
        KafkaSagaOrchestrator.SagaState state = mockSagaState();
        state.setCreatedAt(OffsetDateTime.now().minusHours(4)); // Well past timeout
        return state;
    }

    // Test domain event implementation
    private static class TestDomainEvent extends DomainEvent {
        private final String testData;

        public TestDomainEvent(String aggregateId, long version, String testData) {
            super(aggregateId, "TestAggregate", version);
            this.testData = testData;
        }

        @Override
        public String getEventType() {
            return "TestDomainEvent";
        }

        @Override
        public Object getEventData() {
            return Map.of("testData", testData);
        }

        @Override
        public String getServiceDomain() {
            return "TestDomain";
        }

        @Override
        public String getBehaviorQualifier() {
            return "TEST";
        }
    }
}