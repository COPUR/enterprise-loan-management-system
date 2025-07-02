package com.bank.loanmanagement.loan.application.ports.in;

import com.bank.loanmanagement.loan.application.commands.*;
import com.bank.loanmanagement.loan.application.queries.*;
import com.bank.loanmanagement.loan.application.results.*;
import java.util.List;

/**
 * Payment Processing Use Case (Hexagonal Architecture - Inbound Port)
 * Defines payment-related business operations
 */
public interface PaymentProcessingUseCase {
    
    /**
     * Process a loan payment
     */
    PaymentResult processPayment(ProcessPaymentCommand command);
    
    /**
     * Schedule a future payment
     */
    PaymentScheduleResult schedulePayment(SchedulePaymentCommand command);
    
    /**
     * Get payment history for a loan
     */
    List<PaymentHistory> getPaymentHistory(GetPaymentHistoryQuery query);
    
    /**
     * Calculate early payment options
     */
    EarlyPaymentOptions calculateEarlyPayment(CalculateEarlyPaymentQuery query);
}