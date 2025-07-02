package com.loanmanagement.loan.domain.model.value;

import com.loanmanagement.sharedkernel.domain.value.Money;
import com.loanmanagement.sharedkernel.domain.model.ValueObject;

/**
 * Value object representing the result of a payment operation.
 * Encapsulates payment success/failure state and related information.
 */
public final class PaymentResult extends ValueObject {

    private final boolean successful;
    private final PaymentCalculation calculation;
    private final Money attemptedAmount;
    private final String failureReason;

    private PaymentResult(
            boolean successful,
            PaymentCalculation calculation,
            Money attemptedAmount,
            String failureReason
    ) {
        this.successful = successful;
        this.calculation = calculation;
        this.attemptedAmount = attemptedAmount;
        this.failureReason = failureReason;
    }

    public static PaymentResult successful(PaymentCalculation calculation) {
        return new PaymentResult(true, calculation, calculation.getTotalAmount(), null);
    }

    public static PaymentResult insufficient(Money attemptedAmount, Money requiredAmount) {
        return new PaymentResult(
                false,
                null,
                attemptedAmount,
                String.format("Insufficient amount. Required: %s, Provided: %s",
                        requiredAmount, attemptedAmount)
        );
    }

    public static PaymentResult failed(String reason) {
        return new PaymentResult(false, null, null, reason);
    }

    public boolean isSuccessful() { return successful; }
    public PaymentCalculation getCalculation() { return calculation; }
    public Money getAttemptedAmount() { return attemptedAmount; }
    public String getFailureReason() { return failureReason; }

    public int getInstallmentsPaid() {
        return successful ? 1 : 0;
    }

    public Money getTotalAmountSpent() {
        return successful ? calculation.getTotalAmount() : Money.ZERO;
    }

    public boolean isLoanFullyPaid() {
        // This would need to be determined by the aggregate
        return false;
    }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{successful, calculation, attemptedAmount, failureReason};
    }
}
