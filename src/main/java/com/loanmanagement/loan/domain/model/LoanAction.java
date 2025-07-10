package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;

import java.util.List;

/**
 * Sealed interface for Loan Actions using Java 21 pattern matching
 * Represents all possible actions that can be performed on a loan
 */
public sealed interface LoanAction 
    permits LoanAction.Approve, LoanAction.Reject, LoanAction.Disburse, 
            LoanAction.MakePayment, LoanAction.Restructure, LoanAction.MarkDefault {
    
    default String getActionName() {
        return this.getClass().getSimpleName();
    }
    
    default boolean requiresAuthorization() {
        return switch (this) {
            case Approve ignored -> true;
            case Reject ignored -> true;
            case Disburse ignored -> true;
            case Restructure ignored -> true;
            case MarkDefault ignored -> true;
            case MakePayment ignored -> false;
        };
    }
    
    default AuthorizationLevel getRequiredAuthorizationLevel() {
        return switch (this) {
            case Approve approve when approve.approvedAmount().getAmount().compareTo(java.math.BigDecimal.valueOf(100000)) > 0 -> 
                AuthorizationLevel.SENIOR_MANAGER;
            case Approve ignored -> AuthorizationLevel.LOAN_OFFICER;
            case Reject ignored -> AuthorizationLevel.LOAN_OFFICER;
            case Disburse disburse when disburse.disbursementAmount().getAmount().compareTo(java.math.BigDecimal.valueOf(50000)) > 0 -> 
                AuthorizationLevel.MANAGER;
            case Disburse ignored -> AuthorizationLevel.LOAN_OFFICER;
            case Restructure ignored -> AuthorizationLevel.MANAGER;
            case MarkDefault ignored -> AuthorizationLevel.SENIOR_MANAGER;
            case MakePayment ignored -> AuthorizationLevel.NONE;
        };
    }
    
    // Specific action implementations as records
    record Approve(
        Money approvedAmount,
        List<String> conditions,
        String approvalReason
    ) implements LoanAction {}
    
    record Reject(
        String rejectionReason,
        String rejectionCode
    ) implements LoanAction {}
    
    record Disburse(
        Money disbursementAmount,
        String disbursementMethod,
        String accountNumber
    ) implements LoanAction {}
    
    record MakePayment(
        Money amount,
        String paymentMethod,
        String referenceNumber
    ) implements LoanAction {}
    
    record Restructure(
        LoanTerms newTerms,
        String restructuringReason,
        java.math.BigDecimal newInterestRate
    ) implements LoanAction {}
    
    record MarkDefault(
        String defaultReason,
        java.time.LocalDateTime defaultDate
    ) implements LoanAction {}
}

enum AuthorizationLevel {
    NONE, LOAN_OFFICER, MANAGER, SENIOR_MANAGER, EXECUTIVE
}