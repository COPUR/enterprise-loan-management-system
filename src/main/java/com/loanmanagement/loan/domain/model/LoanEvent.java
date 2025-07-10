package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;

import java.time.LocalDateTime;

/**
 * Sealed interface for Loan Events using Java 21 pattern matching
 * Represents all domain events that can occur in a loan's lifecycle
 */
public sealed interface LoanEvent 
    permits LoanEvent.ApplicationSubmitted, LoanEvent.LoanApproved, LoanEvent.LoanRejected,
            LoanEvent.LoanDisbursed, LoanEvent.PaymentMade, LoanEvent.LoanRestructured,
            LoanEvent.LoanDefaulted, LoanEvent.LoanPaidOff, LoanEvent.LateFeeApplied {
    
    LoanId loanId();
    LocalDateTime timestamp();
    
    default String getEventType() {
        return this.getClass().getSimpleName();
    }
    
    default boolean isStateChangingEvent() {
        return switch (this) {
            case ApplicationSubmitted ignored -> true;
            case LoanApproved ignored -> true;
            case LoanRejected ignored -> true;
            case LoanDisbursed ignored -> true;
            case LoanRestructured ignored -> true;
            case LoanDefaulted ignored -> true;
            case LoanPaidOff ignored -> true;
            case PaymentMade ignored -> false;
            case LateFeeApplied ignored -> false;
        };
    }
    
    default boolean requiresNotification() {
        return switch (this) {
            case LoanApproved ignored -> true;
            case LoanRejected ignored -> true;
            case LoanDisbursed ignored -> true;
            case LoanDefaulted ignored -> true;
            case LoanPaidOff ignored -> true;
            default -> false;
        };
    }
    
    // Event implementations as records
    record ApplicationSubmitted(
        LoanId loanId,
        CustomerId customerId,
        Money requestedAmount,
        LocalDateTime timestamp
    ) implements LoanEvent {}
    
    record LoanApproved(
        LoanId loanId,
        CustomerId customerId,
        Money approvedAmount,
        LoanOfficerId approvingOfficer,
        LocalDateTime timestamp
    ) implements LoanEvent {}
    
    record LoanRejected(
        LoanId loanId,
        CustomerId customerId,
        String rejectionReason,
        LoanOfficerId rejectingOfficer,
        LocalDateTime timestamp
    ) implements LoanEvent {}
    
    record LoanDisbursed(
        LoanId loanId,
        CustomerId customerId,
        Money disbursedAmount,
        String disbursementMethod,
        LocalDateTime timestamp
    ) implements LoanEvent {}
    
    record PaymentMade(
        LoanId loanId,
        CustomerId customerId,
        Money paymentAmount,
        LocalDateTime timestamp
    ) implements LoanEvent {}
    
    record LoanRestructured(
        LoanId loanId,
        CustomerId customerId,
        LoanTerms oldTerms,
        LoanTerms newTerms,
        String restructuringReason,
        LocalDateTime timestamp
    ) implements LoanEvent {}
    
    record LoanDefaulted(
        LoanId loanId,
        CustomerId customerId,
        String defaultReason,
        Money outstandingBalance,
        LocalDateTime timestamp
    ) implements LoanEvent {}
    
    record LoanPaidOff(
        LoanId loanId,
        CustomerId customerId,
        LocalDateTime timestamp
    ) implements LoanEvent {}
    
    record LateFeeApplied(
        LoanId loanId,
        CustomerId customerId,
        Money feeAmount,
        String reason,
        LocalDateTime timestamp
    ) implements LoanEvent {}
}