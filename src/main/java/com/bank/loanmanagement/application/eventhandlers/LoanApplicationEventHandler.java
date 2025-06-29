package com.bank.loanmanagement.application.eventhandlers;

import com.bank.loanmanagement.domain.application.events.LoanApplicationApprovedEvent;
import com.bank.loanmanagement.domain.application.events.LoanApplicationSubmittedEvent;
import com.bank.loanmanagement.domain.application.events.UnderwriterAssignedEvent;
import com.bank.loanmanagement.domain.application.ApplicationPriority;
import com.bank.loanmanagement.domain.application.LoanType;
import com.bank.loanmanagement.sharedkernel.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event handler for loan application domain events.
 * <p>
 * Implements Event-Driven Communication by reacting to domain events and
 * triggering appropriate business processes.
 * <p>
 * Architecture Compliance:
 * <ul>
 *   <li>Clean Code – single responsibility for application event handling</li>
 *   <li>Hexagonal – application service responding to domain events</li>
 *   <li>Event-Driven – decoupled event handling for business workflows</li>
 *   <li>DDD – application service orchestrating domain operations</li>
 * </ul>
 */
@Component
public class LoanApplicationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationEventHandler.class);

    /* -------------------------------------------------- Dependencies */

    private final NotificationService       notificationService;
    private final RiskAssessmentService     riskAssessmentService;
    private final AuditService              auditService;
    private final LoanCreationService       loanCreationService;
    private final UnderwriterWorkloadService underwriterWorkloadService;

    /* -------------------------------------------------- Constructor */

    public LoanApplicationEventHandler(
            NotificationService notificationService,
            RiskAssessmentService riskAssessmentService,
            AuditService auditService,
            LoanCreationService loanCreationService,
            UnderwriterWorkloadService underwriterWorkloadService) {
        this.notificationService = notificationService;
        this.riskAssessmentService = riskAssessmentService;
        this.auditService = auditService;
        this.loanCreationService = loanCreationService;
        this.underwriterWorkloadService = underwriterWorkloadService;
    }

    /* -------------------------------------------------- Event handlers */

    /**
     * Trigger risk assessment, customer confirmation and audit logging when a loan application is submitted.
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(LoanApplicationSubmittedEvent event) {
        log.info("Processing LoanApplicationSubmittedEvent – applicationId={}", event.getApplicationId());

        try {
            /* 1. Risk assessment */
            if (event.requiresImmediateProcessing()) {
                riskAssessmentService.initiateUrgentRiskAssessment(
                        event.getApplicationId(),
                        event.getCustomerId(),
                        event.getLoanType(),
                        event.getRequestedAmount());
            } else {
                riskAssessmentService.initiateStandardRiskAssessment(
                        event.getApplicationId(),
                        event.getCustomerId(),
                        event.getLoanType(),
                        event.getRequestedAmount());
            }

            /* 2. Customer confirmation */
            notificationService.sendApplicationConfirmation(
                    event.getCustomerId(),
                    event.getApplicationId(),
                    event.getLoanType(),
                    event.getRequestedAmount());

            /* 3. Audit log */
            auditService.logApplicationSubmission(
                    event.getApplicationId(),
                    event.getCustomerId(),
                    event.getSubmittedBy(),
                    event.getLoanType(),
                    event.getRequestedAmount(),
                    event.getOccurredOn());

            /* 4. Metrics (best-effort) */
            updateApplicationMetrics(event);
        } catch (Exception ex) {
            log.error("Unable to process LoanApplicationSubmittedEvent – applicationId={}", event.getApplicationId(), ex);
            throw new EventProcessingException("Failed to process LoanApplicationSubmittedEvent", ex);
        }
    }

    /**
     * Persist the loan record when the application is approved and notify stakeholders.
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(LoanApplicationApprovedEvent event) {
        log.info("Processing LoanApplicationApprovedEvent – applicationId={}", event.getApplicationId());

        try {
            /* 1. Create loan */
            String loanId = loanCreationService.createLoanFromApprovedApplication(
                    event.getApplicationId(),
                    event.getCustomerId(),
                    event.getApprovedAmount(),
                    event.getApprovedRate());

            /* 2. Notify customer */
            notificationService.sendApprovalNotification(
                    event.getCustomerId(),
                    event.getApplicationId(),
                    loanId,
                    event.getApprovedAmount(),
                    event.getApprovedRate());

            /* 3. Notify loan officer (optional) */
            notificationService.sendLoanOfficerApprovalNotification(
                    event.getApplicationId(),
                    loanId,
                    event.getApprovedAmount());

            /* 4. Update underwriter metrics */
            underwriterWorkloadService.recordApproval(
                    event.getUnderwriterId(),
                    event.getApprovedAmount());

            /* 5. Executive notification if threshold exceeded */
            if (event.requiresExecutiveNotification()) {
                notificationService.sendExecutiveNotification(
                        event.getApplicationId(),
                        event.getApprovedAmount(),
                        event.getUnderwriterId(),
                        "High-value loan approved – executive notification required");
            }

            /* 6. Audit log */
            auditService.logLoanApproval(
                    event.getApplicationId(),
                    loanId,
                    event.getUnderwriterId(),
                    event.getApproverId(),
                    event.getApprovedAmount(),
                    event.getApprovalReason(),
                    event.getOccurredOn());

        } catch (Exception ex) {
            log.error("Unable to process LoanApplicationApprovedEvent – applicationId={}", event.getApplicationId(), ex);
            throw new EventProcessingException("Failed to process LoanApplicationApprovedEvent", ex);
        }
    }

    /**
     * Update underwriter workload and inform them when they are assigned to a new application.
     */
    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UnderwriterAssignedEvent event) {
        log.info("Processing UnderwriterAssignedEvent – applicationId={}, underwriterId={}",
                 event.getApplicationId(), event.getUnderwriterId());

        try {
            /* 1. Workload metrics */
            underwriterWorkloadService.incrementWorkload(event.getUnderwriterId(), event.getPriority());

            /* 2. Notify underwriter */
            if (event.requiresImmediateNotification()) {
                notificationService.sendUrgentAssignmentNotification(
                        event.getUnderwriterId(),
                        event.getApplicationId(),
                        event.getPriority(),
                        "Urgent loan application – immediate attention required");
            } else {
                notificationService.sendAssignmentNotification(
                        event.getUnderwriterId(),
                        event.getApplicationId(),
                        event.getPriority());
            }

            /* 3. Audit log */
            auditService.logUnderwriterAssignment(
                    event.getApplicationId(),
                    event.getUnderwriterId(),
                    event.getAssignedBy(),
                    event.getAssignmentReason(),
                    event.getAssignedAt());

            /* 4. Balance workload */
            if (underwriterWorkloadService.isOverloaded(event.getUnderwriterId())) {
                log.warn("Underwriter {} may be overloaded after assignment of application {}",
                         event.getUnderwriterId(), event.getApplicationId());
                notificationService.sendWorkloadAlert(
                        event.getUnderwriterId(),
                        "Workload capacity approaching maximum");
            }

        } catch (Exception ex) {
            log.error("Unable to process UnderwriterAssignedEvent – applicationId={}", event.getApplicationId(), ex);
            throw new EventProcessingException("Failed to process UnderwriterAssignedEvent", ex);
        }
    }

    /* -------------------------------------------------- Helper methods */

    private void updateApplicationMetrics(LoanApplicationSubmittedEvent event) {
        try {
            // Integrate with metrics/analytics service
            log.debug("Metrics updated – loanType={}, amount={}", event.getLoanType(), event.getRequestedAmount());
        } catch (Exception ex) {
            log.warn("Metrics update failed – applicationId={}", event.getApplicationId(), ex);
            // Best-effort: do not fail business transaction if metrics are unavailable
        }
    }

    /* -------------------------------------------------- Internal types */

    /** Raised when an unrecoverable error occurs while processing a domain event. */
    static class EventProcessingException extends RuntimeException {
        EventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /* -------------------------------------------------- SPI Definitions (ports) */

    interface NotificationService {
        void sendApplicationConfirmation(String customerId, String applicationId, LoanType loanType, Money amount);
        void sendApprovalNotification(String customerId, String applicationId, String loanId, Money amount, BigDecimal rate);
        void sendLoanOfficerApprovalNotification(String applicationId, String loanId, Money amount);
        void sendExecutiveNotification(String applicationId, Money amount, String underwriterId, String reason);
        void sendAssignmentNotification(String underwriterId, String applicationId, ApplicationPriority priority);
        void sendUrgentAssignmentNotification(String underwriterId, String applicationId, ApplicationPriority priority, String message);
        void sendWorkloadAlert(String underwriterId, String message);
    }

    interface RiskAssessmentService {
        void initiateStandardRiskAssessment(String applicationId, String customerId, LoanType loanType, Money amount);
        void initiateUrgentRiskAssessment(String applicationId, String customerId, LoanType loanType, Money amount);
    }

    interface AuditService {
        void logApplicationSubmission(String applicationId, String customerId, String submittedBy,
                                      LoanType loanType, Money amount, LocalDateTime timestamp);
        void logLoanApproval(String applicationId, String loanId, String underwriterId, String approverId,
                              Money amount, String reason, LocalDateTime timestamp);
        void logUnderwriterAssignment(String applicationId, String underwriterId, String assignedBy,
                                      String reason, LocalDateTime timestamp);
    }

    interface LoanCreationService {
        String createLoanFromApprovedApplication(String applicationId, String customerId, Money amount, BigDecimal rate);
    }

    interface UnderwriterWorkloadService {
        void incrementWorkload(String underwriterId, ApplicationPriority priority);
        void recordApproval(String underwriterId, Money approvedAmount);
        boolean isOverloaded(String underwriterId);
    }
}
