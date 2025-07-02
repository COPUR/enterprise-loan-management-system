package com.loanmanagement.loan.domain.model.value;

import com.loanmanagement.sharedkernel.domain.value.Money;
import com.loanmanagement.sharedkernel.domain.model.ValueObject;

/**
 * Value object representing the result of a payment calculation.
 * Encapsulates payment amount, discounts, penalties, and related data.
 */
public final class PaymentCalculation extends ValueObject {

    private final Money originalAmount;
    private final Money totalAmount;
    private final Money discountAmount;
    private final Money penaltyAmount;
    private final long daysEarly;
    private final long daysLate;
    private final PaymentType paymentType;

    private PaymentCalculation(
            Money originalAmount,
            Money totalAmount,
            Money discountAmount,
            Money penaltyAmount,
            long daysEarly,
            long daysLate,
            PaymentType paymentType
    ) {
        this.originalAmount = originalAmount;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.penaltyAmount = penaltyAmount;
        this.daysEarly = daysEarly;
        this.daysLate = daysLate;
        this.paymentType = paymentType;
    }

    public static PaymentCalculation withDiscount(
            Money originalAmount,
            Money totalAmount,
            Money discountAmount,
            long daysEarly
    ) {
        return new PaymentCalculation(
                originalAmount,
                totalAmount,
                discountAmount,
                Money.ZERO,
                daysEarly,
                0L,
                PaymentType.EARLY
        );
    }

    public static PaymentCalculation withPenalty(
            Money originalAmount,
            Money totalAmount,
            Money penaltyAmount,
            long daysLate
    ) {
        return new PaymentCalculation(
                originalAmount,
                totalAmount,
                Money.ZERO,
                penaltyAmount,
                0L,
                daysLate,
                PaymentType.LATE
        );
    }

    public static PaymentCalculation onTime(Money originalAmount) {
        return new PaymentCalculation(
                originalAmount,
                originalAmount,
                Money.ZERO,
                Money.ZERO,
                0L,
                0L,
                PaymentType.ON_TIME
        );
    }

    public Money getTotalAmount() { return totalAmount; }
    public Money getOriginalAmount() { return originalAmount; }
    public Money getDiscountAmount() { return discountAmount; }
    public Money getPenaltyAmount() { return penaltyAmount; }
    public long getDaysEarly() { return daysEarly; }
    public long getDaysLate() { return daysLate; }
    public PaymentType getPaymentType() { return paymentType; }

    @Override
    protected Object[] getEqualityComponents() {
        return new Object[]{
                originalAmount, totalAmount, discountAmount, penaltyAmount,
                daysEarly, daysLate, paymentType
        };
    }

    public enum PaymentType {
        EARLY, ON_TIME, LATE
    }
}
