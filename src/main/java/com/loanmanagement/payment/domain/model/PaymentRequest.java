package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Value object representing a payment request.
 */
@Value
@Builder
@With
public class PaymentRequest {
    
    String requestId;
    String customerId;
    String loanId;
    BigDecimal amount;
    String currencyCode;
    PaymentMethod paymentMethod;
    PaymentSource paymentSource;
    PaymentAllocationStrategy allocationStrategy;
    LocalDateTime scheduledDate;
    LocalDateTime requestedDate;
    String description;
    String reference;
    boolean isRecurring;
    PaymentFrequency frequency;
    LocalDateTime recurringEndDate;
    Map<String, String> metadata;

    public static class PaymentRequestBuilder {
        public PaymentRequestBuilder amount(BigDecimal amount) {
            if (amount != null && amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Payment amount must be greater than zero");
            }
            this.amount = amount;
            return this;
        }

        public PaymentRequestBuilder customerId(String customerId) {
            if (customerId != null && customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID cannot be empty");
            }
            this.customerId = customerId;
            return this;
        }

        public PaymentRequestBuilder loanId(String loanId) {
            if (loanId != null && loanId.trim().isEmpty()) {
                throw new IllegalArgumentException("Loan ID cannot be empty");
            }
            this.loanId = loanId;
            return this;
        }

        public PaymentRequest build() {
            if (requestId == null || requestId.trim().isEmpty()) {
                throw new IllegalArgumentException("Request ID is required");
            }
            if (customerId == null || customerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Customer ID is required");
            }
            if (loanId == null || loanId.trim().isEmpty()) {
                throw new IllegalArgumentException("Loan ID is required");
            }
            if (amount == null) {
                throw new IllegalArgumentException("Payment amount is required");
            }
            if (paymentMethod == null) {
                throw new IllegalArgumentException("Payment method is required");
            }
            if (paymentSource == null) {
                throw new IllegalArgumentException("Payment source is required");
            }
            if (currencyCode == null || currencyCode.trim().isEmpty()) {
                this.currencyCode = "USD";
            }
            if (allocationStrategy == null) {
                this.allocationStrategy = PaymentAllocationStrategy.INTEREST_FIRST;
            }
            if (requestedDate == null) {
                this.requestedDate = LocalDateTime.now();
            }
            if (scheduledDate == null) {
                this.scheduledDate = requestedDate;
            }
            if (metadata == null) {
                this.metadata = Map.of();
            }
            
            // Validate recurring payment settings
            if (isRecurring && frequency == null) {
                throw new IllegalArgumentException("Frequency is required for recurring payments");
            }
            if (isRecurring && frequency == PaymentFrequency.ONE_TIME) {
                throw new IllegalArgumentException("One-time frequency cannot be used for recurring payments");
            }
            
            return new PaymentRequest(
                requestId, customerId, loanId, amount, currencyCode,
                paymentMethod, paymentSource, allocationStrategy,
                scheduledDate, requestedDate, description, reference,
                isRecurring, frequency, recurringEndDate, metadata
            );
        }
    }

    public boolean isScheduled() {
        return scheduledDate != null && scheduledDate.isAfter(LocalDateTime.now());
    }

    public boolean isImmediate() {
        return !isScheduled();
    }

    public boolean requiresValidation() {
        return paymentMethod.requiresValidation();
    }

    public boolean isHighValue() {
        return amount.compareTo(new BigDecimal("10000")) > 0;
    }
}