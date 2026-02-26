package com.loanmanagement.loan.domain.model;

import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

/**
 * Represents a single entry in an amortization schedule
 */
@Value
@Builder
public class AmortizationEntry {
    int paymentNumber;
    Money paymentAmount;
    Money principalAmount;
    Money interestAmount;
    Money remainingBalance;
}