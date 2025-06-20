package com.bank.loanmanagement.application.port.in;

import com.bank.loanmanagement.domain.payment.Payment;
import com.bank.loanmanagement.domain.payment.PaymentId;

/**
 * Port for payment command operations
 * Following hexagonal architecture principles
 */
public interface PaymentCommandPort {
    
    Payment processPayment(Payment payment);
    
    Payment updatePayment(Payment payment);
    
    Payment schedulePayment(Payment payment);
    
    void cancelPayment(PaymentId paymentId);
    
    Payment refundPayment(PaymentId paymentId);
}