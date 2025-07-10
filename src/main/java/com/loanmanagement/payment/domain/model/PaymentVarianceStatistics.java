package com.loanmanagement.payment.domain.model;

import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

/**
 * Statistics for payment variances
 */
@Value
@Builder
public class PaymentVarianceStatistics {
    Money averageVariance;
    Money maxVariance;
    Money minVariance;
    int varianceCount;
}