package com.bank.loanmanagement.loan.saga.model;

public class SagaStepImpl {
    private String stepId;
    private String stepName;
    private String serviceEndpoint;
    private String compensationEndpoint;

    public SagaStepImpl(String stepId, String stepName, String serviceEndpoint, String compensationEndpoint) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.serviceEndpoint = serviceEndpoint;
        this.compensationEndpoint = compensationEndpoint;
    }

    public String getStepId() {
        return stepId;
    }

    public String getStepName() {
        return stepName;
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }

    public String getCompensationEndpoint() {
        return compensationEndpoint;
    }

    public Object getStepData() {
        // Placeholder for step-specific data
        return null;
    }
}
