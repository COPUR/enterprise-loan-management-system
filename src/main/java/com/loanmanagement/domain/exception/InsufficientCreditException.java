package com.loanmanagement.domain.exception;

/**
 * Domain exception thrown when a customer has insufficient credit for a loan operation.
 * Follows DDD principles by representing a business rule violation.
 */
public class InsufficientCreditException extends RuntimeException {

    private final Long customerId;
    private final String requestedAmount;
    private final String availableAmount;

    public InsufficientCreditException(String message) {
        super(message);
        this.customerId = null;
        this.requestedAmount = null;
        this.availableAmount = null;
    }

    public InsufficientCreditException(Long customerId, String requestedAmount, String availableAmount) {
        super(String.format("Insufficient credit for customer %d. Requested: %s, Available: %s",
              customerId, requestedAmount, availableAmount));
        this.customerId = customerId;
        this.requestedAmount = requestedAmount;
        this.availableAmount = availableAmount;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getRequestedAmount() {
        return requestedAmount;
    }

    public String getAvailableAmount() {
        return availableAmount;
    }
}
