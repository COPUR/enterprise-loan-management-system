
package com.bank.loanmanagement.domain.model;

/**
 * Domain exception thrown when attempting to perform operations on a loan
 * that are not valid for its current state.
 */
public class IllegalLoanStateException extends RuntimeException {
    
    public IllegalLoanStateException(String message) {
        super(message);
    }
    
    public IllegalLoanStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
