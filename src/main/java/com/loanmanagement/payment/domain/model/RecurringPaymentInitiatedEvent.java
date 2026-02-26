package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain event fired when a recurring payment is initiated.
 */
@Value
@Builder
@With
public class RecurringPaymentInitiatedEvent {
    
    PaymentId paymentId;
    PaymentScheduleId scheduleId;
    Long loanId;
    String amount;
    String currency;
    PaymentFrequency frequency;
    LocalDate startDate;
    LocalDate endDate;
    Integer totalPayments;
    String initiatedBy;
    PaymentMethod paymentMethod;
    LocalDateTime occurredAt;
    
    public static RecurringPaymentInitiatedEvent create(
            PaymentId paymentId, 
            PaymentScheduleId scheduleId,
            Long loanId, 
            String amount, 
            String currency,
            PaymentFrequency frequency,
            LocalDate startDate,
            LocalDate endDate,
            Integer totalPayments,
            String initiatedBy,
            PaymentMethod paymentMethod) {
        
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (scheduleId == null) {
            throw new IllegalArgumentException("Schedule ID cannot be null");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be null or empty");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (frequency == null) {
            throw new IllegalArgumentException("Frequency cannot be null");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (initiatedBy == null || initiatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Initiated by cannot be null or empty");
        }
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }
        
        return RecurringPaymentInitiatedEvent.builder()
                .paymentId(paymentId)
                .scheduleId(scheduleId)
                .loanId(loanId)
                .amount(amount.trim())
                .currency(currency.trim())
                .frequency(frequency)
                .startDate(startDate)
                .endDate(endDate)
                .totalPayments(totalPayments)
                .initiatedBy(initiatedBy.trim())
                .paymentMethod(paymentMethod)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}