package com.bank.loanmanagement.graphql.resolver;

import com.bank.loanmanagement.graphql.dto.*;
import com.bank.loanmanagement.service.SystemHealthService;
import com.bank.loanmanagement.service.CircuitBreakerService;
import com.bank.loanmanagement.service.SagaService;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class SystemQueryResolver {

    @Autowired
    private SystemHealthService systemHealthService;
    
    @Autowired
    private CircuitBreakerService circuitBreakerService;
    
    @Autowired
    private SagaService sagaService;

    public CompletableFuture<SystemHealth> getSystemHealth(DataFetchingEnvironment environment) {
        return CompletableFuture.supplyAsync(() -> {
            return SystemHealth.builder()
                .status(HealthStatus.UP)
                .timestamp(LocalDateTime.now())
                .services(systemHealthService.getAllServiceHealth())
                .database(systemHealthService.getDatabaseHealth())
                .cache(systemHealthService.getCacheHealth())
                .circuitBreakers(circuitBreakerService.getAllCircuitBreakerStates())
                .metrics(systemHealthService.getSystemMetrics())
                .build();
        });
    }

    public CompletableFuture<List<CircuitBreakerState>> getCircuitBreakerStatus(DataFetchingEnvironment environment) {
        return CompletableFuture.supplyAsync(() -> {
            return circuitBreakerService.getAllCircuitBreakerStates();
        });
    }

    public CompletableFuture<List<Object>> getSagaStates(DataFetchingEnvironment environment) {
        String status = environment.getArgument("status");
        return CompletableFuture.supplyAsync(() -> {
            return sagaService.getSagaStates(status);
        });
    }

    public CompletableFuture<BusinessRulesConfig> getBusinessRules(DataFetchingEnvironment environment) {
        return CompletableFuture.supplyAsync(() -> {
            return systemHealthService.getBusinessRules();
        });
    }

    public CompletableFuture<List<InterestRateConfig>> getInterestRates(DataFetchingEnvironment environment) {
        return CompletableFuture.supplyAsync(() -> {
            return systemHealthService.getInterestRateConfigs();
        });
    }
}