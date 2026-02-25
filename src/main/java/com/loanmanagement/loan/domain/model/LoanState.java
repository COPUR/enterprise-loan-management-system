package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sealed interface for Loan States using Java 21 pattern matching
 * Provides type-safe state management for loan lifecycle
 */
public sealed interface LoanState 
    permits PendingState, ApprovedState, RejectedState, DisbursedState, 
            RestructuredState, DefaultedState, PaidOffState {
    
    LocalDateTime timestamp();
    String reason();
    
    default String getStateName() {
        return this.getClass().getSimpleName().replace("State", "");
    }
    
    default boolean isTerminalState() {
        return switch (this) {
            case RejectedState ignored -> true;
            case DefaultedState ignored -> true;
            case PaidOffState ignored -> true;
            default -> false;
        };
    }
    
    default boolean allowsPayments() {
        return switch (this) {
            case DisbursedState ignored -> true;
            case RestructuredState ignored -> true;
            default -> false;
        };
    }
    
    default boolean requiresApproval() {
        return this instanceof PendingState;
    }
}

// Specific state implementations as records
record PendingState(
    LocalDateTime timestamp,
    String reason
) implements LoanState {}

record ApprovedState(
    LocalDateTime timestamp,
    String reason,
    LoanOfficerId approvingOfficer,
    List<String> approvalConditions,
    Money approvedAmount
) implements LoanState {}

record RejectedState(
    LocalDateTime timestamp,
    String reason,
    LoanOfficerId rejectingOfficer,
    String rejectionCode
) implements LoanState {}

record DisbursedState(
    LocalDateTime timestamp,
    String reason,
    String disbursementMethod,
    Money disbursedAmount
) implements LoanState {}

record RestructuredState(
    LocalDateTime timestamp,
    String reason,
    LoanTerms newTerms,
    String restructuringReason
) implements LoanState {}

record DefaultedState(
    LocalDateTime timestamp,
    String reason,
    String defaultReason,
    Money outstandingBalance
) implements LoanState {}

record PaidOffState(
    LocalDateTime timestamp,
    String reason
) implements LoanState {}