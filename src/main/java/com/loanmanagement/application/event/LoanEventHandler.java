package com.loanmanagement.application.event;

import com.loanmanagement.domain.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class LoanEventHandler {
    
    private static final Logger log = LoggerFactory.getLogger(LoanEventHandler.class);
    
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final MetricsService metricsService;
    
    public LoanEventHandler(
        NotificationService notificationService,
        AuditService auditService,
        MetricsService metricsService
    ) {
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.metricsService = metricsService;
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleLoanCreated(LoanCreated event) {
        log.info("Loan created: {} for customer: {}", event.getLoanId(), event.getCustomerId());
        
        // Send notification
        notificationService.sendLoanCreatedNotification(
            event.getCustomerId(), 
            event.getLoanId(), 
            event.getLoanAmount()
        );
        
        // Update metrics
        metricsService.recordLoanCreation(
            event.getLoanAmount(), 
            event.getNumberOfInstallments()
        );
        
        // Audit log
        auditService.logLoanCreation(event);
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handlePaymentMade(PaymentMade event) {
        log.info("Payment made for loan: {}, installments paid: {}", 
            event.getLoanId(), event.getInstallmentsPaid());
        
        // Send notification
        notificationService.sendPaymentConfirmation(
            event.getCustomerId(),
            event.getLoanId(),
            event.getTotalAmountPaid(),
            event.getInstallmentsPaid()
        );
        
        // Update metrics
        metricsService.recordPayment(
            event.getTotalAmountPaid(),
            event.getDiscountAmount(),
            event.getPenaltyAmount(),
            event.isLoanFullyPaid()
        );
        
        // Special notification for fully paid loans
        if (event.isLoanFullyPaid()) {
            notificationService.sendLoanFullyPaidNotification(
                event.getCustomerId(),
                event.getLoanId()
            );
        }
        
        // Audit log
        auditService.logPayment(event);
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreditReserved(CreditReserved event) {
        log.info("Credit reserved for customer: {}, amount: {}", 
            event.getCustomerId(), event.getAmount());
        
        metricsService.updateCreditUtilization(
            event.getCustomerId(),
            event.getAvailableCreditAfter()
        );
        
        auditService.logCreditReservation(event);
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreditReleased(CreditReleased event) {
        log.info("Credit released for customer: {}, amount: {}", 
            event.getCustomerId(), event.getAmount());
        
        metricsService.updateCreditUtilization(
            event.getCustomerId(),
            event.getAvailableCreditAfter()
        );
        
        auditService.logCreditRelease(event);
    }
    
    @EventListener
    public void handlePaymentFailed(PaymentFailed event) {
        log.error("Payment failed for loan: {}, reason: {}", 
            event.getLoanId(), event.getFailureReason());
        
        // Send failure notification
        notificationService.sendPaymentFailedNotification(
            event.getCustomerId(),
            event.getLoanId(),
            event.getFailureReason()
        );
        
        // Alert operations team for repeated failures
        metricsService.recordPaymentFailure(event.getLoanId());
        
        auditService.logPaymentFailure(event);
    }
}