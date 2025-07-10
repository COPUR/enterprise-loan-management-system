package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Domain event fired when a payment is settled.
 */
@Value
@Builder
@With
public class PaymentSettledEvent {
    
    PaymentId paymentId;
    Long loanId;
    String amount;
    String currency;
    String settlementAmount;
    String settlementCurrency;
    String settlementReference;
    String settlementMethod;
    CurrencyConversionDetails conversionDetails;
    LocalDateTime occurredAt;
    
    public static PaymentSettledEvent create(
            PaymentId paymentId, 
            Long loanId, 
            String amount, 
            String currency, 
            String settlementAmount,
            String settlementCurrency,
            String settlementReference,
            String settlementMethod,
            CurrencyConversionDetails conversionDetails) {
        
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
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
        if (settlementAmount == null || settlementAmount.trim().isEmpty()) {
            throw new IllegalArgumentException("Settlement amount cannot be null or empty");
        }
        if (settlementCurrency == null || settlementCurrency.trim().isEmpty()) {
            throw new IllegalArgumentException("Settlement currency cannot be null or empty");
        }
        if (settlementReference == null || settlementReference.trim().isEmpty()) {
            throw new IllegalArgumentException("Settlement reference cannot be null or empty");
        }
        if (settlementMethod == null || settlementMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Settlement method cannot be null or empty");
        }
        
        return PaymentSettledEvent.builder()
                .paymentId(paymentId)
                .loanId(loanId)
                .amount(amount.trim())
                .currency(currency.trim())
                .settlementAmount(settlementAmount.trim())
                .settlementCurrency(settlementCurrency.trim())
                .settlementReference(settlementReference.trim())
                .settlementMethod(settlementMethod.trim())
                .conversionDetails(conversionDetails)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}