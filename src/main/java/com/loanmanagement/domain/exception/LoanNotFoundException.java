package com.loanmanagement.domain.exception;

/**
 * Domain exception thrown when a loan is not found.
 * Follows DDD principles by representing a business rule violation.
 */
public class LoanNotFoundException extends RuntimeException {

    private final Long loanId;

    public LoanNotFoundException(Long loanId) {
        super("Loan not found with ID: " + loanId);
        this.loanId = loanId;
    }

    public LoanNotFoundException(String message) {
        super(message);
        this.loanId = null;
    }

    public Long getLoanId() {
        return loanId;
    }
}
