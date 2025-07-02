package com.bank.loanmanagement.loan.saga.definition;

import com.bank.loanmanagement.loan.saga.context.SagaContext;
import com.bank.loanmanagement.loan.saga.retry.RetryPolicy;
import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

@Value
@Builder
public class SagaStepDefinition {
    String stepId;
    String stepName;
    String serviceEndpoint;
    String compensationEndpoint;
    Duration timeout;
    RetryPolicy retryPolicy;
    Function<SagaContext, Boolean> decisionEngine;
    Function<SagaContext, java.util.concurrent.CompletableFuture<Void>> preStepAnalysis;
    Function<SagaContext, java.util.concurrent.CompletableFuture<Void>> postStepAnalysis;
    Predicate<SagaContext> executionCondition;
}
