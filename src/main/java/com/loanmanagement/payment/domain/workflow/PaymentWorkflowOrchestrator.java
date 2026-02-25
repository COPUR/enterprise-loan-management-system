package com.loanmanagement.payment.domain.workflow;

import com.loanmanagement.payment.domain.model.*;
import com.loanmanagement.payment.domain.service.PaymentProcessingService;
import com.loanmanagement.payment.domain.service.PaymentValidationService;
import com.loanmanagement.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Payment Workflow Orchestrator
 * Manages the complete payment processing workflow with state transitions
 */
@Slf4j
@Service
public class PaymentWorkflowOrchestrator {
    
    private final PaymentProcessingService paymentProcessingService;
    private final PaymentValidationService paymentValidationService;
    private final PaymentWorkflowStateManager stateManager;
    private final PaymentNotificationService notificationService;
    
    public PaymentWorkflowOrchestrator() {
        this.paymentProcessingService = new PaymentProcessingService();
        this.paymentValidationService = new PaymentValidationService();
        this.stateManager = new PaymentWorkflowStateManager();
        this.notificationService = new PaymentNotificationService();
    }

    /**
     * Execute complete payment workflow
     */
    public PaymentWorkflowResult executePaymentWorkflow(PaymentWorkflowRequest workflowRequest) {
        log.info("Starting payment workflow for loan: {}, amount: {}", 
                workflowRequest.getPaymentRequest().getLoanId(), 
                workflowRequest.getPaymentRequest().getPaymentAmount());
        
        PaymentWorkflowExecution execution = PaymentWorkflowExecution.builder()
                .workflowId(PaymentWorkflowId.generate())
                .workflowType(workflowRequest.getWorkflowType())
                .paymentRequest(workflowRequest.getPaymentRequest())
                .currentState(PaymentWorkflowState.INITIATED)
                .startTime(LocalDateTime.now())
                .build();
        
        try {
            // Execute workflow steps
            execution = executeWorkflowSteps(execution);
            
            return PaymentWorkflowResult.builder()
                    .workflowId(execution.getWorkflowId())
                    .finalState(execution.getCurrentState())
                    .paymentResult(execution.getPaymentResult())
                    .successful(execution.getCurrentState() == PaymentWorkflowState.COMPLETED)
                    .executionTime(calculateExecutionTime(execution))
                    .stateTransitions(execution.getStateTransitions())
                    .build();
                    
        } catch (Exception e) {
            log.error("Payment workflow failed for loan: {}", 
                    workflowRequest.getPaymentRequest().getLoanId(), e);
            
            execution = stateManager.transitionToState(execution, PaymentWorkflowState.FAILED, e.getMessage());
            
            return PaymentWorkflowResult.builder()
                    .workflowId(execution.getWorkflowId())
                    .finalState(PaymentWorkflowState.FAILED)
                    .successful(false)
                    .errorMessage(e.getMessage())
                    .executionTime(calculateExecutionTime(execution))
                    .stateTransitions(execution.getStateTransitions())
                    .build();
        }
    }
    
    /**
     * Execute asynchronous payment workflow
     */
    public CompletableFuture<PaymentWorkflowResult> executePaymentWorkflowAsync(PaymentWorkflowRequest workflowRequest) {
        return CompletableFuture.supplyAsync(() -> executePaymentWorkflow(workflowRequest));
    }
    
    /**
     * Execute recurring payment workflow
     */
    public RecurringPaymentWorkflowResult executeRecurringPaymentWorkflow(RecurringPaymentWorkflowRequest request) {
        log.info("Starting recurring payment workflow for {} payments", request.getScheduledPayments().size());
        
        List<PaymentWorkflowResult> results = new ArrayList<>();
        int successfulPayments = 0;
        int failedPayments = 0;
        
        for (ScheduledPayment scheduledPayment : request.getScheduledPayments()) {
            try {
                PaymentRequest paymentRequest = createPaymentRequestFromScheduled(scheduledPayment);
                PaymentWorkflowRequest workflowRequest = PaymentWorkflowRequest.builder()
                        .paymentRequest(paymentRequest)
                        .workflowType(PaymentWorkflowType.RECURRING)
                        .priority(PaymentWorkflowPriority.NORMAL)
                        .build();
                
                PaymentWorkflowResult result = executePaymentWorkflow(workflowRequest);
                results.add(result);
                
                if (result.isSuccessful()) {
                    successfulPayments++;
                } else {
                    failedPayments++;
                }
                
            } catch (Exception e) {
                log.error("Failed to process scheduled payment: {}", scheduledPayment.getPaymentId(), e);
                failedPayments++;
            }
        }
        
        return RecurringPaymentWorkflowResult.builder()
                .batchId(request.getBatchId())
                .totalPayments(request.getScheduledPayments().size())
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .results(results)
                .executionDate(LocalDateTime.now())
                .build();
    }
    
    /**
     * Execute batch payment workflow
     */
    public BatchPaymentWorkflowResult executeBatchPaymentWorkflow(BatchPaymentWorkflowRequest request) {
        log.info("Starting batch payment workflow for {} payments", request.getPaymentRequests().size());
        
        List<PaymentWorkflowResult> results = new ArrayList<>();
        int successfulPayments = 0;
        int failedPayments = 0;
        Money totalProcessedAmount = Money.zero("USD");
        
        long startTime = System.currentTimeMillis();
        
        for (PaymentRequest paymentRequest : request.getPaymentRequests()) {
            try {
                PaymentWorkflowRequest workflowRequest = PaymentWorkflowRequest.builder()
                        .paymentRequest(paymentRequest)
                        .workflowType(PaymentWorkflowType.BATCH)
                        .priority(request.getPriority())
                        .build();
                
                PaymentWorkflowResult result = executePaymentWorkflow(workflowRequest);
                results.add(result);
                
                if (result.isSuccessful() && result.getPaymentResult() != null) {
                    successfulPayments++;
                    totalProcessedAmount = totalProcessedAmount.add(result.getPaymentResult().getProcessedAmount());
                } else {
                    failedPayments++;
                }
                
            } catch (Exception e) {
                log.error("Failed to process batch payment for loan: {}", paymentRequest.getLoanId(), e);
                failedPayments++;
            }
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        return BatchPaymentWorkflowResult.builder()
                .batchId(request.getBatchId())
                .totalPayments(request.getPaymentRequests().size())
                .successfulPayments(successfulPayments)
                .failedPayments(failedPayments)
                .totalProcessedAmount(totalProcessedAmount)
                .results(results)
                .executionTimeMs(executionTime)
                .executionDate(LocalDateTime.now())
                .build();
    }
    
    /**
     * Execute payment reversal workflow
     */
    public PaymentReversalWorkflowResult executePaymentReversalWorkflow(PaymentReversalWorkflowRequest request) {
        log.info("Starting payment reversal workflow for payment: {}", request.getOriginalPaymentId());
        
        PaymentWorkflowExecution execution = PaymentWorkflowExecution.builder()
                .workflowId(PaymentWorkflowId.generate())
                .workflowType(PaymentWorkflowType.REVERSAL)
                .currentState(PaymentWorkflowState.INITIATED)
                .startTime(LocalDateTime.now())
                .build();
        
        try {
            // Validate reversal request
            execution = stateManager.transitionToState(execution, PaymentWorkflowState.VALIDATING, 
                    "Validating reversal request");
            
            PaymentReversalValidationResult validationResult = validateReversalRequest(request);
            if (!validationResult.isValid()) {
                execution = stateManager.transitionToState(execution, PaymentWorkflowState.VALIDATION_FAILED, 
                        validationResult.getErrorMessage());
                throw new PaymentWorkflowException("Reversal validation failed: " + validationResult.getErrorMessage());
            }
            
            // Process reversal
            execution = stateManager.transitionToState(execution, PaymentWorkflowState.PROCESSING, 
                    "Processing payment reversal");
            
            PaymentReversalRequest reversalRequest = PaymentReversalRequest.builder()
                    .originalPaymentId(request.getOriginalPaymentId())
                    .reversalReason(request.getReversalReason())
                    .reversalAmount(request.getReversalAmount())
                    .reversalDate(LocalDateTime.now())
                    .requestedBy(request.getRequestedBy())
                    .build();
            
            PaymentReversalResult reversalResult = paymentProcessingService.processPaymentReversal(reversalRequest);
            
            if (reversalResult.isSuccessful()) {
                execution = stateManager.transitionToState(execution, PaymentWorkflowState.COMPLETED, 
                        "Reversal completed successfully");
                
                // Send notifications
                notificationService.sendReversalNotification(reversalResult);
                
                return PaymentReversalWorkflowResult.builder()
                        .workflowId(execution.getWorkflowId())
                        .reversalResult(reversalResult)
                        .successful(true)
                        .executionTime(calculateExecutionTime(execution))
                        .build();
            } else {
                execution = stateManager.transitionToState(execution, PaymentWorkflowState.FAILED, 
                        "Reversal processing failed");
                throw new PaymentWorkflowException("Reversal processing failed");
            }
            
        } catch (Exception e) {
            log.error("Payment reversal workflow failed for payment: {}", request.getOriginalPaymentId(), e);
            
            execution = stateManager.transitionToState(execution, PaymentWorkflowState.FAILED, e.getMessage());
            
            return PaymentReversalWorkflowResult.builder()
                    .workflowId(execution.getWorkflowId())
                    .successful(false)
                    .errorMessage(e.getMessage())
                    .executionTime(calculateExecutionTime(execution))
                    .build();
        }
    }
    
    /**
     * Get workflow status
     */
    public PaymentWorkflowStatus getWorkflowStatus(PaymentWorkflowId workflowId) {
        // In a real implementation, this would retrieve from a workflow state store
        return PaymentWorkflowStatus.builder()
                .workflowId(workflowId)
                .currentState(PaymentWorkflowState.PROCESSING)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
    
    // Private helper methods
    
    private PaymentWorkflowExecution executeWorkflowSteps(PaymentWorkflowExecution execution) {
        // Step 1: Validation
        execution = executeValidationStep(execution);
        if (execution.getCurrentState() == PaymentWorkflowState.VALIDATION_FAILED) {
            return execution;
        }
        
        // Step 2: Pre-processing
        execution = executePreProcessingStep(execution);
        if (execution.getCurrentState() == PaymentWorkflowState.FAILED) {
            return execution;
        }
        
        // Step 3: Processing
        execution = executeProcessingStep(execution);
        if (execution.getCurrentState() == PaymentWorkflowState.FAILED) {
            return execution;
        }
        
        // Step 4: Post-processing
        execution = executePostProcessingStep(execution);
        
        return execution;
    }
    
    private PaymentWorkflowExecution executeValidationStep(PaymentWorkflowExecution execution) {
        log.debug("Executing validation step for workflow: {}", execution.getWorkflowId());
        
        execution = stateManager.transitionToState(execution, PaymentWorkflowState.VALIDATING, 
                "Starting payment validation");
        
        try {
            PaymentValidationResult validationResult = paymentValidationService
                    .validatePayment(execution.getPaymentRequest());
            
            if (validationResult.isValid()) {
                execution = stateManager.transitionToState(execution, PaymentWorkflowState.VALIDATED, 
                        "Payment validation successful");
            } else {
                String errorMessage = validationResult.getViolations().stream()
                        .findFirst()
                        .map(PaymentViolation::getMessage)
                        .orElse("Validation failed");
                
                execution = stateManager.transitionToState(execution, PaymentWorkflowState.VALIDATION_FAILED, 
                        errorMessage);
            }
            
            return execution;
            
        } catch (Exception e) {
            log.error("Validation step failed for workflow: {}", execution.getWorkflowId(), e);
            return stateManager.transitionToState(execution, PaymentWorkflowState.VALIDATION_FAILED, 
                    "Validation error: " + e.getMessage());
        }
    }
    
    private PaymentWorkflowExecution executePreProcessingStep(PaymentWorkflowExecution execution) {
        log.debug("Executing pre-processing step for workflow: {}", execution.getWorkflowId());
        
        execution = stateManager.transitionToState(execution, PaymentWorkflowState.PRE_PROCESSING, 
                "Starting pre-processing");
        
        try {
            // Perform pre-processing tasks
            // - Fraud detection
            // - Risk assessment
            // - Customer verification
            // - Account validation
            
            // For now, just simulate pre-processing
            Thread.sleep(100); // Simulate processing time
            
            execution = stateManager.transitionToState(execution, PaymentWorkflowState.PRE_PROCESSING_COMPLETED, 
                    "Pre-processing completed");
            
            return execution;
            
        } catch (Exception e) {
            log.error("Pre-processing step failed for workflow: {}", execution.getWorkflowId(), e);
            return stateManager.transitionToState(execution, PaymentWorkflowState.FAILED, 
                    "Pre-processing error: " + e.getMessage());
        }
    }
    
    private PaymentWorkflowExecution executeProcessingStep(PaymentWorkflowExecution execution) {
        log.debug("Executing processing step for workflow: {}", execution.getWorkflowId());
        
        execution = stateManager.transitionToState(execution, PaymentWorkflowState.PROCESSING, 
                "Starting payment processing");
        
        try {
            PaymentResult paymentResult = paymentProcessingService.processPayment(execution.getPaymentRequest());
            
            execution = execution.toBuilder()
                    .paymentResult(paymentResult)
                    .build();
            
            if (paymentResult.isSuccessful()) {
                execution = stateManager.transitionToState(execution, PaymentWorkflowState.PROCESSED, 
                        "Payment processed successfully");
            } else {
                execution = stateManager.transitionToState(execution, PaymentWorkflowState.FAILED, 
                        "Payment processing failed: " + paymentResult.getFailureMessage());
            }
            
            return execution;
            
        } catch (Exception e) {
            log.error("Processing step failed for workflow: {}", execution.getWorkflowId(), e);
            return stateManager.transitionToState(execution, PaymentWorkflowState.FAILED, 
                    "Processing error: " + e.getMessage());
        }
    }
    
    private PaymentWorkflowExecution executePostProcessingStep(PaymentWorkflowExecution execution) {
        log.debug("Executing post-processing step for workflow: {}", execution.getWorkflowId());
        
        execution = stateManager.transitionToState(execution, PaymentWorkflowState.POST_PROCESSING, 
                "Starting post-processing");
        
        try {
            // Perform post-processing tasks
            // - Send notifications
            // - Update loan balance
            // - Generate receipts
            // - Trigger downstream processes
            
            if (execution.getPaymentResult() != null && execution.getPaymentResult().isSuccessful()) {
                notificationService.sendPaymentNotification(execution.getPaymentResult());
            }
            
            execution = stateManager.transitionToState(execution, PaymentWorkflowState.COMPLETED, 
                    "Workflow completed successfully");
            
            return execution;
            
        } catch (Exception e) {
            log.error("Post-processing step failed for workflow: {}", execution.getWorkflowId(), e);
            return stateManager.transitionToState(execution, PaymentWorkflowState.POST_PROCESSING_FAILED, 
                    "Post-processing error: " + e.getMessage());
        }
    }
    
    private PaymentRequest createPaymentRequestFromScheduled(ScheduledPayment scheduledPayment) {
        return PaymentRequest.builder()
                .loanId(scheduledPayment.getLoanId())
                .customerId(scheduledPayment.getCustomerId())
                .paymentAmount(scheduledPayment.getPaymentAmount())
                .paymentDate(LocalDateTime.now())
                .paymentType(PaymentType.RECURRING_PAYMENT)
                .build();
    }
    
    private PaymentReversalValidationResult validateReversalRequest(PaymentReversalWorkflowRequest request) {
        // Validate reversal business rules
        if (request.getOriginalPaymentId() == null) {
            return PaymentReversalValidationResult.invalid("Original payment ID is required");
        }
        
        if (request.getReversalReason() == null) {
            return PaymentReversalValidationResult.invalid("Reversal reason is required");
        }
        
        if (request.getReversalAmount() == null || request.getReversalAmount().getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return PaymentReversalValidationResult.invalid("Valid reversal amount is required");
        }
        
        // Additional business rule validations would go here
        
        return PaymentReversalValidationResult.valid();
    }
    
    private long calculateExecutionTime(PaymentWorkflowExecution execution) {
        if (execution.getStartTime() == null) {
            return 0;
        }
        return java.time.Duration.between(execution.getStartTime(), LocalDateTime.now()).toMillis();
    }
}