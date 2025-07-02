package com.bank.loanmanagement.loan.saga.executor;

import com.bank.loanmanagement.loan.saga.definition.SagaDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaStepExecutor {
    public void executeStep(SagaDefinition sagaDefinition, String stepId, Object stepData) {
        log.info("Executing step {} for saga {}", stepId, sagaDefinition.getSagaType());
        // Placeholder for actual step execution logic
    }
}
