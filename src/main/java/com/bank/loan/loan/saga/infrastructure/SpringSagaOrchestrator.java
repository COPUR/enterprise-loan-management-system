package com.bank.loanmanagement.loan.saga.infrastructure;

import com.bank.loanmanagement.loan.saga.domain.SagaDefinition;
import com.bank.loanmanagement.loan.saga.domain.SagaOrchestrator;
import com.bank.loanmanagement.loan.saga.domain.SagaExecution;
import com.bank.loanmanagement.loan.saga.domain.SagaStep;
import com.bank.loanmanagement.loan.saga.domain.LoanOriginationSaga;
import com.bank.loan.loan.sharedkernel.domain.event.DomainEvent;
import com.bank.loanmanagement.loan.sharedkernel.infrastructure.eventstore.EventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Spring-based SAGA Orchestrator implementation
 * Provides distributed transaction coordination with event-driven architecture
 * Integrates with Spring's transaction management and async processing
 * Ensures BIAN compliance and FAPI security throughout SAGA execution
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpringSagaOrchestrator<T extends SagaDefinition> implements SagaOrchestrator<T> {

    private final SagaExecutionRepository sagaExecutionRepository;
    private final SagaStepRepository sagaStepRepository;
    private final EventStore eventStore;
    private final ApplicationEventPublisher eventPublisher;
    private final SagaStepExecutor sagaStepExecutor;
    
    // In-memory cache for active SAGAs (could be replaced with Redis for clustering)
    private final Map<String, SagaExecutionImpl<T>> activeSagas = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public SagaExecution<T> startSaga(T sagaDefinition, Map<String, Object> initialData) {
        String sagaId = UUID.randomUUID().toString();
        log.info("Starting SAGA {} of type {}", sagaId, sagaDefinition.getSagaType());

        // Validate compliance before starting
        if (sagaDefinition instanceof LoanOriginationSaga loanSaga) {
            var validationResult = loanSaga.validateCompliance();
            if (!validationResult.isValid()) {
                throw new SagaOrchestrator.SagaOrchestrationException(
                    "SAGA definition validation failed: " + validationResult.message(),
                    sagaId, null, SagaOrchestrator.SagaStatus.FAILED
                );
            }
        }

        // Extract FAPI security context from initial data
        String fapiInteractionId = (String) initialData.get("fapiInteractionId");
        String clientId = (String) initialData.get("clientId");
        String correlationId = (String) initialData.getOrDefault("correlationId", UUID.randomUUID().toString());

        // Create SAGA execution
        SagaExecutionImpl<T> execution = new SagaExecutionImpl<>(
            sagaId,
            sagaDefinition,
            SagaOrchestrator.SagaStatus.INITIATED,
            initialData,
            fapiInteractionId,
            clientId,
            correlationId
        );

        // Save to repository
        SagaExecutionEntity entity = convertToEntity(execution);
        sagaExecutionRepository.save(entity);

        // Add to active cache
        activeSagas.put(sagaId, execution);

        // Start first step asynchronously
        scheduleNextStep(execution);

        log.info("SAGA {} started successfully with {} steps", sagaId, sagaDefinition.getSteps().size());
        return execution;
    }

    @Override
    @Transactional
    public SagaExecution<T> processEvent(String sagaId, DomainEvent event) {
        log.debug("Processing event {} for SAGA {}", event.getEventType(), sagaId);

        SagaExecutionImpl<T> execution = getSagaExecutionImpl(sagaId);
        if (execution == null) {
            log.warn("SAGA {} not found for event processing", sagaId);
            return null;
        }

        // Update SAGA based on event
        // This is where you would implement event-based SAGA progression
        // For now, we'll log the event and continue with step-based progression
        log.info("Received event {} for SAGA {} in status {}", 
                event.getEventType(), sagaId, execution.getStatus());

        return execution;
    }

    @Override
    @Async
    @Transactional
    public SagaExecution<T> handleStepCompletion(String sagaId, String stepId, Object stepResult) {
        log.info("Handling step completion for SAGA {} step {}", sagaId, stepId);

        SagaExecutionImpl<T> execution = getSagaExecutionImpl(sagaId);
        if (execution == null) {
            throw new SagaOrchestrator.SagaOrchestrationException(
                "SAGA not found", sagaId, stepId, SagaOrchestrator.SagaStatus.FAILED
            );
        }

        // Update step status
        SagaStepImpl step = execution.getCurrentStep();
        if (step != null && step.getStepId().equals(stepId)) {
            step.complete(stepResult);
            execution.addCompletedStep(step);

            // Update SAGA data with step result
            execution.updateSagaData(stepId + "_result", stepResult);

            // Save progress
            saveSagaExecution(execution);

            // Schedule next step or complete SAGA
            if (hasMoreSteps(execution)) {
                scheduleNextStep(execution);
            } else {
                completeSaga(execution);
            }
        }

        return execution;
    }

    @Override
    @Transactional
    public SagaExecution<T> handleStepFailure(String sagaId, String stepId, Exception failure) {
        log.error("Handling step failure for SAGA {} step {}: {}", sagaId, stepId, failure.getMessage());

        SagaExecutionImpl<T> execution = getSagaExecutionImpl(sagaId);
        if (execution == null) {
            throw new SagaOrchestrator.SagaOrchestrationException(
                "SAGA not found", sagaId, stepId, SagaOrchestrator.SagaStatus.FAILED
            );
        }

        // Update step status
        SagaStepImpl step = execution.getCurrentStep();
        if (step != null && step.getStepId().equals(stepId)) {
            step.fail(failure);
            execution.addFailedStep(step);

            // Determine if step should be retried
            if (shouldRetryStep(step, execution.getSagaDefinition().getRetryPolicy())) {
                log.info("Retrying step {} for SAGA {} (attempt {})", 
                        stepId, sagaId, step.getRetryCount() + 1);
                retryStep(execution, step);
            } else {
                log.info("Step {} failed permanently for SAGA {}, starting compensation", stepId, sagaId);
                startCompensation(execution, "Step " + stepId + " failed: " + failure.getMessage());
            }
        }

        return execution;
    }

    @Override
    @Transactional
    public SagaExecution<T> compensate(String sagaId, String reason) {
        log.info("Starting compensation for SAGA {} - reason: {}", sagaId, reason);

        SagaExecutionImpl<T> execution = getSagaExecutionImpl(sagaId);
        if (execution == null) {
            throw new SagaOrchestrator.SagaOrchestrationException(
                "SAGA not found", sagaId, null, SagaOrchestrator.SagaStatus.FAILED
            );
        }

        startCompensation(execution, reason);
        return execution;
    }

    @Override
    @Transactional(readOnly = true)
    public SagaExecution<T> getSagaExecution(String sagaId) {
        return getSagaExecutionImpl(sagaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SagaExecution<T>> getActiveSagas() {
        return new ArrayList<SagaExecution<T>>(activeSagas.values());
    }

    @Override
    @Transactional
    public List<SagaExecution<T>> handleTimeouts() {
        OffsetDateTime now = OffsetDateTime.now();
        List<SagaExecution<T>> timedOutSagas = new ArrayList<>();

        for (SagaExecutionImpl<T> execution : activeSagas.values()) {
            if (execution.getTimeoutAt() != null && now.isAfter(execution.getTimeoutAt())) {
                log.warn("SAGA {} timed out", execution.getSagaId());
                execution.timeout();
                startCompensation(execution, "SAGA timeout");
                timedOutSagas.add((SagaExecution<T>) execution);
            }
        }

        return timedOutSagas;
    }

    @Async
    private CompletableFuture<Void> scheduleNextStep(SagaExecutionImpl<T> execution) {
        return CompletableFuture.runAsync(() -> {
            try {
                SagaDefinition.SagaStepDefinition nextStepDef = findNextStep(execution);
                if (nextStepDef != null) {
                    // Check execution condition
                    if (nextStepDef.getExecutionCondition().test(execution.getSagaData())) {
                        SagaStepImpl nextStep = new SagaStepImpl(nextStepDef);
                        execution.setCurrentStep(nextStep);
                        execution.setStatus(SagaOrchestrator.SagaStatus.RUNNING);
                        
                        // Execute step
                        sagaStepExecutor.executeStep(execution.getSagaDefinition(), nextStep.getStepId(), nextStep.getStepData());
                    } else {
                        log.info("Skipping step {} for SAGA {} due to execution condition", 
                                nextStepDef.getStepId(), execution.getSagaId());
                        // Move to next step
                        scheduleNextStep(execution);
                    }
                } else {
                    completeSaga(execution);
                }
            } catch (Exception e) {
                log.error("Error scheduling next step for SAGA {}", execution.getSagaId(), e);
                handleStepFailure(execution.getSagaId(), 
                                execution.getCurrentStepId(), e);
            }
        });
    }

    private void startCompensation(SagaExecutionImpl<T> execution, String reason) {
        execution.setStatus(SagaOrchestrator.SagaStatus.COMPENSATING);
        execution.setFailureReason(reason);
        
        // Execute compensation actions in reverse order
        List<SagaStepImpl> completedSteps = new ArrayList<>(execution.getCompletedSteps());
        Collections.reverse(completedSteps);
        
        for (SagaStepImpl step : completedSteps) {
            if (step.isCompensationRequired()) {
                sagaStepExecutor.executeCompensation(execution.getSagaDefinition(), step.getStepId());
            }
        }
        
        execution.setStatus(SagaOrchestrator.SagaStatus.COMPENSATED);
        execution.setCompletedAt(OffsetDateTime.now());
        activeSagas.remove(execution.getSagaId());
        saveSagaExecution(execution);
    }

    private void completeSaga(SagaExecutionImpl<T> execution) {
        log.info("Completing SAGA {}", execution.getSagaId());
        execution.setStatus(SagaOrchestrator.SagaStatus.COMPLETED);
        execution.setCompletedAt(OffsetDateTime.now());
        activeSagas.remove(execution.getSagaId());
        saveSagaExecution(execution);
        
        // Publish SAGA completion event
        publishSagaCompletionEvent(execution);
    }

    private void publishSagaCompletionEvent(SagaExecutionImpl<T> execution) {
        // Create and publish domain event for SAGA completion
        // This would integrate with your event publishing infrastructure
        log.info("SAGA {} completed successfully", execution.getSagaId());
    }

    private SagaExecutionImpl<T> getSagaExecutionImpl(String sagaId) {
        // First check cache
        SagaExecutionImpl<T> execution = activeSagas.get(sagaId);
        if (execution != null) {
            return execution;
        }

        // Load from repository
        Optional<SagaExecutionEntity> entity = sagaExecutionRepository.findById(sagaId);
        if (entity.isPresent()) {
            execution = convertFromEntity(entity.get());
            if (execution.getStatus() == SagaOrchestrator.SagaStatus.RUNNING || 
                execution.getStatus() == SagaOrchestrator.SagaStatus.COMPENSATING) {
                activeSagas.put(sagaId, execution);
            }
            return execution;
        }

        return null;
    }

    private boolean hasMoreSteps(SagaExecutionImpl<T> execution) {
        return findNextStep(execution) != null;
    }

    private SagaDefinition.SagaStepDefinition findNextStep(SagaExecutionImpl<T> execution) {
        List<String> completedStepIds = execution.getCompletedSteps().stream()
                .map(SagaStep::getStepId)
                .collect(Collectors.toList());

        return execution.getSagaDefinition().getSteps().stream()
                .filter(step -> !completedStepIds.contains(step.getStepId()))
                .filter(step -> step.getDependsOnSteps().stream()
                        .allMatch(completedStepIds::contains))
                .findFirst()
                .orElse(null);
    }

    private boolean shouldRetryStep(SagaStepImpl step, SagaDefinition.RetryPolicy retryPolicy) {
        return step.getRetryCount() < retryPolicy.getMaxRetries() &&
               retryPolicy.getRetryableExceptions().contains(step.getFailureReason().getClass());
    }

    @Async
    private CompletableFuture<Void> retryStep(SagaExecutionImpl<T> execution, SagaStepImpl step) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Calculate retry delay
                long delayMs = calculateRetryDelay(step.getRetryCount(), 
                                                 execution.getSagaDefinition().getRetryPolicy());
                Thread.sleep(delayMs);
                
                // Retry step execution
                step.retry();
                sagaStepExecutor.executeStep(execution.getSagaDefinition(), step.getStepId(), step.getStepData());
            } catch (Exception e) {
                handleStepFailure(execution.getSagaId(), step.getStepId(), e);
            }
        });
    }

    private long calculateRetryDelay(int retryCount, SagaDefinition.RetryPolicy retryPolicy) {
        long delay = retryPolicy.getInitialDelay().toMillis();
        for (int i = 0; i < retryCount; i++) {
            delay = (long) (delay * retryPolicy.getBackoffMultiplier());
        }
        return Math.min(delay, retryPolicy.getMaxDelay().toMillis());
    }

    private void saveSagaExecution(SagaExecutionImpl<T> execution) {
        SagaExecutionEntity entity = convertToEntity(execution);
        sagaExecutionRepository.save(entity);
    }

    private SagaExecutionEntity convertToEntity(SagaExecutionImpl<T> execution) {
        // Convert SAGA execution to JPA entity for persistence
        // This is a simplified implementation
        return SagaExecutionEntity.builder()
                .sagaId(execution.getSagaId())
                .sagaType(execution.getSagaDefinition().getSagaType())
                .status(execution.getStatus().name())
                .currentStepId(execution.getCurrentStepId())
                .sagaData("{}")  // Would serialize execution.getSagaData()
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .failureReason(execution.getFailureReason())
                .retryCount(execution.getRetryCount())
                .timeoutAt(execution.getTimeoutAt())
                .fapiInteractionId(execution.getFapiInteractionId())
                .clientId(execution.getClientId())
                .correlationId(execution.getCorrelationId())
                .build();
    }

    @SuppressWarnings("unchecked")
    private SagaExecutionImpl<T> convertFromEntity(SagaExecutionEntity entity) {
        // Convert JPA entity back to SAGA execution
        // This is a simplified implementation - would need proper deserialization
        return new SagaExecutionImpl<>(
                entity.getSagaId(),
                null, // Would need to reconstruct saga definition
                SagaOrchestrator.SagaStatus.valueOf(entity.getStatus()),
                Map.of(), // Would deserialize saga data
                entity.getFapiInteractionId(),
                entity.getClientId(),
                entity.getCorrelationId()
        );
    }

    // JPA Entity for SAGA persistence
    @Entity
    @Table(name = "saga_executions")
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SagaExecutionEntity {
        @Id
        private String sagaId;
        private String sagaType;
        private String status;
        private String currentStepId;
        @Lob
        private String sagaData;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;
        private String failureReason;
        private Integer retryCount;
        private OffsetDateTime timeoutAt;
        private String fapiInteractionId;
        private String clientId;
        private String correlationId;
    }

    @Repository
    public interface SagaExecutionRepository extends org.springframework.data.jpa.repository.JpaRepository<SagaExecutionEntity, String> {
        List<SagaExecutionEntity> findByStatus(String status);
        List<SagaExecutionEntity> findByTimeoutAtBefore(OffsetDateTime timestamp);
    }

    @Repository
    public interface SagaStepRepository extends org.springframework.data.jpa.repository.JpaRepository<SagaStepEntity, String> {
        List<SagaStepEntity> findBySagaId(String sagaId);
    }

    @Entity
    @Table(name = "saga_steps")
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SagaStepEntity {
        @Id
        private String stepId;
        private String sagaId;
        private String stepType;
        private String status;
        @Lob
        private String stepData;
        @Lob
        private String stepResult;
        private String failureReason;
        private OffsetDateTime startedAt;
        private OffsetDateTime completedAt;
        private Integer retryCount;
        private String targetServiceDomain;
        private String behaviorQualifier;
    }
}