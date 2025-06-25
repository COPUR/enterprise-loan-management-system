package com.bank.loanmanagement.ai.domain.model;

/**
 * Status enumeration for AI loan analysis requests
 */
public enum AnalysisStatus {
    PENDING_NLP_PROCESSING("Pending Natural Language Processing"),
    PENDING("Pending Analysis"),
    PROCESSING("Processing"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String description;

    AnalysisStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if the status indicates the request is active
     */
    public boolean isActive() {
        return this == PENDING_NLP_PROCESSING || this == PENDING || this == PROCESSING;
    }

    /**
     * Check if the status indicates completion (success or failure)
     */
    public boolean isComplete() {
        return this == COMPLETED || this == FAILED;
    }

    /**
     * Check if the status indicates the request can be processed
     */
    public boolean canBeProcessed() {
        return this == PENDING;
    }

    /**
     * Get the next expected status in the workflow
     */
    public AnalysisStatus getNextStatus() {
        return switch (this) {
            case PENDING_NLP_PROCESSING -> PENDING;
            case PENDING -> PROCESSING;
            case PROCESSING -> COMPLETED;
            case COMPLETED, FAILED -> throw new IllegalStateException("No next status for completed/failed requests");
        };
    }
}