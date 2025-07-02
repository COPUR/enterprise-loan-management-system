package com.bank.loanmanagement.loan.saga.infrastructure;

import com.bank.loanmanagement.loan.messaging.infrastructure.kafka.KafkaEventPublisher;
import com.bank.loanmanagement.loan.messaging.infrastructure.kafka.KafkaTopicResolver;
import com.bank.loanmanagement.loan.saga.domain.SagaDefinition;
import com.bank.loanmanagement.loan.saga.domain.SagaOrchestrator;
import com.bank.loanmanagement.loan.saga.domain.SagaExecution;
import com.bank.loan.loan.sharedkernel.domain.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka-based SAGA Orchestrator implementation
 * Provides distributed transaction coordination using Kafka for event-driven SAGA execution
 * Integrates with BIAN service domains and ensures Berlin Group compliance
 * Implements compensation patterns with reliable event delivery
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaSagaOrchestrator<T extends SagaDefinition> implements SagaOrchestrator<T> {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final KafkaTopicResolver topicResolver;
    private final ObjectMapper objectMapper;
    private final SagaStateStore sagaStateStore;
    
    // In-memory cache for active SAGAs (could be replaced with Redis for clustering)
    private final Map<String, KafkaSagaExecution<T>> activeSagas = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public SagaExecution<T> startSaga(T sagaDefinition, Map<String, Object> initialData) {
        String sagaId = UUID.randomUUID().toString();
        log.info("Starting Kafka-based SAGA {} of type {}", sagaId, sagaDefinition.getSagaType());

        // Extract FAPI security context
        String fapiInteractionId = (String) initialData.get("fapiInteractionId");
        String clientId = (String) initialData.get("clientId");
        String correlationId = UUID.randomUUID().toString();

        // Create SAGA execution
        KafkaSagaExecution<T> execution = new KafkaSagaExecution<>(
            sagaId,
            sagaDefinition,
            SagaOrchestrator.SagaStatus.INITIATED,
            new HashMap<>(initialData),
            fapiInteractionId,
            clientId,
            correlationId
        );

        // Store SAGA state
        sagaStateStore.storeSagaState(execution);
        activeSagas.put(sagaId, execution);

        // Publish SAGA started event
        publishSagaEvent(execution, "SagaStarted", Map.of(
            "sagaType", sagaDefinition.getSagaType(),
            "totalSteps", sagaDefinition.getSteps().size()
        ));

        // Schedule first step
        scheduleNextStep(execution);

        log.info("Kafka SAGA {} started successfully", sagaId);
        return execution;
    }

    @Override
    public SagaExecution<T> processEvent(String sagaId, DomainEvent event) {
        log.debug("Processing event {} for Kafka SAGA {}", event.getEventType(), sagaId);

        KafkaSagaExecution<T> execution = getOrLoadSagaExecution(sagaId);
        if (execution == null) {
            log.warn("Kafka SAGA {} not found for event processing", sagaId);
            return null;
        }

        // Process event based on current SAGA state
        processEventForSaga(execution, event);
        
        return execution;
    }

    @Override
    public SagaExecution<T> handleStepCompletion(String sagaId, String stepId, Object stepResult) {
        log.info("Handling step completion for Kafka SAGA {} step {}", sagaId, stepId);

        KafkaSagaExecution<T> execution = getOrLoadSagaExecution(sagaId);
        if (execution == null) {
            throw new SagaOrchestrator.SagaOrchestrationException(
                "Kafka SAGA not found", sagaId, stepId, SagaOrchestrator.SagaStatus.FAILED
            );
        }

        // Update step state
        execution.completeStep(stepId, stepResult);
        sagaStateStore.updateSagaState(execution);

        // Publish step completion event
        publishSagaEvent(execution, "StepCompleted", Map.of(
            "stepId", stepId,
            "stepResult", stepResult != null ? stepResult.toString() : "null"
        ));

        // Schedule next step or complete SAGA
        if (hasMoreSteps(execution)) {
            scheduleNextStep(execution);
        } else {
            completeSaga(execution);
        }

        return execution;
    }

    @Override
    public SagaExecution<T> handleStepFailure(String sagaId, String stepId, Exception failure) {
        log.error("Handling step failure for Kafka SAGA {} step {}: {}", sagaId, stepId, failure.getMessage());

        KafkaSagaExecution<T> execution = getOrLoadSagaExecution(sagaId);
        if (execution == null) {
            throw new SagaOrchestrator.SagaOrchestrationException(
                "Kafka SAGA not found", sagaId, stepId, SagaOrchestrator.SagaStatus.FAILED
            );
        }

        // Update step state
        execution.failStep(stepId, failure);
        sagaStateStore.updateSagaState(execution);

        // Publish step failure event
        publishSagaEvent(execution, "StepFailed", Map.of(
            "stepId", stepId,
            "failureReason", failure.getMessage(),
            "retryCount", execution.getRetryCount()
        ));

        // Determine retry or compensation
        if (shouldRetryStep(execution, stepId)) {
            scheduleStepRetry(execution, stepId);
        } else {
            startCompensation(execution, "Step " + stepId + " failed: " + failure.getMessage());
        }

        return execution;
    }

    @Override
    @Transactional
    public SagaExecution<T> compensate(String sagaId, String reason) {
        log.info("Starting compensation for Kafka SAGA {} - reason: {}", sagaId, reason);

        KafkaSagaExecution<T> execution = getOrLoadSagaExecution(sagaId);
        if (execution == null) {
            throw new SagaOrchestrator.SagaOrchestrationException(
                "Kafka SAGA not found", sagaId, null, SagaOrchestrator.SagaStatus.FAILED
            );
        }

        startCompensation(execution, reason);
        return execution;
    }

    @Override
    public SagaExecution<T> getSagaExecution(String sagaId) {
        return getOrLoadSagaExecution(sagaId);
    }

    @Override
    public List<SagaExecution<T>> getActiveSagas() {
        return new ArrayList<SagaExecution<T>>(activeSagas.values());
    }

    @Override
    public List<SagaExecution<T>> handleTimeouts() {
        OffsetDateTime now = OffsetDateTime.now();
        List<SagaExecution<T>> timedOutSagas = new ArrayList<>();

        for (KafkaSagaExecution<T> execution : activeSagas.values()) {
            if (execution.getTimeoutAt() != null && now.isAfter(execution.getTimeoutAt())) {
                log.warn("Kafka SAGA {} timed out", execution.getSagaId());
                execution.timeout();
                
                // Publish timeout event
                publishSagaEvent(execution, "SagaTimedOut", Map.of(
                    "timeoutAt", execution.getTimeoutAt().toString()
                ));
                
                startCompensation(execution, "SAGA timeout");
                timedOutSagas.add((SagaExecution<T>) execution);
            }
        }

        return timedOutSagas;
    }

    private void scheduleNextStep(KafkaSagaExecution<T> execution) {
        SagaDefinition.SagaStepDefinition nextStep = findNextStep(execution);
        if (nextStep != null) {
            // Check execution condition
            if (nextStep.getExecutionCondition().test(execution.getSagaData())) {
                execution.setCurrentStep(nextStep.getStepId());
                execution.setStatus(SagaOrchestrator.SagaStatus.RUNNING);
                sagaStateStore.updateSagaState(execution);

                // Publish step scheduling event to appropriate service domain topic
                publishStepExecutionEvent(execution, nextStep);
            } else {
                log.info("Skipping step {} for Kafka SAGA {} due to execution condition", 
                        nextStep.getStepId(), execution.getSagaId());
                // Skip step and move to next
                execution.skipStep(nextStep.getStepId());
                scheduleNextStep(execution);
            }
        } else {
            completeSaga(execution);
        }
    }

    private void publishStepExecutionEvent(KafkaSagaExecution<T> execution, SagaDefinition.SagaStepDefinition step) {
        try {
            // Create step execution command event
            SagaStepExecutionEvent stepEvent = SagaStepExecutionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sagaId(execution.getSagaId())
                    .stepId(step.getStepId())
                    .stepType(step.getStepType())
                    .targetServiceDomain(step.getTargetServiceDomain())
                    .behaviorQualifier(step.getBehaviorQualifier())
                    .serviceOperation(step.getServiceOperation())
                    .stepConfiguration(step.getStepConfiguration())
                    .sagaData(execution.getSagaData())
                    .stepTimeout(step.getStepTimeout())
                    .maxRetries(step.getMaxRetries())
                    .occurredOn(OffsetDateTime.now())
                    .fapiInteractionId(execution.getFapiInteractionId())
                    .correlationId(execution.getCorrelationId())
                    .build();

            // Determine target topic based on service domain
            String topic = topicResolver.resolveSagaTopicForEvent(stepEvent, execution.getSagaDefinition().getSagaType());
            
            // Serialize and publish
            String eventJson = objectMapper.writeValueAsString(stepEvent);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                topic, 
                execution.getSagaId(), // Use saga ID as partition key for ordering
                eventJson
            );

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Published step execution event for SAGA {} step {} to topic {}", 
                            execution.getSagaId(), step.getStepId(), topic);
                } else {
                    log.error("Failed to publish step execution event for SAGA {} step {}: {}", 
                             execution.getSagaId(), step.getStepId(), ex.getMessage(), ex);
                    handleStepFailure(execution.getSagaId(), step.getStepId(), 
                                    new RuntimeException("Failed to publish step execution event", ex));
                }
            });

        } catch (Exception e) {
            log.error("Error creating step execution event for SAGA {} step {}", 
                     execution.getSagaId(), step.getStepId(), e);
            handleStepFailure(execution.getSagaId(), step.getStepId(), e);
        }
    }

    private void startCompensation(KafkaSagaExecution<T> execution, String reason) {
        execution.setStatus(SagaOrchestrator.SagaStatus.COMPENSATING);
        execution.setFailureReason(reason);
        sagaStateStore.updateSagaState(execution);

        // Publish compensation started event
        publishSagaEvent(execution, "CompensationStarted", Map.of(
            "reason", reason,
            "completedSteps", execution.getCompletedStepIds().size()
        ));

        // Execute compensation actions in reverse order
        List<String> completedStepIds = new ArrayList<>(execution.getCompletedStepIds());
        Collections.reverse(completedStepIds);

        for (String stepId : completedStepIds) {
            SagaDefinition.SagaStepDefinition stepDef = findStepDefinition(execution.getSagaDefinition(), stepId);
            if (stepDef != null && stepDef.isCompensationRequired()) {
                publishCompensationEvent(execution, stepDef);
            }
        }

        // Mark as compensated
        execution.setStatus(SagaOrchestrator.SagaStatus.COMPENSATED);
        execution.setCompletedAt(OffsetDateTime.now());
        activeSagas.remove(execution.getSagaId());
        sagaStateStore.updateSagaState(execution);

        publishSagaEvent(execution, "SagaCompensated", Map.of(
            "reason", reason
        ));
    }

    private void publishCompensationEvent(KafkaSagaExecution<T> execution, SagaDefinition.SagaStepDefinition step) {
        try {
            SagaCompensationEvent compensationEvent = SagaCompensationEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sagaId(execution.getSagaId())
                    .stepId(step.getStepId())
                    .compensationAction(step.getCompensationAction())
                    .targetServiceDomain(step.getTargetServiceDomain())
                    .compensationTimeout(step.getCompensationTimeout())
                    .sagaData(execution.getSagaData())
                    .occurredOn(OffsetDateTime.now())
                    .fapiInteractionId(execution.getFapiInteractionId())
                    .correlationId(execution.getCorrelationId())
                    .build();

            String topic = topicResolver.resolveSagaTopicForEvent(compensationEvent, execution.getSagaDefinition().getSagaType());
            String eventJson = objectMapper.writeValueAsString(compensationEvent);

            kafkaTemplate.send(topic, execution.getSagaId(), eventJson);
            
            log.info("Published compensation event for SAGA {} step {}", 
                    execution.getSagaId(), step.getStepId());

        } catch (Exception e) {
            log.error("Failed to publish compensation event for SAGA {} step {}", 
                     execution.getSagaId(), step.getStepId(), e);
        }
    }

    private void completeSaga(KafkaSagaExecution<T> execution) {
        log.info("Completing Kafka SAGA {}", execution.getSagaId());
        
        execution.setStatus(SagaOrchestrator.SagaStatus.COMPLETED);
        execution.setCompletedAt(OffsetDateTime.now());
        activeSagas.remove(execution.getSagaId());
        sagaStateStore.updateSagaState(execution);

        // Publish SAGA completion event
        publishSagaEvent(execution, "SagaCompleted", Map.of(
            "totalSteps", execution.getSagaDefinition().getSteps().size(),
            "completedSteps", execution.getCompletedStepIds().size(),
            "duration", java.time.Duration.between(execution.getStartedAt(), execution.getCompletedAt()).toString()
        ));
    }

    private void publishSagaEvent(KafkaSagaExecution<T> execution, String eventType, Map<String, Object> eventData) {
        try {
            SagaEvent sagaEvent = SagaEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .sagaId(execution.getSagaId())
                    .eventType(eventType)
                    .sagaType(execution.getSagaDefinition().getSagaType())
                    .sagaStatus(execution.getStatus())
                    .eventData(eventData)
                    .occurredOn(OffsetDateTime.now())
                    .fapiInteractionId(execution.getFapiInteractionId())
                    .correlationId(execution.getCorrelationId())
                    .build();

            kafkaEventPublisher.publishSagaEvent(sagaEvent, execution.getSagaId(), execution.getCurrentStepId());
            
        } catch (Exception e) {
            log.error("Failed to publish SAGA event {} for SAGA {}", eventType, execution.getSagaId(), e);
        }
    }

    private KafkaSagaExecution<T> getOrLoadSagaExecution(String sagaId) {
        // Check cache first
        KafkaSagaExecution<T> execution = activeSagas.get(sagaId);
        if (execution != null) {
            return execution;
        }

        // Load from state store
        execution = sagaStateStore.loadSagaState(sagaId);
        if (execution != null && (execution.getStatus() == SagaOrchestrator.SagaStatus.RUNNING || 
                                 execution.getStatus() == SagaOrchestrator.SagaStatus.COMPENSATING)) {
            activeSagas.put(sagaId, execution);
        }

        return execution;
    }

    private boolean hasMoreSteps(KafkaSagaExecution<T> execution) {
        return findNextStep(execution) != null;
    }

    private SagaDefinition.SagaStepDefinition findNextStep(KafkaSagaExecution<T> execution) {
        Set<String> completedStepIds = execution.getCompletedStepIds();
        Set<String> skippedStepIds = execution.getSkippedSteps();

        return execution.getSagaDefinition().getSteps().stream()
                .filter(step -> !completedStepIds.contains(step.getStepId()))
                .filter(step -> !skippedStepIds.contains(step.getStepId()))
                .filter(step -> step.getDependsOnSteps().stream()
                        .allMatch(dep -> completedStepIds.contains(dep) || skippedStepIds.contains(dep)))
                .findFirst()
                .orElse(null);
    }

    private SagaDefinition.SagaStepDefinition findStepDefinition(T sagaDefinition, String stepId) {
        return sagaDefinition.getSteps().stream()
                .filter(step -> step.getStepId().equals(stepId))
                .findFirst()
                .orElse(null);
    }

    private boolean shouldRetryStep(KafkaSagaExecution<T> execution, String stepId) {
        SagaDefinition.SagaStepDefinition stepDef = findStepDefinition(execution.getSagaDefinition(), stepId);
        return stepDef != null && execution.getRetryCount() < stepDef.getMaxRetries();
    }

    private void scheduleStepRetry(KafkaSagaExecution<T> execution, String stepId) {
        execution.incrementRetryCount();
        sagaStateStore.updateSagaState(execution);

        // Schedule retry after delay
        SagaDefinition.SagaStepDefinition stepDef = findStepDefinition(execution.getSagaDefinition(), stepId);
        if (stepDef != null) {
            // In a real implementation, you would schedule this with a delay
            publishStepExecutionEvent(execution, stepDef);
        }
    }

    private void processEventForSaga(KafkaSagaExecution<T> execution, DomainEvent event) {
        // Process domain events that affect SAGA state
        // This could trigger step completion, step failure, or other SAGA state changes
        log.info("Processing domain event {} for SAGA {}", event.getEventType(), execution.getSagaId());
    }

    // Event classes for Kafka messaging
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.EqualsAndHashCode(callSuper=false)
    public static class SagaStepExecutionEvent extends DomainEvent {
        private String sagaId;
        private String stepId;
        private String stepType;
        private String targetServiceDomain;
        private String behaviorQualifier;
        private String serviceOperation;
        private Map<String, Object> stepConfiguration;
        private Map<String, Object> sagaData;
        private java.time.Duration stepTimeout;
        private int maxRetries;
        private String fapiInteractionId;
        private String correlationId;

        @Override public String getEventType() { return "SagaStepExecution"; }
        @Override public Object getEventData() { return this; }
        @Override public String getServiceDomain() { return targetServiceDomain; }
        @Override public String getBehaviorQualifier() { return behaviorQualifier; }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.EqualsAndHashCode(callSuper=false)
    public static class SagaCompensationEvent extends DomainEvent {
        private String sagaId;
        private String stepId;
        private String compensationAction;
        private String targetServiceDomain;
        private java.time.Duration compensationTimeout;
        private Map<String, Object> sagaData;
        private String fapiInteractionId;
        private String correlationId;

        @Override public String getEventType() { return "SagaCompensation"; }
        @Override public Object getEventData() { return this; }
        @Override public String getServiceDomain() { return targetServiceDomain; }
        @Override public String getBehaviorQualifier() { return "CONTROL"; }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.EqualsAndHashCode(callSuper=false)
    public static class SagaEvent extends DomainEvent {
        private String sagaId;
        private String sagaType;
        private SagaStatus sagaStatus;
        private Map<String, Object> eventData;
        private String fapiInteractionId;
        private String correlationId;

        @Override public String getEventType() { return super.getEventType(); }
        @Override public Object getEventData() { return eventData; }
        @Override public String getServiceDomain() { return "SagaOrchestration"; }
        @Override public String getBehaviorQualifier() { return "NOTIFY"; }
    }
}