package com.bank.loanmanagement.loan.saga.domain;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * SAGA Definition interface for defining distributed transaction workflows
 * Provides blueprint for SAGA orchestration with BIAN service domain integration
 * Supports compensation patterns and timeout handling
 */
public interface SagaDefinition {

    /**
     * Unique identifier for the SAGA type
     */
    String getSagaType();

    /**
     * SAGA version for evolution and compatibility
     */
    String getVersion();

    /**
     * SAGA description for documentation and monitoring
     */
    String getDescription();

    /**
     * Ordered list of steps in the SAGA
     */
    List<SagaStepDefinition> getSteps();

    /**
     * SAGA timeout duration
     */
    Duration getTimeout();

    /**
     * Retry policy for failed steps
     */
    RetryPolicy getRetryPolicy();

    /**
     * BIAN service domain context for this SAGA
     */
    String getServiceDomainContext();

    /**
     * Compliance requirements for this SAGA
     */
    ComplianceRequirements getComplianceRequirements();

    /**
     * Individual step definition within a SAGA
     */
    interface SagaStepDefinition {
        String getStepId();
        String getStepType();
        String getDescription();
        
        // BIAN service domain integration
        String getTargetServiceDomain();
        String getBehaviorQualifier();
        String getServiceOperation();
        
        // Step configuration
        Duration getStepTimeout();
        int getMaxRetries();
        Map<String, Object> getStepConfiguration();
        
        // Compensation configuration
        String getCompensationAction();
        boolean isCompensationRequired();
        Duration getCompensationTimeout();
        
        // Conditional execution
        Predicate<Map<String, Object>> getExecutionCondition();
        List<String> getDependsOnSteps();
        
        // Error handling
        List<Class<? extends Exception>> getRetryableExceptions();
        List<Class<? extends Exception>> getFatalExceptions();
    }

    /**
     * Retry policy configuration
     */
    interface RetryPolicy {
        int getMaxRetries();
        Duration getInitialDelay();
        Duration getMaxDelay();
        double getBackoffMultiplier();
        List<Class<? extends Exception>> getRetryableExceptions();
        List<Class<? extends Exception>> getNonRetryableExceptions();
    }

    /**
     * Compliance requirements for regulatory adherence
     */
    interface ComplianceRequirements {
        boolean isAuditRequired();
        boolean isBianCompliant();
        boolean isBerlinGroupCompliant();
        boolean isFapiSecurityRequired();
        List<String> getRegulatoryReports();
        Map<String, String> getComplianceMetadata();
    }

    /**
     * Default implementations for common patterns
     */
    class DefaultRetryPolicy implements RetryPolicy {
        private final int maxRetries;
        private final Duration initialDelay;
        private final Duration maxDelay;
        private final double backoffMultiplier;
        private final List<Class<? extends Exception>> retryableExceptions;
        private final List<Class<? extends Exception>> nonRetryableExceptions;

        public DefaultRetryPolicy(int maxRetries, Duration initialDelay, Duration maxDelay, 
                                double backoffMultiplier,
                                List<Class<? extends Exception>> retryableExceptions,
                                List<Class<? extends Exception>> nonRetryableExceptions) {
            this.maxRetries = maxRetries;
            this.initialDelay = initialDelay;
            this.maxDelay = maxDelay;
            this.backoffMultiplier = backoffMultiplier;
            this.retryableExceptions = retryableExceptions;
            this.nonRetryableExceptions = nonRetryableExceptions;
        }

        @Override public int getMaxRetries() { return maxRetries; }
        @Override public Duration getInitialDelay() { return initialDelay; }
        @Override public Duration getMaxDelay() { return maxDelay; }
        @Override public double getBackoffMultiplier() { return backoffMultiplier; }
        @Override public List<Class<? extends Exception>> getRetryableExceptions() { return retryableExceptions; }
        @Override public List<Class<? extends Exception>> getNonRetryableExceptions() { return nonRetryableExceptions; }
    }

    class DefaultComplianceRequirements implements ComplianceRequirements {
        private final boolean auditRequired;
        private final boolean bianCompliant;
        private final boolean berlinGroupCompliant;
        private final boolean fapiSecurityRequired;
        private final List<String> regulatoryReports;
        private final Map<String, String> complianceMetadata;

        public DefaultComplianceRequirements(boolean auditRequired, boolean bianCompliant, 
                                           boolean berlinGroupCompliant, boolean fapiSecurityRequired,
                                           List<String> regulatoryReports, Map<String, String> complianceMetadata) {
            this.auditRequired = auditRequired;
            this.bianCompliant = bianCompliant;
            this.berlinGroupCompliant = berlinGroupCompliant;
            this.fapiSecurityRequired = fapiSecurityRequired;
            this.regulatoryReports = regulatoryReports;
            this.complianceMetadata = complianceMetadata;
        }

        @Override public boolean isAuditRequired() { return auditRequired; }
        @Override public boolean isBianCompliant() { return bianCompliant; }
        @Override public boolean isBerlinGroupCompliant() { return berlinGroupCompliant; }
        @Override public boolean isFapiSecurityRequired() { return fapiSecurityRequired; }
        @Override public List<String> getRegulatoryReports() { return regulatoryReports; }
        @Override public Map<String, String> getComplianceMetadata() { return complianceMetadata; }
    }

    /**
     * Builder for creating SAGA step definitions
     */
    class SagaStepDefinitionBuilder {
        private String stepId;
        private String stepType;
        private String description;
        private String targetServiceDomain;
        private String behaviorQualifier;
        private String serviceOperation;
        private Duration stepTimeout = Duration.ofMinutes(5);
        private int maxRetries = 3;
        private Map<String, Object> stepConfiguration = Map.of();
        private String compensationAction;
        private boolean compensationRequired = true;
        private Duration compensationTimeout = Duration.ofMinutes(2);
        private Predicate<Map<String, Object>> executionCondition = data -> true;
        private List<String> dependsOnSteps = List.of();
        private List<Class<? extends Exception>> retryableExceptions = List.of();
        private List<Class<? extends Exception>> fatalExceptions = List.of();

        public SagaStepDefinitionBuilder stepId(String stepId) { this.stepId = stepId; return this; }
        public SagaStepDefinitionBuilder stepType(String stepType) { this.stepType = stepType; return this; }
        public SagaStepDefinitionBuilder description(String description) { this.description = description; return this; }
        public SagaStepDefinitionBuilder targetServiceDomain(String domain) { this.targetServiceDomain = domain; return this; }
        public SagaStepDefinitionBuilder behaviorQualifier(String qualifier) { this.behaviorQualifier = qualifier; return this; }
        public SagaStepDefinitionBuilder serviceOperation(String operation) { this.serviceOperation = operation; return this; }
        public SagaStepDefinitionBuilder stepTimeout(Duration timeout) { this.stepTimeout = timeout; return this; }
        public SagaStepDefinitionBuilder maxRetries(int retries) { this.maxRetries = retries; return this; }
        public SagaStepDefinitionBuilder stepConfiguration(Map<String, Object> config) { this.stepConfiguration = config; return this; }
        public SagaStepDefinitionBuilder compensationAction(String action) { this.compensationAction = action; return this; }
        public SagaStepDefinitionBuilder compensationRequired(boolean required) { this.compensationRequired = required; return this; }
        public SagaStepDefinitionBuilder compensationTimeout(Duration timeout) { this.compensationTimeout = timeout; return this; }
        public SagaStepDefinitionBuilder executionCondition(Predicate<Map<String, Object>> condition) { this.executionCondition = condition; return this; }
        public SagaStepDefinitionBuilder dependsOnSteps(List<String> dependencies) { this.dependsOnSteps = dependencies; return this; }
        public SagaStepDefinitionBuilder retryableExceptions(List<Class<? extends Exception>> exceptions) { this.retryableExceptions = exceptions; return this; }
        public SagaStepDefinitionBuilder fatalExceptions(List<Class<? extends Exception>> exceptions) { this.fatalExceptions = exceptions; return this; }

        public SagaStepDefinition build() {
            return new SagaStepDefinition() {
                @Override public String getStepId() { return stepId; }
                @Override public String getStepType() { return stepType; }
                @Override public String getDescription() { return description; }
                @Override public String getTargetServiceDomain() { return targetServiceDomain; }
                @Override public String getBehaviorQualifier() { return behaviorQualifier; }
                @Override public String getServiceOperation() { return serviceOperation; }
                @Override public Duration getStepTimeout() { return stepTimeout; }
                @Override public int getMaxRetries() { return maxRetries; }
                @Override public Map<String, Object> getStepConfiguration() { return stepConfiguration; }
                @Override public String getCompensationAction() { return compensationAction; }
                @Override public boolean isCompensationRequired() { return compensationRequired; }
                @Override public Duration getCompensationTimeout() { return compensationTimeout; }
                @Override public Predicate<Map<String, Object>> getExecutionCondition() { return executionCondition; }
                @Override public List<String> getDependsOnSteps() { return dependsOnSteps; }
                @Override public List<Class<? extends Exception>> getRetryableExceptions() { return retryableExceptions; }
                @Override public List<Class<? extends Exception>> getFatalExceptions() { return fatalExceptions; }
            };
        }
    }
}