package com.loanmanagement.loan.domain.event;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Domain Event: Loan Payment Made
 * Published when a payment is made on a loan
 */
@Value
@Builder
public class LoanPaymentMadeEvent {
    
    String eventId;
    LocalDateTime occurredOn;
    LoanId loanId;
    CustomerId customerId;
    Money paymentAmount;
    Money principalAmount;
    Money interestAmount;
    Money feesAmount;
    LocalDateTime paymentDate;
    Money remainingBalance;
}