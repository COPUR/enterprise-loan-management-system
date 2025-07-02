package com.bank.loanmanagement.loan.saga.retry;

import lombok.Builder;
import lombok.Value;

import java.util.function.BiPredicate;

@Value
@Builder
public class RetryPolicy {
    int maxRetries;
    BackoffStrategy backoffStrategy;
    BiPredicate<Exception, Integer> retryPredicate;
    boolean adaptiveRetry;
}
