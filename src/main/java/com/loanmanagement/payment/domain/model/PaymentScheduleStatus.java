package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different payment schedule statuses.
 */
public enum PaymentScheduleStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed"),
    PENDING_ACTIVATION("Pending Activation"),
    PENDING_MODIFICATION("Pending Modification"),
    PENDING_CANCELLATION("Pending Cancellation"),
    EXPIRED("Expired"),
    FAILED("Failed"),
    PAUSED("Paused"),
    DRAFT("Draft");

    private final String displayName;

    PaymentScheduleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isPending() {
        return this == PENDING_ACTIVATION || this == PENDING_MODIFICATION || this == PENDING_CANCELLATION;
    }

    public boolean isTerminal() {
        return this == CANCELLED || this == COMPLETED || this == EXPIRED || this == FAILED;
    }

    public boolean canBeModified() {
        return this == ACTIVE || this == INACTIVE || this == SUSPENDED || this == PAUSED || this == DRAFT;
    }
}