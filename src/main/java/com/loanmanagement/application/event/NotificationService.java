// application/event/NotificationService.java
package com.loanmanagement.application.event;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class NotificationService {
    
    private final EmailService emailService;
    private final SmsService smsService;
    
    public NotificationService(EmailService emailService, SmsService smsService) {
        this.emailService = emailService;
        this.smsService = smsService;
    }
    
    public void sendLoanCreatedNotification(Long customerId, Long loanId, BigDecimal amount) {
        String message = String.format(
            "Your loan application #%d for $%.2f has been approved.", 
            loanId, amount
        );
        
        emailService.sendEmail(customerId, "Loan Approved", message);
        smsService.sendSms(customerId, message);
    }
    
    public void sendPaymentConfirmation(Long customerId, Long loanId, BigDecimal amount, Integer installments) {
        String message = String.format(
            "Payment of $%.2f received for loan #%d. %d installment(s) paid.", 
            amount, loanId, installments
        );
        
        emailService.sendEmail(customerId, "Payment Confirmation", message);
    }
    
    public void sendLoanFullyPaidNotification(Long customerId, Long loanId) {
        String message = String.format(
            "Congratulations! Your loan #%d has been fully paid.", 
            loanId
        );
        
        emailService.sendEmail(customerId, "Loan Fully Paid", message);
        smsService.sendSms(customerId, message);
    }
    
    public void sendPaymentFailedNotification(Long customerId, Long loanId, String reason) {
        String message = String.format(
            "Payment failed for loan #%d. Reason: %s", 
            loanId, reason
        );
        
        emailService.sendEmail(customerId, "Payment Failed", message);
    }
}

