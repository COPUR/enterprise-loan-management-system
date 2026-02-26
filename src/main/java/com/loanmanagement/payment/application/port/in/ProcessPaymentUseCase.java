package com.loanmanagement.payment.application.port.in;

import com.loanmanagement.payment.domain.model.Payment;
import com.loanmanagement.payment.domain.model.PaymentType;
import com.loanmanagement.shared.domain.model.Money;
import java.time.LocalDate;

public interface ProcessPaymentUseCase {
    
    Payment processPayment(ProcessPaymentCommand command);
    
    record ProcessPaymentCommand(
            Long loanId,
            Money amount,
            LocalDate paymentDate,
            PaymentType type,
            String paymentMethod,
            String reference,
            String processedBy
    ) {}
}