
// application/event/AuditService.java
package com.loanmanagement.application.event;

import com.loanmanagement.domain.event.*;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    public void logLoanCreation(LoanCreated event) {
        AuditLog log = new AuditLog(
            event.getEventId(),
            "LOAN_CREATED",
            event.getCustomerId().toString(),
            String.format("Loan %d created with amount %.2f", 
                event.getLoanId(), event.getLoanAmount()),
            event.getOccurredOn()
        );
        
        auditLogRepository.save(log);
    }
    
    public void logPayment(PaymentMade event) {
        AuditLog log = new AuditLog(
            event.getEventId(),
            "PAYMENT_MADE",
            event.getCustomerId().toString(),
            String.format("Payment of %.2f made for loan %d", 
                event.getTotalAmountPaid(), event.getLoanId()),
            event.getOccurredOn()
        );
        
        auditLogRepository.save(log);
    }
    
    public void logCreditReservation(CreditReserved event) {
        AuditLog log = new AuditLog(
            event.getEventId(),
            "CREDIT_RESERVED",
            event.getCustomerId().toString(),
            String.format("Credit reserved: %.2f", event.getAmount()),
            event.getOccurredOn()
        );
        
        auditLogRepository.save(log);
    }
    
    public void logCreditRelease(CreditReleased event) {
        AuditLog log = new AuditLog(
            event.getEventId(),
            "CREDIT_RELEASED",
            event.getCustomerId().toString(),
            String.format("Credit released: %.2f", event.getAmount()),
            event.getOccurredOn()
        );
        
        auditLogRepository.save(log);
    }
    
    public void logPaymentFailure(PaymentFailed event) {
        AuditLog log = new AuditLog(
            event.getEventId(),
            "PAYMENT_FAILED",
            event.getCustomerId().toString(),
            String.format("Payment failed for loan %d: %s", 
                event.getLoanId(), event.getFailureReason()),
            Instant.now()
        );
        
        auditLogRepository.save(log);
    }
}