package com.bank.loanmanagement.loan.saga.retry;

public enum BackoffStrategy {
    LINEAR,
    EXPONENTIAL,
    EXPONENTIAL_WITH_JITTER
}
