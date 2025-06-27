package com.banking.loan.infrastructure.adapters.out;

import com.banking.loan.application.ports.in.LoanApplicationUseCase;
import com.banking.loan.application.ports.in.PaymentProcessingUseCase;
import com.banking.loan.application.ports.in.CustomerManagementUseCase;
import com.banking.loan.application.ports.in.AIServicesUseCase;
import com.banking.loan.application.ports.in.ComplianceUseCase;
import com.banking.loan.application.commands.*;
import com.banking.loan.domain.shared.*;
import com.banking.loan.domain.events.*;
import com.banking.loan.resilience.BankingCircuitBreakerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Event Listener - Infrastructure Adapter for Event-Driven Communication
 * Handles cross-service events and coordinates business processes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventListener {
    
    private final LoanApplicationUseCase loanApplicationUseCase;
    private final PaymentProcessingUseCase paymentProcessingUseCase;
    private final CustomerManagementUseCase customerManagementUseCase;
    private final AIServicesUseCase aiServicesUseCase;
    private final ComplianceUseCase complianceUseCase;
    private final BankingCircuitBreakerService circuitBreakerService;
    private final ObjectMapper objectMapper;
    
    // ============= CUSTOMER DOMAIN EVENT HANDLERS =============
    
    @KafkaListener(topics = {
        "banking.customer.onboarding.kyc-completed.v1",
        "banking.customer.lifecycle.profile-updated.v1",
        "banking.customer.lifecycle.account-created.v1"
    }, groupId = "loan-service-group", containerFactory = "bankingKafkaListenerContainerFactory")
    public void handleCustomerLifecycleEvents(
            @Payload String eventData,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset,
            @Header(value = KafkaHeaders.CORRELATION_ID, required = false) String correlationId,
            Acknowledgment ack) {
        
        log.info("Received CustomerCreated event on topic: {} with correlation: {}", topic, correlationId);
        
        try {
            CustomerCreatedEvent event = objectMapper.readValue(eventData, CustomerCreatedEvent.class);
            
            // Trigger welcome loan recommendations for new customer
            circuitBreakerService.executeAIOperation(
                () -> {
                    aiServicesUseCase.generateRecommendations(
                        new GenerateRecommendationsCommand(
                            event.getAggregateId(),
                            "WELCOME_LOAN_PRODUCTS",
                            java.util.Map.of("event_type", "new_customer_onboarding"),
                            java.util.Map.of("correlationId", correlationId)
                        )
                    );
                    return null;
                },
                "generate-welcome-recommendations"
            );
            
            ack.acknowledge();
            log.debug("Successfully processed CustomerCreated event for customer: {}", event.getAggregateId());
            
        } catch (Exception e) {
            log.error("Failed to process CustomerCreated event with correlation: {}", correlationId, e);
            // Don't acknowledge - message will be retried
        }
    }
    
    @KafkaListener(topics = "banking.customer.blocked", groupId = "loan-service-group")
    public void handleCustomerBlocked(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.info("Received CustomerBlocked event with correlation: {}", correlationId);
        
        try {
            CustomerBlockedEvent event = objectMapper.readValue(eventData, CustomerBlockedEvent.class);
            
            // Suspend all active loan applications for blocked customer
            circuitBreakerService.executeLoanOperation(
                () -> {
                    // Implementation would query and suspend active applications
                    log.info("Suspending loan applications for blocked customer: {}", event.getAggregateId());
                    return null;
                },
                "suspend-customer-loans"
            );
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process CustomerBlocked event with correlation: {}", correlationId, e);
        }
    }
    
    // ============= PAYMENT DOMAIN EVENT HANDLERS =============
    
    @KafkaListener(topics = "banking.payment.processed", groupId = "loan-service-group")
    public void handlePaymentProcessed(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.info("Received PaymentProcessed event with correlation: {}", correlationId);
        
        try {
            PaymentProcessedEvent event = objectMapper.readValue(eventData, PaymentProcessedEvent.class);
            
            // Update loan payment schedule and status
            circuitBreakerService.executePaymentOperation(
                () -> {
                    // This would update the loan aggregate with payment information
                    log.info("Updating loan {} with processed payment of {}", 
                        event.loanId(), event.paymentAmount());
                    return null;
                },
                "update-loan-with-payment"
            );
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process PaymentProcessed event with correlation: {}", correlationId, e);
        }
    }
    
    @KafkaListener(topics = "banking.payment.failed", groupId = "loan-service-group")
    public void handlePaymentFailed(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.info("Received PaymentFailed event with correlation: {}", correlationId);
        
        try {
            PaymentFailedEvent event = objectMapper.readValue(eventData, PaymentFailedEvent.class);
            
            // Handle payment failure - may trigger notifications, retry logic, etc.
            circuitBreakerService.executePaymentOperation(
                () -> {
                    log.warn("Payment failed for loan {} - amount: {}, reason: {}", 
                        event.loanId(), event.attemptedAmount(), event.failureReason());
                    
                    // Could trigger automatic retry, notification to customer, etc.
                    return null;
                },
                "handle-payment-failure"
            );
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process PaymentFailed event with correlation: {}", correlationId, e);
        }
    }
    
    // ============= AI DOMAIN EVENT HANDLERS =============
    
    @KafkaListener(topics = "banking.ai.recommendation-generated", groupId = "loan-service-group")
    public void handleAIRecommendationGenerated(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.info("Received AIRecommendationGenerated event with correlation: {}", correlationId);
        
        try {
            AIRecommendationGeneratedEvent event = objectMapper.readValue(eventData, AIRecommendationGeneratedEvent.class);
            
            // Store recommendations for customer and potentially trigger notifications
            circuitBreakerService.executeAIOperation(
                () -> {
                    log.info("Storing AI recommendations for customer: {} - type: {}", 
                        event.getAggregateId(), event.getRecommendationType());
                    
                    // Implementation would store recommendations and possibly notify customer
                    return null;
                },
                "store-ai-recommendations"
            );
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process AIRecommendationGenerated event with correlation: {}", correlationId, e);
        }
    }
    
    // ============= FRAUD DOMAIN EVENT HANDLERS =============
    
    @KafkaListener(topics = "banking.fraud.detected", groupId = "loan-service-group")
    public void handleFraudDetected(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.warn("Received FraudDetected event with correlation: {}", correlationId);
        
        try {
            FraudDetectedEvent event = objectMapper.readValue(eventData, FraudDetectedEvent.class);
            
            // Immediately flag associated loans for investigation
            circuitBreakerService.executeLoanOperation(
                () -> {
                    log.warn("Fraud detected for customer: {} - risk level: {}", 
                        event.getCustomerId(), event.getRiskLevel());
                    
                    // Implementation would flag all customer loans for review
                    // and possibly freeze ongoing applications
                    return null;
                },
                "handle-fraud-detection"
            );
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process FraudDetected event with correlation: {}", correlationId, e);
        }
    }
    
    // ============= COMPLIANCE DOMAIN EVENT HANDLERS =============
    
    @KafkaListener(topics = "banking.compliance.check-completed", groupId = "loan-service-group")
    public void handleComplianceCheckCompleted(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.info("Received ComplianceCheckCompleted event with correlation: {}", correlationId);
        
        try {
            ComplianceCheckCompletedEvent event = objectMapper.readValue(eventData, ComplianceCheckCompletedEvent.class);
            
            if (event.getLoanId() != null) {
                // Update loan with compliance check results
                circuitBreakerService.executeLoanOperation(
                    () -> {
                        log.info("Updating loan {} with compliance check result: {}", 
                            event.getLoanId(), event.getComplianceStatus());
                        
                        // Implementation would update loan aggregate with compliance status
                        return null;
                    },
                    "update-loan-compliance"
                );
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process ComplianceCheckCompleted event with correlation: {}", correlationId, e);
        }
    }
    
    // ============= SAGA COORDINATION EVENT HANDLERS =============
    
    @KafkaListener(topics = "banking.saga.started", groupId = "loan-service-group")
    public void handleSagaStarted(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.info("Received SagaStarted event with correlation: {}", correlationId);
        
        try {
            SagaStartedEvent event = objectMapper.readValue(eventData, SagaStartedEvent.class);
            
            if ("LoanOrigination".equals(event.getSagaType())) {
                // Loan service participates in loan origination saga
                log.info("Participating in loan origination saga: {}", event.getAggregateId());
                
                // Implementation would handle loan service's part of the saga
            }
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process SagaStarted event with correlation: {}", correlationId, e);
        }
    }
    
    @KafkaListener(topics = "banking.saga.failed", groupId = "loan-service-group")
    public void handleSagaFailed(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.warn("Received SagaFailed event with correlation: {}", correlationId);
        
        try {
            SagaFailedEvent event = objectMapper.readValue(eventData, SagaFailedEvent.class);
            
            // Handle saga failure - may require compensation actions
            circuitBreakerService.executeLoanOperation(
                () -> {
                    log.warn("Saga failed: {} - step: {}, compensation required: {}", 
                        event.getSagaType(), event.getFailedStep(), event.getFailureReason());
                    
                    // Implementation would handle compensation logic
                    return null;
                },
                "handle-saga-failure"
            );
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process SagaFailed event with correlation: {}", correlationId, e);
        }
    }
    
    // ============= EXTERNAL SYSTEM EVENT HANDLERS =============
    
    @KafkaListener(topics = "banking.external.credit-bureau-response", groupId = "loan-service-group")
    public void handleCreditBureauResponse(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.info("Received CreditBureauResponse event with correlation: {}", correlationId);
        
        try {
            // Parse credit bureau response and update loan application
            circuitBreakerService.executeExternalServiceOperation(
                () -> {
                    log.info("Processing credit bureau response for correlation: {}", correlationId);
                    
                    // Implementation would update loan with credit check results
                    // and potentially move the application to next stage
                    return null;
                },
                "credit-bureau",
                "process-credit-response"
            );
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process CreditBureauResponse event with correlation: {}", correlationId, e);
        }
    }
    
    @KafkaListener(topics = "banking.external.regulatory-update", groupId = "loan-service-group")
    public void handleRegulatoryUpdate(
            @Payload String eventData,
            @Header(KafkaHeaders.CORRELATION_ID) String correlationId,
            Acknowledgment ack) {
        
        log.info("Received RegulatoryUpdate event with correlation: {}", correlationId);
        
        try {
            // Handle regulatory updates that might affect loan processing
            circuitBreakerService.executeLoanOperation(
                () -> {
                    log.info("Processing regulatory update with correlation: {}", correlationId);
                    
                    // Implementation would update business rules, compliance checks, etc.
                    return null;
                },
                "handle-regulatory-update"
            );
            
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process RegulatoryUpdate event with correlation: {}", correlationId, e);
        }
    }
}