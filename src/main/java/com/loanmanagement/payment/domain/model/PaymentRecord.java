package com.loanmanagement.payment.domain.model;

import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Record of a payment made against a loan
 */
@Value
@Builder
public class PaymentRecord {
    PaymentId paymentId;
    Money paymentAmount;
    PaymentAllocation allocation;
    LocalDateTime paymentDate;
    Money remainingBalance;
    String paymentMethod;
}