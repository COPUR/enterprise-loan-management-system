package com.bank.loanmanagement.saga;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loan Creation SAGA Orchestrator
 * Implements distributed transaction pattern across Customer, Loan, and Payment microservices
 */
@Service
@Slf4j
public class LoanCreationSagaOrchestrator {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private SagaStateRepository sagaStateRepository;
    
    // In-memory saga state for high-performance lookup
    private final Map<String, SagaState> activeSagas = new ConcurrentHashMap<>();

    /**
     * Step 1: Handle Loan Application Submitted Event
     */
    @KafkaListener(topics = "loan-saga-events", groupId = "saga-orchestrator")
    @Transactional
    public void handleLoanApplicationSubmitted(LoanApplicationSubmittedEvent event) {
        log.info("Starting Loan Creation SAGA for application: {}", event.getApplicationId());
        
        String sagaId = "loan-creation-" + event.getApplicationId();
        
        SagaState sagaState = SagaState.builder()
            .sagaId(sagaId)
            .sagaType(SagaType.LOAN_CREATION)
            .status(SagaStatus.STARTED)
            .currentStep(SagaStep.VALIDATE_CUSTOMER)
            .customerId(event.getCustomerId())
            .requestedAmount(event.getRequestedAmount())
            .interestRate(event.getInterestRate())
            .numberOfInstallments(event.getNumberOfInstallments())
            .applicationId(event.getApplicationId())
            .startedAt(LocalDateTime.now())
            .build();
        
        activeSagas.put(sagaId, sagaState);
        sagaStateRepository.save(sagaState);
        
        // Step 1: Validate Customer Exists
        ValidateCustomerCommand command = ValidateCustomerCommand.builder()
            .sagaId(sagaId)
            .customerId(event.getCustomerId())
            .commandId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("customer-commands", command);
        log.info("Sent ValidateCustomerCommand for SAGA: {}", sagaId);
    }

    /**
     * Step 2: Handle Customer Validated Event
     */
    @KafkaListener(topics = "customer-events", groupId = "saga-orchestrator")
    @Transactional
    public void handleCustomerValidated(CustomerValidatedEvent event) {
        SagaState sagaState = activeSagas.get(event.getSagaId());
        if (sagaState == null || sagaState.getCurrentStep() != SagaStep.VALIDATE_CUSTOMER) {
            return;
        }
        
        log.info("Customer validated for SAGA: {}", event.getSagaId());
        
        // Update saga state
        sagaState.setCurrentStep(SagaStep.RESERVE_CREDIT);
        sagaState.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);
        
        // Step 2: Reserve Credit
        ReserveCreditCommand command = ReserveCreditCommand.builder()
            .sagaId(event.getSagaId())
            .customerId(sagaState.getCustomerId())
            .amount(sagaState.getRequestedAmount())
            .reservationId("reservation-" + event.getSagaId())
            .commandId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("credit-commands", command);
        log.info("Sent ReserveCreditCommand for SAGA: {}", event.getSagaId());
    }

    /**
     * Step 3: Handle Credit Reserved Event
     */
    @KafkaListener(topics = "credit-events", groupId = "saga-orchestrator")
    @Transactional
    public void handleCreditReserved(CreditReservedEvent event) {
        SagaState sagaState = activeSagas.get(event.getSagaId());
        if (sagaState == null || sagaState.getCurrentStep() != SagaStep.RESERVE_CREDIT) {
            return;
        }
        
        log.info("Credit reserved for SAGA: {}", event.getSagaId());
        
        // Update saga state
        sagaState.setCurrentStep(SagaStep.CREATE_LOAN);
        sagaState.setReservationId(event.getReservationId());
        sagaState.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);
        
        // Step 3: Create Loan
        CreateLoanCommand command = CreateLoanCommand.builder()
            .sagaId(event.getSagaId())
            .customerId(sagaState.getCustomerId())
            .principal(sagaState.getRequestedAmount())
            .interestRate(sagaState.getInterestRate())
            .numberOfInstallments(sagaState.getNumberOfInstallments())
            .commandId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("loan-commands", command);
        log.info("Sent CreateLoanCommand for SAGA: {}", event.getSagaId());
    }

    /**
     * Step 4: Handle Loan Created Event
     */
    @KafkaListener(topics = "loan-events", groupId = "saga-orchestrator")
    @Transactional
    public void handleLoanCreated(LoanCreatedEvent event) {
        SagaState sagaState = activeSagas.get(event.getSagaId());
        if (sagaState == null || sagaState.getCurrentStep() != SagaStep.CREATE_LOAN) {
            return;
        }
        
        log.info("Loan created for SAGA: {}", event.getSagaId());
        
        // Update saga state
        sagaState.setCurrentStep(SagaStep.GENERATE_INSTALLMENTS);
        sagaState.setLoanId(event.getLoanId());
        sagaState.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);
        
        // Step 4: Generate Installment Schedule
        GenerateInstallmentScheduleCommand command = GenerateInstallmentScheduleCommand.builder()
            .sagaId(event.getSagaId())
            .loanId(event.getLoanId())
            .totalAmount(event.getTotalAmount())
            .numberOfInstallments(sagaState.getNumberOfInstallments())
            .commandId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("loan-commands", command);
        log.info("Sent GenerateInstallmentScheduleCommand for SAGA: {}", event.getSagaId());
    }

    /**
     * Step 5: Handle Installment Schedule Generated Event (SAGA Completion)
     */
    @KafkaListener(topics = "loan-events", groupId = "saga-orchestrator")
    @Transactional
    public void handleInstallmentScheduleGenerated(InstallmentScheduleGeneratedEvent event) {
        SagaState sagaState = activeSagas.get(event.getSagaId());
        if (sagaState == null || sagaState.getCurrentStep() != SagaStep.GENERATE_INSTALLMENTS) {
            return;
        }
        
        log.info("SAGA completed successfully: {}", event.getSagaId());
        
        // Update saga state to completed
        sagaState.setStatus(SagaStatus.COMPLETED);
        sagaState.setCurrentStep(SagaStep.COMPLETED);
        sagaState.setCompletedAt(LocalDateTime.now());
        sagaState.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);
        
        // Publish success notification
        LoanCreationSuccessEvent successEvent = LoanCreationSuccessEvent.builder()
            .sagaId(event.getSagaId())
            .applicationId(sagaState.getApplicationId())
            .loanId(sagaState.getLoanId())
            .customerId(sagaState.getCustomerId())
            .amount(sagaState.getRequestedAmount())
            .eventId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("loan-creation-events", successEvent);
        
        // Remove from active sagas
        activeSagas.remove(event.getSagaId());
    }

    /**
     * Compensation Handlers for Failed Steps
     */
    @KafkaListener(topics = "credit-events", groupId = "saga-orchestrator")
    @Transactional
    public void handleCreditReservationFailed(CreditReservationFailedEvent event) {
        SagaState sagaState = activeSagas.get(event.getSagaId());
        if (sagaState == null) {
            return;
        }
        
        log.warn("Credit reservation failed for SAGA: {} - {}", event.getSagaId(), event.getReason());
        
        // Update saga state to failed
        sagaState.setStatus(SagaStatus.FAILED);
        sagaState.setFailureReason(event.getReason());
        sagaState.setFailedAt(LocalDateTime.now());
        sagaState.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);
        
        // Start compensation - Cancel loan application
        CancelLoanApplicationCommand command = CancelLoanApplicationCommand.builder()
            .sagaId(event.getSagaId())
            .applicationId(sagaState.getApplicationId())
            .reason("Credit reservation failed: " + event.getReason())
            .commandId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("loan-commands", command);
        
        activeSagas.remove(event.getSagaId());
    }

    @KafkaListener(topics = "loan-events", groupId = "saga-orchestrator")
    @Transactional
    public void handleLoanCreationFailed(LoanCreationFailedEvent event) {
        SagaState sagaState = activeSagas.get(event.getSagaId());
        if (sagaState == null) {
            return;
        }
        
        log.warn("Loan creation failed for SAGA: {} - {}", event.getSagaId(), event.getReason());
        
        // Update saga state
        sagaState.setStatus(SagaStatus.COMPENSATING);
        sagaState.setFailureReason(event.getReason());
        sagaState.setUpdatedAt(LocalDateTime.now());
        sagaStateRepository.save(sagaState);
        
        // Start compensation - Release reserved credit
        ReleaseCreditCommand command = ReleaseCreditCommand.builder()
            .sagaId(event.getSagaId())
            .customerId(sagaState.getCustomerId())
            .amount(sagaState.getRequestedAmount())
            .reservationId(sagaState.getReservationId())
            .commandId(java.util.UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .build();
            
        kafkaTemplate.send("credit-commands", command);
        
        activeSagas.remove(event.getSagaId());
    }

    /**
     * SAGA Timeout Handler
     */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 60000) // Check every minute
    @Transactional
    public void handleSagaTimeouts() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(5);
        
        activeSagas.values().stream()
            .filter(saga -> saga.getStartedAt().isBefore(timeoutThreshold))
            .filter(saga -> saga.getStatus() == SagaStatus.STARTED)
            .forEach(saga -> {
                log.warn("SAGA timeout detected: {}", saga.getSagaId());
                
                // Update saga state to timed out
                saga.setStatus(SagaStatus.TIMED_OUT);
                saga.setFailureReason("SAGA execution timeout");
                saga.setFailedAt(LocalDateTime.now());
                saga.setUpdatedAt(LocalDateTime.now());
                sagaStateRepository.save(saga);
                
                // Start compensation based on current step
                startCompensation(saga);
                
                activeSagas.remove(saga.getSagaId());
            });
    }

    private void startCompensation(SagaState sagaState) {
        switch (sagaState.getCurrentStep()) {
            case RESERVE_CREDIT:
            case CREATE_LOAN:
            case GENERATE_INSTALLMENTS:
                // Release credit if it was reserved
                if (sagaState.getReservationId() != null) {
                    ReleaseCreditCommand command = ReleaseCreditCommand.builder()
                        .sagaId(sagaState.getSagaId())
                        .customerId(sagaState.getCustomerId())
                        .amount(sagaState.getRequestedAmount())
                        .reservationId(sagaState.getReservationId())
                        .commandId(java.util.UUID.randomUUID().toString())
                        .timestamp(LocalDateTime.now())
                        .build();
                        
                    kafkaTemplate.send("credit-commands", command);
                }
                break;
            default:
                log.info("No compensation needed for step: {}", sagaState.getCurrentStep());
        }
    }
}

// SAGA State Management
enum SagaType {
    LOAN_CREATION, PAYMENT_PROCESSING
}

enum SagaStatus {
    STARTED, COMPLETED, FAILED, COMPENSATING, COMPENSATED, TIMED_OUT
}

enum SagaStep {
    VALIDATE_CUSTOMER,
    RESERVE_CREDIT,
    CREATE_LOAN,
    GENERATE_INSTALLMENTS,
    COMPLETED
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SagaState {
    private String sagaId;
    private SagaType sagaType;
    private SagaStatus status;
    private SagaStep currentStep;
    private String applicationId;
    private Long customerId;
    private Long loanId;
    private BigDecimal requestedAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private String reservationId;
    private String failureReason;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private LocalDateTime updatedAt;
}

// SAGA Repository Interface
interface SagaStateRepository extends org.springframework.data.jpa.repository.JpaRepository<SagaStateEntity, String> {
    default void save(SagaState sagaState) {
        SagaStateEntity entity = SagaStateEntity.fromSagaState(sagaState);
        save(entity);
    }
}

// SAGA State Entity for Persistence
@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "saga_states", schema = "banking_gateway")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SagaStateEntity {
    
    @jakarta.persistence.Id
    private String sagaId;
    
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private SagaType sagaType;
    
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private SagaStatus status;
    
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private SagaStep currentStep;
    
    private String applicationId;
    private Long customerId;
    private Long loanId;
    private BigDecimal requestedAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private String reservationId;
    private String failureReason;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    private LocalDateTime updatedAt;
    
    public static SagaStateEntity fromSagaState(SagaState sagaState) {
        return SagaStateEntity.builder()
            .sagaId(sagaState.getSagaId())
            .sagaType(sagaState.getSagaType())
            .status(sagaState.getStatus())
            .currentStep(sagaState.getCurrentStep())
            .applicationId(sagaState.getApplicationId())
            .customerId(sagaState.getCustomerId())
            .loanId(sagaState.getLoanId())
            .requestedAmount(sagaState.getRequestedAmount())
            .interestRate(sagaState.getInterestRate())
            .numberOfInstallments(sagaState.getNumberOfInstallments())
            .reservationId(sagaState.getReservationId())
            .failureReason(sagaState.getFailureReason())
            .startedAt(sagaState.getStartedAt())
            .completedAt(sagaState.getCompletedAt())
            .failedAt(sagaState.getFailedAt())
            .updatedAt(sagaState.getUpdatedAt())
            .build();
    }
}

// SAGA Commands
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ValidateCustomerCommand {
    private String commandId;
    private String sagaId;
    private Long customerId;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ReserveCreditCommand {
    private String commandId;
    private String sagaId;
    private Long customerId;
    private BigDecimal amount;
    private String reservationId;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreateLoanCommand {
    private String commandId;
    private String sagaId;
    private Long customerId;
    private BigDecimal principal;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class GenerateInstallmentScheduleCommand {
    private String commandId;
    private String sagaId;
    private Long loanId;
    private BigDecimal totalAmount;
    private Integer numberOfInstallments;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CancelLoanApplicationCommand {
    private String commandId;
    private String sagaId;
    private String applicationId;
    private String reason;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ReleaseCreditCommand {
    private String commandId;
    private String sagaId;
    private Long customerId;
    private BigDecimal amount;
    private String reservationId;
    private LocalDateTime timestamp;
}

// SAGA Events
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanApplicationSubmittedEvent {
    private String eventId;
    private String applicationId;
    private Long customerId;
    private BigDecimal requestedAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CustomerValidatedEvent {
    private String eventId;
    private String sagaId;
    private Long customerId;
    private boolean valid;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreditReservedEvent {
    private String eventId;
    private String sagaId;
    private Long customerId;
    private BigDecimal amount;
    private String reservationId;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreditReservationFailedEvent {
    private String eventId;
    private String sagaId;
    private Long customerId;
    private BigDecimal amount;
    private String reservationId;
    private String reason;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanCreatedEvent {
    private String eventId;
    private String sagaId;
    private Long loanId;
    private Long customerId;
    private BigDecimal amount;
    private BigDecimal totalAmount;
    private Integer numberOfInstallments;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanCreationFailedEvent {
    private String eventId;
    private String sagaId;
    private String applicationId;
    private String reason;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class InstallmentScheduleGeneratedEvent {
    private String eventId;
    private String sagaId;
    private Long loanId;
    private Integer numberOfInstallments;
    private LocalDateTime timestamp;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LoanCreationSuccessEvent {
    private String eventId;
    private String sagaId;
    private String applicationId;
    private Long loanId;
    private Long customerId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}